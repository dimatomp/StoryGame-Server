package ru.ifmo.ctddev.games.messages;

/**
 * Created by pva701 on 9/17/14.
 */
public class StateMessage {
    int money;
    int energy;
    int x, y;

    public StateMessage(int money, int energy, int x, int y) {
        this.money = money;
        this.energy = energy;
        this.x = x;
        this.y = y;
    }
}
