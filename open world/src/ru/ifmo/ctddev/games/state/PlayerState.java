package ru.ifmo.ctddev.games.state;

import ru.ifmo.ctddev.games.messages.UserVote;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by Aksenov239 on 30.08.2014.
 */
public class PlayerState {
    public static int MAX_ENERGY = 100;
    public static int DEFAULT_MONEY = 100;
    public static int DEFAULT_VISIBLE = 3;

    private int userId;
    private String userName;
    private int x, y;
    private int energy;
    private int money;
    private int lastActionTime;
    private Map<Long, UserVote> votes;

/*   public PlayerState() {
        x = MapState.getDefaultX();
        y = MapState.getDefaultY();
        votes = new HashMap<Long, UserVote>();
        money = 0;
    }*/

    public PlayerState(int userId, String userName, int energy, int money, int x, int y, int lastAction) {
        this.userId = userId;
        this.userName = userName;
        this.energy = energy;
        this.money = money;
        this.x = x;
        this.y = y;
        this.lastActionTime = lastAction;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getMoney() {
        return money;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public void addMoney(int add) {
        money += add;
    }

    public Map<Long, UserVote> getVotes() {
        return votes;
    }

    public int getLastActionTime() {
        return lastActionTime;
    }

    public void setLastActionTime(int lastActionTime) {
        this.lastActionTime = lastActionTime;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public int getEnergy() {
        return energy;
    }

    public int getVision() {
        return DEFAULT_VISIBLE;
    }
}
