/**
 * Created by Aksenov239 on 28.08.2014.
 */

package ru.ifmo.ctddev.games;

import com.corundumstudio.socketio.listener.*;
import com.corundumstudio.socketio.*;
import ru.ifmo.ctddev.games.messages.*;
import ru.ifmo.ctddev.games.messages.JoinMessage;
import ru.ifmo.ctddev.games.state.*;

import java.io.IOException;
import java.util.*;
import java.sql.*;

public class GameServer {
    final static Map <UUID, Player> onlineUsers = new HashMap<UUID, Player>();
    final static int SECONDS_PER_DAY = 24 * 3600;
    static SocketIOServer server;
    static Shop shop;
    static MapState map;
    static Random helpfulRandom;

    static String NAME_OF_DB;
    static String USER;
    static String PASSWORD;
    static Connection connectionToDB;

    final static boolean DEBUG_WITH_DUMPING = true;

    static class AsyncEvents {
        private static int PERIOD_OF_DUMPING = 60000;
        private static Connection connection;
        private static Timer timer;

        private static Timer timerEnergy;
        private static final int PERIOD_OF_ADD_ENERGY = 23000;
        private static final int ADD_ENERGY = 6;

        public static void setConnection(Connection con) {
            connection = con;
        }

        public static void start() {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Logger.log("Dumper.run");
                    if (onlineUsers.size() == 0)
                        return;
                    String queryUsers = "INSERT INTO Users (userId, userName, energy, money, x, y, lastActionTime) VALUES ";
                    boolean first = true;
                    for (Map.Entry <UUID, Player> e : onlineUsers.entrySet()) {
                        if (!first)
                            queryUsers += ", (?, ?, ?, ?, ?, ?, ?)";
                        else
                            queryUsers += "(?, ?, ?, ?, ?, ?, ?)";
                        first = false;
                    }
                    queryUsers += " ON DUPLICATE KEY UPDATE userId=VALUES(userId), userName = VALUES(userName), energy=VALUES(energy), money=VALUES(money), " +
                            "x=VALUES(x), y=VALUES(y), lastActionTime=VALUES(lastActionTime);";
                    try {
                        PreparedStatement st = connection.prepareStatement(queryUsers);
                        int i = 0;
                        for (Map.Entry <UUID, Player> e : onlineUsers.entrySet()) {
                            Player player = e.getValue();
                            st.setInt(i + 1, player.getUserId());
                            st.setString(i + 2, player.getUserName());
                            st.setInt(i + 3, player.getEnergy());
                            st.setInt(i + 4, player.getMoney());
                            st.setInt(i + 5, player.getX());
                            st.setInt(i + 6, player.getY());
                            st.setInt(i + 7, player.getLastActionTime());
                            i += 7;
                        }
                        st.executeUpdate();
                        st.close();

                        for (Map.Entry <UUID, Player> e : onlineUsers.entrySet()) {
                            Player player = e.getValue();
                            dumpInventory(player);
                        }
                    } catch (SQLException e) {
                        Logger.log("Dumper exception!");
                        e.printStackTrace();
                        System.exit(0);
                    }
                }
            }, PERIOD_OF_DUMPING, PERIOD_OF_DUMPING);

            timerEnergy = new Timer();
            timerEnergy.schedule(new TimerTask() {
                @Override
                public void run() {
                    server.getBroadcastOperations().sendEvent("add_energy", new AddEnergyMessage(ADD_ENERGY));
                    for (Map.Entry <UUID, Player> e : onlineUsers.entrySet())
                        e.getValue().addEnergy(ADD_ENERGY);
                }
            }, PERIOD_OF_ADD_ENERGY, PERIOD_OF_ADD_ENERGY);
        }

        public static void dump(Player state) {
            try {
                Logger.log("dump player");
                PreparedStatement statement = connectionToDB.prepareStatement("UPDATE Users SET energy=?, money=?, x=?, y=?, " +
                        "lastActionTime=? WHERE userId=?;");
                statement.setInt(1, state.getEnergy());
                statement.setInt(2, state.getMoney());
                statement.setInt(3, state.getX());
                statement.setInt(4, state.getY());
                statement.setInt(5, state.getLastActionTime());
                statement.setInt(6, state.getUserId());
                statement.executeUpdate();
                statement.close();
                dumpInventory(state);
            } catch (Exception e) {
                Logger.log("Dumping exception");
                e.printStackTrace();
                System.exit(0);
            }
        }

        private static void dumpInventory(Player player) {
            try {
                PreparedStatement st;
                Map <Integer, InventoryItem> inv = player.getInventory();
                Map <Integer, InventoryItem> invInDb = loadInventoryFromDatabase(player.getUserId());
                for (Map.Entry <Integer, InventoryItem> it : inv.entrySet()) {
                    InventoryItem curInv = it.getValue();
                    if (curInv.getCount() == 0) {
                        st = connection.prepareStatement("DELETE FROM UserInventory WHERE userId = ? AND itemId = ?;");
                        st.setInt(1, player.getUserId());
                        st.setInt(2, curInv.getId());
                        st.executeUpdate();
                    } else if (invInDb.containsKey(curInv.getId())) {
                        st = connection.prepareStatement("UPDATE UserInventory SET count = ? WHERE userId = ? AND itemId = ?;");
                        st.setInt(1, curInv.getCount());
                        st.setInt(2, player.getUserId());
                        st.setInt(3, curInv.getId());
                        st.executeUpdate();
                    } else {
                        st = connection.prepareStatement("INSERT INTO UserInventory (userId, itemId, count) VALUES(?, ?, ?);");
                        st.setInt(1, player.getUserId());
                        st.setInt(2, curInv.getId());
                        st.setInt(3, curInv.getCount());
                        st.executeUpdate();
                    }
                    st.close();
                }
            } catch (SQLException e) {
                Logger.log("Execute in dumpInventory!");
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    static class CommandReader implements Runnable {
        public CommandReader() {}
        private int getNumber(String s) {
            try {
                int number = Integer.parseInt(s);
                return number;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        @Override
        public void run() {
            System.err.println("");
            System.err.println("Available commands!");
            System.err.println("NewPoll question fromOption numberOptions option1 minimalAmount1 option2 minimalAmount2 ... optionN minimalAmountN");
            System.err.println("ClosePoll pollId");
            System.err.println("NewItem name type(number) costBuy costSell");
            System.err.println("exit");
            Scanner in = new Scanner(System.in);
            while (true) {
                String line = in.nextLine();
                StringTokenizer tokenizer = new StringTokenizer(line);
                ArrayList <String> lexems = new ArrayList<String>();
                while (tokenizer.hasMoreElements())
                    lexems.add((String)tokenizer.nextElement());
                for (int i = 0; i < lexems.size(); ++i)
                    System.err.print(lexems.get(i) + "+");
                System.err.println("");

                String command = lexems.get(0);
                if (command.equals("NewPoll")) {
                    String quest = lexems.get(1);
                    String fromOption = lexems.get(2);
                    if (quest.isEmpty() || fromOption.isEmpty()) {
                        System.err.println("Empty question or parent option!");
                    } else {
                        final int SZ_HEAD = 4;
                        int numberOfPolls = getNumber(lexems.get(SZ_HEAD - 1));
                        if (numberOfPolls <= 0 || lexems.size() != 2 * numberOfPolls + SZ_HEAD)
                            System.err.println("Illegal number of polls!");
                        else {
                            String[] names = new String[numberOfPolls];
                            int[] minAm = new int[numberOfPolls];
                            boolean fail = false;
                            for (int i = SZ_HEAD, j = 0; i < lexems.size(); i += 2, ++j) {
                                if (lexems.get(i).isEmpty()) {
                                    System.err.println("Empty option!");
                                    fail = true;
                                    break;
                                }
                                names[j] = lexems.get(i);
                            }

                            for (int i = SZ_HEAD + 1, j = 0; i < lexems.size() && !fail; i += 2, ++j) {
                                int num = getNumber(lexems.get(i));
                                if (num == -1) {
                                    System.err.println("Illegal minimal amount!");
                                    fail = true;
                                    break;
                                }
                                minAm[j] = num;
                            }

                            if (!fail) {
                                fail = !newPoll(fromOption, quest, names, minAm);
                                if (fail)
                                    System.err.println("Incorrect newPoll");
                            }
                        }
                    }
                } else if (command.equals("ClosePoll")) {
                    int id = getNumber(lexems.get(1));
                    if (id == -1)
                        System.err.println("Illegal id!");
                    else
                        VotesState.closePoll(id);
                } else if (command.equals("NewItem")) {
                    String name = lexems.get(1);
                    int type = getNumber(lexems.get(2));
                    int costBuy = getNumber(lexems.get(3));
                    int costSell = getNumber(lexems.get(4));
                    boolean fail = name.isEmpty() || costBuy == -1 || costSell == -1 || type == -1 || costBuy < costSell;
                    if (name.isEmpty())
                        System.err.println("Name is empty!");
                    else if (type == -1)
                        System.err.println("type is incorrect!");
                    else if (costBuy == -1)
                        System.err.println("costBuy is incorrect!");
                    else if (costSell == -1)
                        System.err.println("costSell is incorrect!");
                    else if (costBuy < costSell)
                        System.err.println("costBuy less than costSell!");
                    if (!fail)
                        shop.addItem(new Item(-1, name, type, costBuy, costSell));
                } else if (command.equals("exit")) {
                    server.stop();
                    System.exit(0);
                } else {
                    System.err.println("Unknown command!");
                }
            }
        }
    }

    private static void initialization() {
        shop = new Shop(connectionToDB);
        map = new MapState();
        helpfulRandom = new Random(System.currentTimeMillis());
    }

    public static void main(String[] args) throws InterruptedException {
        ///Database initializate
        if (args.length != 5) {
            System.err.println("You don't input information!");
            System.exit(0);
        }
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        NAME_OF_DB = args[2];
        USER = args[3];
        PASSWORD = args[4];

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            System.err.println("Class of database not found!");
            System.exit(0);
        }

        System.err.println("Connecting to database...");
        try {
            connectionToDB = DriverManager.getConnection("jdbc:mysql://localhost/" + NAME_OF_DB, USER, PASSWORD);
            System.err.println("Successful connection to database!");
        } catch (Exception ex) {
            System.err.println("Unsuccessful connection to database :(");
            ex.printStackTrace();
            System.exit(0);
        }

        initialization();

        System.err.println("Load active polls...");
        VotesState.setDatabase(connectionToDB);
        VotesState.loadActivePolls();
        System.err.println("Successful load active polls!");

        System.err.println("Load Available items...");
        shop.loadAvailableItems();
        System.err.println("Successful load available items!");

        Configuration config = new Configuration();
        config.setHostname(ip);
        config.setPort(port);
        server = new SocketIOServer(config);
        addListeners();

        if (DEBUG_WITH_DUMPING) {
            try {
                AsyncEvents.setConnection(DriverManager.getConnection("jdbc:mysql://localhost/" + NAME_OF_DB, USER, PASSWORD));
                AsyncEvents.start();
            } catch (Exception e) {
                Logger.log("Initializate Dumper exception!");
                e.printStackTrace();
                System.exit(0);
            }
        }

        Thread comReader = new Thread(new CommandReader());
        comReader.start();

        server.start();
        System.err.println("The server has started! All information write in log");
    }

    //Methods for work
    private static Map <Integer, InventoryItem> loadInventoryFromDatabase(int userId) {
        try {
            Map <Integer, InventoryItem> inventory = new HashMap<Integer, InventoryItem>();
            PreparedStatement preStatementDB;
            ResultSet resultSetDB;
            preStatementDB = connectionToDB.prepareStatement("SELECT * FROM UserInventory WHERE userId = ?;");
            preStatementDB.setInt(1, userId);
            resultSetDB = preStatementDB.executeQuery();
            while (resultSetDB.next()) {
                int itemId = resultSetDB.getInt("itemId");
                int count = resultSetDB.getInt("count");
                preStatementDB = connectionToDB.prepareStatement("SELECT * FROM Items WHERE itemId = ?;");
                preStatementDB.setInt(1, itemId);
                ResultSet curItemInShop = preStatementDB.executeQuery();
                curItemInShop.next();
                String nameItem = curItemInShop.getString("name");
                int type = curItemInShop.getInt("type");
                int costSell = curItemInShop.getInt("costSell");
                inventory.put(itemId, new InventoryItem(itemId, nameItem, costSell, type, count));
                curItemInShop.close();
            }
            return inventory;
        } catch (SQLException e) {
            Logger.log("Exception in loadInventoryFromDatabase!");
            e.printStackTrace();
            System.exit(0);
        }
        return null;
    }

    //Server methods
    private static void addListeners() {
        //New user connected
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient socketIOClient) {
                Logger.log("New client has connected");
            }
        });

        //User disconnected
        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {
                disconnectedUser(socketIOClient);
            }
        });

        /*
             -> join_request, {userName: string}
             <- start, {success: boolean}
        */
        server.addEventListener("join_request", JoinMessage.class, new DataListener<JoinMessage>() {
            @Override
            public void onData(SocketIOClient client, JoinMessage data, AckRequest ackRequest) {
                joinRequest(client, data, ackRequest);
            }
        });

        /*
            * -> move_request, {id: long, dx: int, dy: int}
            * <- move_response {success: boolean}
        */
        server.addEventListener("move_request", MoveMessage.class, new DataListener<MoveMessage>() {
            @Override
            public void onData(SocketIOClient client, MoveMessage data, AckRequest ackRequest) {
                moveRequest(client, data, ackRequest);
            }
        });


        /*
            * -> get_state
            * <- state {money: int, energy: int, x: int, y: int}
        */
        server.addEventListener("get_state", StateMessage.class, new DataListener<StateMessage>() {
            @Override
            public void onData(SocketIOClient socketIOClient, StateMessage data, AckRequest ackRequest) throws Exception {
                getState(socketIOClient, data, ackRequest);
            }
        });

        /*
            * -> get_map
            * <- map, {field: int[][]}
        */
        server.addEventListener("get_map", MapMessage.class, new DataListener<MapMessage>() {
            @Override
            public void onData(SocketIOClient socketIOClient, MapMessage data, AckRequest ackRequest) throws Exception {
                getMap(socketIOClient, data, ackRequest);
            }
        });

        /*
          * -> dig
          * <- dig_response, {type: int, amount: int, energy: int}
        */
        server.addEventListener("dig", DigMessage.class, new DataListener<DigMessage>() {
            @Override
            public void onData(SocketIOClient socketIOClient, DigMessage data, AckRequest ackRequest) throws Exception {
                dig(socketIOClient, data, ackRequest);
            }
        });

        /*
            * -> vote_information
            * <- vote_information, {polls: {id: long, question: string, options: string[], minimalAmount: int[], priority: int, date: string, investedMoney: int[]}[], userVotes: {id -> {optionId: string, amount: int}}}
        */
        server.addEventListener("vote_information", VotesInformationRequestMessage.class, new DataListener<VotesInformationRequestMessage>() {
            @Override
            public void onData(SocketIOClient socketIOClient, VotesInformationRequestMessage data, AckRequest ackRequest) throws Exception {
                voteInformation(socketIOClient, data, ackRequest);
            }
        });

        /*
            * -> vote, {pollId: int, optionName: string, amount: int}
            * <- vote_response, {success: boolean}
        */
        server.addEventListener("vote", VoteMessage.class, new DataListener<VoteMessage>() {
            @Override
            public void onData(SocketIOClient socketIOClient, VoteMessage data, AckRequest ackRequest) throws Exception {
                vote(socketIOClient, data, ackRequest);
            }
        });

        /*
            * -> get_store
            * <- store: {success: boolean, items: {id: int, name: string, type: int, costBuy: int, costSell:int}[]}
        */
        server.addEventListener("get_store", GetStoreMessage.class, new DataListener<GetStoreMessage>() {
            @Override
            public void onData(SocketIOClient client, GetStoreMessage data, AckRequest ackRequest) throws Exception {
                getStore(client, data, ackRequest);
            }
        });

        /*
            * -> buy_item {itemId: int, count: int}
            * <- buy_response {success: boolean, money: int}
        */
        server.addEventListener("buy_item", BuyItemMessage.class, new DataListener<BuyItemMessage>() {
            @Override
            public void onData(SocketIOClient client, BuyItemMessage data, AckRequest ackRequest) throws Exception {
                buyItem(client, data, ackRequest);
            }
        });

        /*
            * -> sell_item {itemId: int, count: int}
            * <- sell_response {success: boolean, money: int}
        */
        server.addEventListener("sell_item", SellItemMessage.class, new DataListener<SellItemMessage>() {
            @Override
            public void onData(SocketIOClient client, SellItemMessage data, AckRequest ackRequest) throws Exception {
                sellItem(client, data, ackRequest);
            }
        });

        /*
            * -> get_inventory
            * <- inventory: {items: {itemId: int, name: string, type: int, costSell:int, count: int}[]}
        */
        server.addEventListener("get_inventory", GetInventoryMessage.class, new DataListener<GetInventoryMessage>() {
            @Override
            public void onData(SocketIOClient client, GetInventoryMessage data, AckRequest ackRequest) throws Exception {
                getInventory(client, data, ackRequest);
            }
        });

        server.addEventListener("get_tree", GetTreeMessage.class, new DataListener<GetTreeMessage>() {
            @Override
            public void onData(SocketIOClient client, GetTreeMessage data, AckRequest ackRequest) throws Exception {
                getTree(client, data, ackRequest);
            }
        });

        /*
            *->throw_out {name:string, count:int}
            *->throw_out_response {success:boolean}
        */
        server.addEventListener("throw_out", ThrowOutMessage.class, new DataListener<ThrowOutMessage>() {
            @Override
            public void onData(SocketIOClient client, ThrowOutMessage data, AckRequest ackRequest) throws Exception {
                throwOut(client, data, ackRequest);
            }
        });
    }

    private static void disconnectedUser(SocketIOClient socketIOClient) {
        UUID sessionId = socketIOClient.getSessionId();
        Player userDisjoined = onlineUsers.get(sessionId);
        String name = userDisjoined.getUserName();
        int x = userDisjoined.getX();
        int y = userDisjoined.getY();
        Iterator <SocketIOClient> itBySockets = server.getAllClients().iterator();
        while (itBySockets.hasNext()) {
            SocketIOClient socket = itBySockets.next();
            if (socket.getSessionId() != sessionId)
                socket.sendEvent("user_disjoined", new UserDisjoinedBroadcastMessage(name, x, y));
        }
        if (DEBUG_WITH_DUMPING)
            AsyncEvents.dump(userDisjoined);//TODO this must acync
        onlineUsers.remove(sessionId);
        Logger.log("The client has disconnected " + name);
    }

    //API methods
    private static void joinRequest(SocketIOClient client, JoinMessage data, AckRequest ackRequest) {
        Logger.log("Hello, " + data.getUserName() + "!");
        UUID sessionId = client.getSessionId();
        String name = data.getUserName();
        Player newOnlineUser = null;

        //already connected
        boolean hasConnected = false;
        UUID prevSessionId = null;
        for (Map.Entry <UUID, Player> e : onlineUsers.entrySet())
            if (e.getValue().getUserName().equals(data.getUserName())) {
                newOnlineUser = e.getValue();
                hasConnected = true;
                prevSessionId = e.getKey();
                break;
            }

        if (hasConnected) {
            server.getClient(prevSessionId).disconnect();
            onlineUsers.remove(prevSessionId);
            onlineUsers.put(sessionId, newOnlineUser);
            client.sendEvent("start", new StartMessage(true, map.getVision(newOnlineUser)));
            return;
        }
        //finish already connected

        try {
            PreparedStatement preStatementDB;
            ResultSet resultSetDB;
            preStatementDB = connectionToDB.prepareStatement("SELECT * FROM Users WHERE userName = ?;");
            preStatementDB.setString(1, name);
            resultSetDB = preStatementDB.executeQuery();

            if (SQLExtension.size(resultSetDB) == 0) {//new user
                preStatementDB = connectionToDB.prepareStatement("INSERT INTO Users (userName, energy, money, x, y, lastActionTime) " +
                                                                 "VALUES (?, ?, ?, ?, ?, ?);");
                int currentTime = (int)(System.currentTimeMillis() / 1000);
                preStatementDB.setString(1, name);
                preStatementDB.setInt(2, Player.MAX_ENERGY);
                preStatementDB.setInt(3, Player.DEFAULT_MONEY);
                preStatementDB.setInt(4, map.getDefaultX());
                preStatementDB.setInt(5, map.getDefaultY());
                preStatementDB.setInt(6, currentTime);
                preStatementDB.executeUpdate();

                preStatementDB = connectionToDB.prepareStatement("SELECT LAST_INSERT_ID();");
                resultSetDB = preStatementDB.executeQuery();
                resultSetDB.next();
                newOnlineUser = new Player(resultSetDB.getInt(1), name,
                        Player.MAX_ENERGY, Player.DEFAULT_MONEY, map.getDefaultX(), map.getDefaultY(), currentTime);
            } else {
                resultSetDB.next();
                newOnlineUser = new Player(resultSetDB.getInt("userId"), resultSetDB.getString("userName"),
                        resultSetDB.getInt("energy"), resultSetDB.getInt("money"), resultSetDB.getInt("x"), resultSetDB.getInt("y"), resultSetDB.getInt("lastActionTime"));

                int currentMidnight = (int)(System.currentTimeMillis() / 1000);
                currentMidnight /= SECONDS_PER_DAY;
                currentMidnight *= SECONDS_PER_DAY;
                if (newOnlineUser.getLastActionTime() < currentMidnight) {
                    int last = (int)(System.currentTimeMillis() / 1000);
                    newOnlineUser.setEnergy(Player.MAX_ENERGY);
                    newOnlineUser.setLastActionTime(last);
                    preStatementDB = connectionToDB.prepareStatement("UPDATE Users SET energy = ?, lastActionTime = ? WHERE userId = ?;");
                    preStatementDB.setInt(1, Player.MAX_ENERGY);
                    preStatementDB.setInt(2, (int)(System.currentTimeMillis() / 1000));
                    preStatementDB.setInt(3, newOnlineUser.getUserId());
                    preStatementDB.executeUpdate();
                }
                preStatementDB.close();
                Map <Integer, InventoryItem> invdb = loadInventoryFromDatabase(newOnlineUser.getUserId());
                newOnlineUser.setInventory(invdb);
            }
            onlineUsers.put(sessionId, newOnlineUser);
        } catch (Exception e) {
            client.sendEvent("start", new StartMessage(false));
            e.printStackTrace();
            System.exit(0);
        }
        client.sendEvent("start", new StartMessage(true, map.getVision(newOnlineUser)));

        Iterator <SocketIOClient> itBySockets = server.getAllClients().iterator();
        while (itBySockets.hasNext()) {
            SocketIOClient socket = itBySockets.next();
            if (sessionId != socket.getSessionId())
                socket.sendEvent("user_joined", new UserJoinedBroadcastMessage(name, newOnlineUser.getX(), newOnlineUser.getY()));
        }
    }

    private static void moveRequest(SocketIOClient client, MoveMessage data, AckRequest ackRequest) {
        int dx = data.getDx();
        int dy = data.getDy();
        UUID sessionId = client.getSessionId();
        Player state = onlineUsers.get(client.getSessionId());
        MoveResponseMessage moveResponseMessage;
        if (map.canMove(state.getX(), state.getY(), dx, dy) && Math.abs(dx) + Math.abs(dy) == 1) {
            moveResponseMessage = new MoveResponseMessage(true, map.getNextLayer(state, dx, dy), dx, dy);
            client.sendEvent("move_response", moveResponseMessage);
            Iterator <SocketIOClient> itBySockets = server.getAllClients().iterator();
            while (itBySockets.hasNext()) {
                SocketIOClient socket = itBySockets.next();
                if (socket.getSessionId() != sessionId)
                    socket.sendEvent("move", new MoveBroadcastMessage(state.getUserName(), state.getX(), state.getY(), dx, dy));
            }
            state.move(dx, dy);
        } else {
            moveResponseMessage = new MoveResponseMessage(false);
            client.sendEvent("move_response", moveResponseMessage);
        }
    }

    private static void getState(SocketIOClient client, StateMessage data, AckRequest ackRequest) {
        Player state = onlineUsers.get(client.getSessionId());
        client.sendEvent("state", new StateMessage(state.getMoney(), state.getEnergy(), state.getX(), state.getY()));
    }

    private static void getMap(SocketIOClient client, MapMessage data, AckRequest ackRequest) {
        client.sendEvent("map", new MapMessage(map.getField()));
    }

    private static boolean occurredProbably(int percent) {
        return helpfulRandom.nextInt(100) < percent;
    }

    private static void dig(SocketIOClient client, DigMessage data, AckRequest ackRequest) {//TODO write dig
        final int PROBABLY_LUCKY = 70;
        final int PROBABLY_EXTRA_ITEM = 50;
        final int ENERGY_GRASS = 8;
        final int ENERGY_DESERT = 5;
        final double ADD_PROBABLY = 0.2;
        final int ADD_COUNT = 1;
        final int MAX_COUNT_FIND = 3;

        Player state = onlineUsers.get(client.getSessionId());
        int x = state.getX();
        int y = state.getY();
        int energy = (map.getValue(x, y) == MapState.Field.GRASS ? ENERGY_GRASS : ENERGY_DESERT);
        if (x == map.getShopX() && y == map.getShopY() || state.getEnergy() < energy) {
            client.sendEvent("dig_response", new DigResponseMessage(0, "", 0, 0, 0, state.getEnergy()));
            return;
        }

        if (occurredProbably(PROBABLY_LUCKY)) {
            Map <Integer, Item> items = shop.getAvailableItems();
            Item[] srt = new Item[items.size()];
            int it = 0;
            for (Map.Entry <Integer, Item> e : items.entrySet())
                srt[it++] = e.getValue();
            Arrays.sort(srt, 0, srt.length, new Comparator<Item>() {
                @Override
                public int compare(Item o1, Item o2) {
                    if (o1.getCostBuy() < o2.getCostBuy())
                        return -1;
                    if (o1.getCostBuy() == o2.getCostBuy())
                        return 0;
                    return 1;
                }
            });
            double totalSum = 0;
            for (int j = 0; j < srt.length; ++j)
                totalSum += 1.0 / srt[j].getCostBuy();

            Item found = null;
            double probably = Math.min(helpfulRandom.nextDouble() +
                    (map.getValue(x, y) == MapState.Field.GRASS && occurredProbably(PROBABLY_EXTRA_ITEM)? ADD_PROBABLY : 0), 1);
            double sum = 0;
            for (int i = 0; i < srt.length; ++i) {
                sum += (1.0 / srt[i].getCostBuy()) / totalSum;
                if (sum >= probably) {
                    found = srt[i];
                    break;
                }
            }
            int count = Math.abs(helpfulRandom.nextInt()) % MAX_COUNT_FIND + 1;
            count += (map.getValue(x, y) == MapState.Field.GRASS ? ADD_COUNT : 0);
            state.addEnergy(-energy);
            state.setLastActionTime((int)(System.currentTimeMillis() / 1000));
            state.addItems(new InventoryItem(found), count);
            client.sendEvent("dig_response", new DigResponseMessage(found.getId(), found.getName(), found.getCostSell(),
                    count, found.getType(), state.getEnergy()));
        } else {
            state.addEnergy(-energy);
            client.sendEvent("dig_response", new DigResponseMessage(0, "", 0, 0, 0, state.getEnergy()));
        }
    }

    //Vote
    private static void voteInformation(SocketIOClient client, VotesInformationRequestMessage data, AckRequest ackRequest) {
        Player state = onlineUsers.get(client.getSessionId());
        client.sendEvent("vote_information", new VotesInformationResponseMessage(VotesState.getActivePolls(),
                VotesState.getUserVotes(state)));
    }

    static double getProgress(double s, double ai, int x) {
        if (x == 0)
            return 0;
        double l = ai * Math.log(s) - s * Math.log(s);
        if (ai < 0.0001)
            l = 0;
        double r = ai * Math.log(s + x) + x - s * Math.log(s + x);
        return r - l;
    }

    private static void vote(SocketIOClient client, VoteMessage data, AckRequest ackRequest) {
        Logger.log("new vote from user");
        Player state = onlineUsers.get(client.getSessionId());
        boolean result = VotesState.vote(state, data.getId(), data.getOption(), data.getAmount());
        client.sendEvent("vote_response", new VoteResponseMessage(result));

        if (result) {
            final int THRESHOLD = 1000;
            int amount = data.getAmount(), s = 0, ai = 0;
            Poll poll = VotesState.getPollById(data.getId());
            int[] invMoney = poll.getInvestedMoney();
            String[] optName = poll.getOptionsName();
            for (int i = 0; i < invMoney.length; ++i) {
                s += invMoney[i];
                if (optName[i].equals(data.getOption()))
                    ai = invMoney[i];
            }
            double addProgress = getProgress(s, ai, amount);
            Tree tree = Tree.extractTree(connectionToDB);
            int level = tree.getLevelByName(data.getOption());
            addProgress *= 1.0 * level / THRESHOLD;
            try {
                PreparedStatement preStatementDB;
                preStatementDB = connectionToDB.prepareStatement("UPDATE Tree SET progress = LEAST(100, progress + ?) WHERE name = ?;");
                preStatementDB.setDouble(1, addProgress);
                preStatementDB.setString(2, data.getOption());
                preStatementDB.executeUpdate();
                preStatementDB.close();
            } catch (SQLException e) {
                Logger.log("Exception when update tree vote!");
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    private static boolean newPoll(String fromOption, String question, String[] optionsName, int[] minimalAmount) {
        boolean result = Tree.addVertexesInDatabase(connectionToDB, fromOption, optionsName);
        if (!result)
            return false;
        Poll poll = VotesState.addActivePoll(question, optionsName, minimalAmount);
        server.getBroadcastOperations().sendEvent("new_vote", new NewVoteBroadcastMessage(poll));
        return true;
    }

    //Shop
    private static void getStore(SocketIOClient client, GetStoreMessage data, AckRequest ackRequest) {
        Player state = onlineUsers.get(client.getSessionId());
        if (state.getX() != map.getShopX() || state.getY() != map.getShopY())
            client.sendEvent("store", new StoreMessage(false));
        else
            client.sendEvent("store", new StoreMessage(true, shop.getAvailableItems()));
    }

    private static void buyItem(SocketIOClient client, BuyItemMessage data, AckRequest ackRequest) {
        Player state = onlineUsers.get(client.getSessionId());
        if (state.getX() != map.getShopX() || state.getY() != map.getShopY())
            client.sendEvent("buy_response", new BuyResponseMessage(false, state.getMoney()));
        else
            client.sendEvent("buy_response", new BuyResponseMessage(shop.buyItem(state, data.getItemId(),
                    data.getCount()), state.getMoney()));
    }

    private static void sellItem(SocketIOClient client, SellItemMessage data, AckRequest ackRequest) {
        Player state = onlineUsers.get(client.getSessionId());
        if (state.getX() != map.getShopX() || state.getY() != map.getShopY())
            client.sendEvent("sell_response", new SellResponseMessage(false, state.getMoney()));
        else
            client.sendEvent("sell_response", new SellResponseMessage(shop.sellItem(state, data.getItemId(),
                    data.getCount()), state.getMoney()));
    }

    //Inventory
    public static void getInventory(SocketIOClient client, GetInventoryMessage data, AckRequest ackRequest) {
        Player state = onlineUsers.get(client.getSessionId());
        client.sendEvent("inventory", new InventoryMessage(state.getInventory()));
    }

    public static void throwOut(SocketIOClient client, ThrowOutMessage data, AckRequest ackRequest) {
        Player player = onlineUsers.get(client.getSessionId());
        Map <Integer, InventoryItem> inv = player.getInventory();
        InventoryItem item = null;
        for (Map.Entry <Integer, InventoryItem> e : inv.entrySet())
            if (e.getValue().getName().equals(data.getName()))
                item = e.getValue();
        if (item == null)
            client.sendEvent("throw_out_response", new ThrowOutResponseMessage(false));
        else {
            if (item.getCount() < data.getCount())
                client.sendEvent("throw_out_response", new ThrowOutResponseMessage(false));
            else {
                player.addItems(item, -data.getCount());
                client.sendEvent("throw_out_response", new ThrowOutResponseMessage(true));
            }
        }
    }

    //Tree
    public static void getTree(SocketIOClient client, GetTreeMessage data, AckRequest ackRequest) {
        Tree tree = Tree.extractTree(connectionToDB);
        Node[] arr = tree.getNodeOnPlane();
        for (int i = 0; i < arr.length; ++i)
            System.err.println(arr[i].getX() + " " + arr[i].getY());
        client.sendEvent("tree", new TreeMessage(tree.getNodeOnPlane()));
    }
}