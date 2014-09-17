package ru.ifmo.ctddev.games.messages;

/**
 * Created by pva701 on 9/18/14.
 */
public class UserDisjoinedBroadcastMessage {
    private String userName;
    private int x, y;

    public UserDisjoinedBroadcastMessage(String userName, int x, int y) {
        this.userName = userName;
        this.x = x;
        this.y = y;
    }
}
