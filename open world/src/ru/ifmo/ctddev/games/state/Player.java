package ru.ifmo.ctddev.games.state;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by Aksenov239 on 30.08.2014.
 */
public class Player {
    public static int MAX_ENERGY = 100;
    public static int DEFAULT_MONEY = 100;
    public static int DEFAULT_VISIBLE = 3;

    private int userId;
    private String userName;
    private int x, y;
    private int energy;
    private int money;
    private int lastActionTime;
    private Map<Integer, InventoryItem> inventory;

    public Player(int userId, String userName, int energy, int money, int x, int y, int lastAction) {
        this.userId = userId;
        this.userName = userName;
        this.energy = energy;
        this.money = money;
        this.x = x;
        this.y = y;
        this.lastActionTime = lastAction;
        inventory = new HashMap<Integer, InventoryItem>();
    }

    public Map<Integer, InventoryItem> getInventory() {
        return inventory;
    }

    public void setInventory(Map<Integer, InventoryItem> inventory) {
        this.inventory = inventory;
    }

    public void addItems(InventoryItem item, int count) {
        int id = item.getId();
        if (!inventory.containsKey(id))
            inventory.put(id, new InventoryItem(item.getId(), item.getName(), item.getCostSell(), item.getType()));
        InventoryItem cur = inventory.get(id);
        cur.addCount(count);
        inventory.put(id, cur);
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
