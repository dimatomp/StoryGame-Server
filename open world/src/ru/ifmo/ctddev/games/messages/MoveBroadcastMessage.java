package ru.ifmo.ctddev.games.messages;

/**
 * Created by pva701 on 9/18/14.
 */
public class MoveBroadcastMessage {
    private String userName;
    int x, y;
    int dx, dy;

    public MoveBroadcastMessage(String userName, int x, int y, int dx, int dy) {
        this.userName = userName;
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }
}
