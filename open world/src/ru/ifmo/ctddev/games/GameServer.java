/**
 * Created by Aksenov239 on 28.08.2014.
 */

package ru.ifmo.ctddev.games;

import com.corundumstudio.socketio.listener.*;
import com.corundumstudio.socketio.*;
import ru.ifmo.ctddev.games.messages.*;
import ru.ifmo.ctddev.games.messages.JoinMessage;
import ru.ifmo.ctddev.games.state.MapState;
import ru.ifmo.ctddev.games.state.PlayerState;
import ru.ifmo.ctddev.games.state.Poll;
import ru.ifmo.ctddev.games.state.VotesState;

import java.io.IOException;
import java.util.*;
import java.sql.*;

public class GameServer {
    final static Map <UUID, PlayerState> onlineUsers = new HashMap<UUID, PlayerState>();
    final static int SECONDS_PER_DAY = 24 * 3600;
    static SocketIOServer server;

    static MapState map;
    static VotesState votesState;

    static String NAME_OF_DB;
    static String USER;
    static String PASSWORD;
    static Connection connectionToDB;
    static PreparedStatement preStatementDB;
    static ResultSet resultSetDB;

    class CommandReader implements Runnable {
        @Override
        public void run() {
            Scanner in = new Scanner(System.in);
            while (true) {
                String cmd = in.nextLine();
                //
            }
        }
    }

    private static void initialization() {
        map = new MapState();
        votesState = new VotesState();
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            System.in.close();
            System.out.close();
        } catch (IOException e) {
            System.err.println("Failed to daemonize myself.");
            System.exit(0);
        }

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
        System.err.println("Load active polls...");
        VotesState.setDatabase(connectionToDB);
        VotesState.loadActivePolls();
        System.err.println("Successful load active polls!");

        initialization();
        Configuration config = new Configuration();
        config.setHostname(ip);
        config.setPort(port);

        server = new SocketIOServer(config);

        System.err.println("The server has started!");

