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
import ru.ifmo.ctddev.games.state.VotesState;

import java.io.IOException;
import java.util.*;
import java.sql.*;

public class GameServer {

    //final static Map<UUID, String> socketToUsername = new HashMap<UUID, String>();
    //final static Map<String, PlayerState> usernameToState = new HashMap<String, PlayerState>();
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
        }

        ///Database initializate
        if (args.length != 3) {
            System.err.println("You don't input information about database!");
            System.exit(0);
        }
        NAME_OF_DB = args[0];
        USER = args[1];
        PASSWORD = args[2];

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            System.err.println("Class of database not found!");
            System.exit(0);
        }

        System.err.println("Connecting to database...");
        try {
            //connectionToDB = DriverManager.getConnection("jdbc:mysql://localhost/" + NAME_OF_DB, USER, PASSWORD);
            connectionToDB = DriverManager.getConnection("jdbc:mysql://localhost/" + NAME_OF_DB, USER, PASSWORD);
            System.err.println("Successful connection to database!");
        } catch (Exception ex) {
            System.err.println("Unsuccessful connection to database :(");
            ex.printStackTrace();
            System.exit(0);
        }


        initialization();
        Configuration config = new Configuration();
        config.setHostname("192.168.117.133");
        config.setPort(9092);

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

        /*server.addEventListener("vote_information", VotesInformationRequestMessage.class, new DataListener<VotesInformationRequestMessage>() {
            @Override
            public void onData(SocketIOClient socketIOClient, VotesInformationRequestMessage votesInformationRequestMessage, AckRequest ackRequest) throws Exception {
                String username = socketToUsername.get(socketIOClient.getSessionId());
                PlayerState state = usernameToState.get(username);

                Map<Long, UserVote> player_votes = state.getVotes();
                Set<Long> toRemove = new HashSet<Long>();
                for (Long id : player_votes.keySet()) {
                    if (!votesState.active(id))
                        toRemove.add(id);
                }

                for (Long id : toRemove)
                    player_votes.remove(id);

                socketIOClient.sendEvent("vote_information", new VotesInformationResponseMessage(VotesState.getVotes(), player_votes));
            }
        });

        server.addEventListener("vote", VoteMessage.class, new DataListener<VoteMessage>() {
            @Override
            public void onData(SocketIOClient socketIOClient, VoteMessage voteMessage, AckRequest ackRequest) throws Exception {
                String username = socketToUsername.get(socketIOClient.getSessionId());
                PlayerState state = usernameToState.get(username);

                int optNumber = 0;
                boolean result = VotesState.vote(state, voteMessage.getId(), voteMessage.getOption(), voteMessage.getAmount());
                socketIOClient.sendEvent("vote_result", new VoteResultMessage(result));
            }
        });*/

        server.start();

        Thread.sleep(20000);
        System.out.println("Sending test poll.");
        votesState.addPoll("How about now?", new int[]{0, 0}, "Yes", "No");
        server.getBroadcastOperations().sendEvent("new_vote");

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
            preStatementDB = connectionToDB.prepareStatement("SELECT * FROM Users WHERE userId = '1';");
            preStatementDB.setString(1, name);
            resultSetDB = preStatementDB.executeQuery();
            if (resultSetDB.getFetchSize() == 0) {//new user
                preStatementDB = connectionToDB.prepareStatement("INSERT INTO Users (userName, energy, money, x, y, lastAction) " +
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
                resultSetDB.next();
                resultSetDB = preStatementDB.executeQuery();
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
        } catch (Exception e) {
            client.sendEvent("start", new StartMessage(false));
            e.printStackTrace();
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
            moveResponseMessage = new MoveResponseMessage(true, map.getNextLayer(state, dx, dy));
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
}