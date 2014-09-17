package ru.ifmo.ctddev.games.messages;

/**
 * Created by Aksenov239 on 28.08.2014.
 */
public class MoveResponseMessage {

    private boolean success;
    private int[] layer;

    public MoveResponseMessage(boolean success) {
        this.success = success;
        layer = null;
    }

    public MoveResponseMessage(boolean success, int[] layer) {
        this.success = success;
        this.layer = layer;
    }

    public boolean getSuccess() {
        return success;
    }

    public void setSuccessful(boolean successful) {
        this.success = successful;
    }
}
