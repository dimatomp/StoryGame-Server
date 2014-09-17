package ru.ifmo.ctddev.games.messages;

/**
 * Created by Aksenov239 on 28.08.2014.
 */
public class MoveMessage {
    private int dx;
    private int dy;

    public MoveMessage() {
    }

    public MoveMessage(int dx, int dy) {
        super();
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() {
        return dx;
    }
    public int getDy() {
        return dy;
    }
}
