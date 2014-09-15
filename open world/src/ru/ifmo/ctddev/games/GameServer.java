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

public class GameServer {

    final static Map<UUID, String> socketToUsername = new HashMap<>();
    final static Map<String, PlayerState> usernameToState = new HashMap<>();
    final static int DEFAULT_VISION = 3;

    static MapState map;
    static VotesState votesState;

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

        initialization();

        Configuration config = new Configuration();
        config.setHostname("192.168.43.218");
        config.setPort(9092);

        final SocketIOServer server = new SocketIOServer(config);

        System.err.println("The server has started!");

        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient socketIOClient) {
                System.err.println("New client has connected");
            }
        });

        server.addEventListener("join_request", JoinMessage.class, new DataListener<JoinMessage>() {
            @Override
            public void onData(SocketIOClient client, JoinMessage data, AckRequest ackRequest) {
                try {
                    System.err.println("Hello, " + data.getUserName() + "!");
                    UUID sessionId = client.getSessionId();
                    socketToUsername.put(sessionId, data.getUserName());

                    if (!usernameToState.containsKey(data.getUserName())) {
                        usernameToState.put(data.getUserName(), new PlayerState());
                    }

                    PlayerState state = usernameToState.get(data.getUserName());

                    int[][] map = GameServer.map.getVision(state, DEFAULT_VISION);

                    StartMessage startMessage = new StartMessage(true, map);
                    client.sendEvent("start", startMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {
                System.err.println("The client has disconnected");
                UUID sessionId = socketIOClient.getSessionId();
                socketToUsername.remove(sessionId);
            }
        });

        server.addEventListener("move_request", MoveMessage.class, new DataListener<MoveMessage>() {
            @Override
            public void onData(SocketIOClient client, MoveMessage data, AckRequest ackRequest) {
                try {
                    System.err.println("Move request: " + data.getDirection());
                    int direction = data.getDirection();

                    String username = socketToUsername.get(client.getSessionId());
                    PlayerState state = usernameToState.get(username);
                    MoveResponseMessage moveResponseMessage = new MoveResponseMessage(false, null, 0, 0);

                    if (GameServer.map.canMove(state, direction)) {
                        int[] layer = GameServer.map.getNextLayer(state, DEFAULT_VISION, direction);
                        state.move(direction);

                        moveResponseMessage = new MoveResponseMessage(true, layer, state.getCurrentMoveSpeed(), direction);
                    }
                    client.sendEvent("move_response", moveResponseMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        server.addEventListener("vote_information", VotesInformationRequestMessage.class, new DataListener<VotesInformationRequestMessage>() {
            @Override
            public void onData(SocketIOClient socketIOClient, VotesInformationRequestMessage votesInformationRequestMessage, AckRequest ackRequest) throws Exception {
                String username = socketToUsername.get(socketIOClient.getSessionId());
                PlayerState state = usernameToState.get(username);

                Map<Long, UserVote> player_votes = state.getVotes();
                Set<Long> toRemove = new HashSet<>();
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
        });

        server.start();

        Thread.sleep(20000);
        System.out.println("Sending test poll.");
        votesState.addPoll("How about now?", new int[]{0, 0}, "Yes", "No");
        server.getBroadcastOperations().sendEvent("new_vote");

        Thread.sleep(Integer.MAX_VALUE);

        server.stop();
    }

}
