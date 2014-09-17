package ru.ifmo.ctddev.games.messages;

/**
 * Created by pva701 on 9/18/14.
 */
public class UserJoinedBroadcastMessage {
    private String userName;
    private int x, y;

    public UserJoinedBroadcastMessage(String userName, int y, int x) {
        this.userName = userName;
        this.y = y;
        this.x = x;
    }
}