        //New user connected
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient socketIOClient) {
                System.err.println("New client has connected");
            }
        });

        //User disconnected
        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {
                System.err.println("The client has disconnected");
                UUID sessionId = socketIOClient.getSessionId();
                PlayerState userDisjoined = onlineUsers.get(sessionId);
                String name = userDisjoined.getUserName();
                int x = userDisjoined.getX();
                int y = userDisjoined.getY();
                Iterator <SocketIOClient> itBySockets = server.getAllClients().iterator();
                while (itBySockets.hasNext()) {
                    SocketIOClient socket = itBySockets.next();
                    if (socket.getSessionId() != sessionId)
                        socket.sendEvent("user_disjoined", new UserDisjoinedBroadcastMessage(name, x, y));
                }
                onlineUsers.remove(sessionId);
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

        server.start();
        Thread.sleep(30000);
        newVote("This pull works?", new String[] {"Yes", "No"}, new int[] {10, 12});
        System.err.println("New vote was sent!");
        /*System.out.println("Sending test poll.");
        votesState.addActivePoll("How about now?", new int[]{0, 0}, "Yes", "No");
        server.getBroadcastOperations().sendEvent("new_vote");*/

        Thread.sleep(Integer.MAX_VALUE);

        server.stop();
    }

    //API methods
    private static void joinRequest(SocketIOClient client, JoinMessage data, AckRequest ackRequest) {
        System.err.println("Hello, " + data.getUserName() + "!");
        UUID sessionId = client.getSessionId();
        String name = data.getUserName();
        PlayerState newOnlineUser = null;

        try {
            preStatementDB = connectionToDB.prepareStatement("SELECT * FROM Users WHERE userId = ?;");
            preStatementDB.setString(1, name);
            resultSetDB = preStatementDB.executeQuery();
            if (resultSetDB.getFetchSize() == 0) {//new user
                preStatementDB = connectionToDB.prepareStatement("INSERT INTO Users (userName, energy, money, x, y, lastActionTime) " +
                                                                 "VALUES (?, ?, ?, ?, ?, ?);");
                int currentTime = (int)(System.currentTimeMillis() / 1000);
                preStatementDB.setString(1, name);
                preStatementDB.setInt(2, PlayerState.MAX_ENERGY);
                preStatementDB.setInt(3, PlayerState.DEFAULT_MONEY);
                preStatementDB.setInt(4, map.getDefaultX());
                preStatementDB.setInt(5, map.getDefaultY());
                preStatementDB.setInt(6, currentTime);
                preStatementDB.executeUpdate();

                preStatementDB = connectionToDB.prepareStatement("SELECT LAST_INSERT_ID();");
                resultSetDB = preStatementDB.executeQuery();
                resultSetDB.next();
                newOnlineUser = new PlayerState(resultSetDB.getInt(1), name,
                        PlayerState.MAX_ENERGY, PlayerState.DEFAULT_MONEY, map.getDefaultX(), map.getDefaultY(), currentTime);
                onlineUsers.put(sessionId, newOnlineUser);
            } else {
                preStatementDB = connectionToDB.prepareStatement("SELECT * FROM Users WHERE userName = ?;");
                preStatementDB.setString(1, name);
                resultSetDB = preStatementDB.executeQuery();
                resultSetDB.next();
                newOnlineUser = new PlayerState(resultSetDB.getInt("userId"), resultSetDB.getString("userName"),
                        resultSetDB.getInt("energy"), resultSetDB.getInt("money"), resultSetDB.getInt("x"), resultSetDB.getInt("y"), resultSetDB.getInt("lastActionTime"));

                int currentMidnight = (int)(System.currentTimeMillis() / 1000);
                currentMidnight /= SECONDS_PER_DAY;
                currentMidnight *= SECONDS_PER_DAY;
                if (newOnlineUser.getLastActionTime() < currentMidnight) {
                    int last = (int)(System.currentTimeMillis() / 1000);
                    newOnlineUser.setEnergy(PlayerState.MAX_ENERGY);
                    newOnlineUser.setLastActionTime(last);
                    preStatementDB = connectionToDB.prepareStatement("UPDATE Users SET energy = ?, lastActionTime = ? WHERE userId = ?;");
                    preStatementDB.setInt(1, PlayerState.MAX_ENERGY);
                    preStatementDB.setInt(2, (int)(System.currentTimeMillis() / 1000));
                    preStatementDB.setInt(3, newOnlineUser.getUserId());
                    preStatementDB.executeUpdate();
                }
                onlineUsers.put(sessionId, newOnlineUser);
            }
            preStatementDB.close();
        } catch (Exception e) {
            client.sendEvent("start", new StartMessage(false));
            e.printStackTrace();
            System.exit(0);
        }
        client.sendEvent("start", new StartMessage(true, map.getVision(newOnlineUser)));

        Iterator <SocketIOClient> itBySockets = server.getAllClients().iterator();
        while (itBySockets.hasNext()) {
            SocketIOClient socket = itBySockets.next();
            socket.sendEvent("user_joined", new UserJoinedBroadcastMessage(name, newOnlineUser.getX(), newOnlineUser.getY()));
        }
    }

    private static void moveRequest(SocketIOClient client, MoveMessage data, AckRequest ackRequest) {
        int dx = data.getDx();
        int dy = data.getDy();
        UUID sessionId = client.getSessionId();
        PlayerState state = onlineUsers.get(client.getSessionId());
        MoveResponseMessage moveResponseMessage;
        if (map.canMove(state.getX(), state.getY(), dx, dy)) {
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
        PlayerState state = onlineUsers.get(client.getSessionId());
        client.sendEvent("state", new StateMessage(state.getMoney(), state.getEnergy(), state.getX(), state.getY()));
    }

    private static void getMap(SocketIOClient client, MapMessage data, AckRequest ackRequest) {
        client.sendEvent("map", new MapMessage(map.getField()));
    }

    private static void dig(SocketIOClient client, DigMessage data, AckRequest ackRequest) {

    }

    //Vote
    private static void voteInformation(SocketIOClient client, VotesInformationRequestMessage data, AckRequest ackRequest) {
        PlayerState state = onlineUsers.get(client.getSessionId());
        client.sendEvent("vote_information", new VotesInformationResponseMessage(VotesState.getActivePolls(), VotesState.getUserVotes(state)));
    }

    private static void vote(SocketIOClient client, VoteMessage data, AckRequest ackRequest) {
        PlayerState state = onlineUsers.get(client.getSessionId());
        boolean result = VotesState.vote(state, data.getId(), data.getOption(), data.getAmount());
        client.sendEvent("vote_response", new VoteResponseMessage(result));
    }

    private static void newVote(String question, String[] optionsName, int[] minimalAmount) {
        Poll poll = VotesState.addActivePoll(question, optionsName, minimalAmount);
        server.getBroadcastOperations().sendEvent("new_vote", new NewVoteBroadcastMessage(poll));
    }
}