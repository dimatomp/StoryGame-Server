package ru.ifmo.ctddev.games.state;

import ru.ifmo.ctddev.games.messages.UserVote;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by Aksenov239 on 30.08.2014.
 */
public class PlayerState {
    private int x, y;
    private int currentMoveSpeed;
    private int level;
    private int money;
    private Map<Long, UserVote> votes;

    public PlayerState() {
        x = MapState.getDefaultX();
        y = MapState.getDefaultY();
        currentMoveSpeed = 100;
        votes = new HashMap<>();
        money = 0;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getCurrentMoveSpeed() {
        return currentMoveSpeed;
    }

    public int getMoney() {
        return money;
    }
    
    public int getLevel( ) {
        return level;
    }

    public void move(int direction) {
        x += MapState.dx[direction];
        y += MapState.dy[direction];
    }

    public void addMoney(int add) {
        money += add;
    }

    public void levelUp() {
        level++;
    }

    public Map<Long, UserVote> getVotes() {
        return votes;
    }
}
