package ru.ifmo.ctddev.games.messages;

/**
 * Created by pva701 on 9/18/14.
 */
public class DigResponseMessage {
    private int type;
    private int amount;
    private int energy;

    public DigResponseMessage(int type, int amount, int energy) {
        this.type = type;
        this.amount = amount;
        this.energy = energy;
    }
}
