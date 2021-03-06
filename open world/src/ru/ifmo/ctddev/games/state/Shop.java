package ru.ifmo.ctddev.games.state;

import ru.ifmo.ctddev.games.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pva701 on 9/18/14.
 */
public class Shop {
    private Connection connectionToDB;
    private PreparedStatement preStatementDB;
    private Map<Integer, Item> items;

    public Shop(Connection connection) {
        connectionToDB = connection;
    }

    public void loadAvailableItems() {
        try {
            items = new HashMap<Integer, Item>();
            preStatementDB = connectionToDB.prepareStatement("SELECT * FROM Items;");
            ResultSet resultSet = preStatementDB.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("itemId");
                String name = resultSet.getString("name");
                int type = resultSet.getInt("type");
                int costBuy = resultSet.getInt("costBuy");
                int costSell = resultSet.getInt("costSell");
                items.put(id, new Item(id, name, type, costBuy, costSell));
            }
            resultSet.close();
            preStatementDB.close();
        } catch (Exception e) {
            Logger.log("loadAvailableItems exception!");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void addItem(Item it) {
        try {
            preStatementDB = connectionToDB.prepareStatement("INSERT INTO Items " +
                    "(name, type, costBuy, costSell) VALUE(?, ?, ?, ?);");
            preStatementDB.setString(1, it.getName());
            preStatementDB.setInt(2, it.getType());
            preStatementDB.setInt(3, it.getCostBuy());
            preStatementDB.setInt(4, it.getCostSell());
            preStatementDB.executeUpdate();
            preStatementDB.close();

            preStatementDB = connectionToDB.prepareStatement("SELECT LAST_INSERT_ID();");
            ResultSet resultSetDB = preStatementDB.executeQuery();
            resultSetDB.next();
            int id = resultSetDB.getInt("itemId");
            it.setId(id);
            items.put(id, it);
            preStatementDB.close();
        } catch (SQLException e) {
            Logger.log("Exception in addItem!");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public Map <Integer, Item> getAvailableItems() {
        return items;
    }

    public boolean buyItem(Player state, int itemId, int count) {
        if (!items.containsKey(itemId))
            return false;
        Item cur = items.get(itemId);
        if (state.getMoney() < count * cur.getCostBuy())
            return false;
        state.addItems(new InventoryItem(cur), count);
        state.addMoney(-count * cur.getCostBuy());
        return true;
    }

    public boolean sellItem(Player state, int itemId, int count) {
        if (!items.containsKey(itemId))
            return false;
        InventoryItem cur = state.getInventory().get(itemId);
        if (cur == null || cur.getCount() < count)
            return false;
        state.addItems(cur, -count);
        state.addMoney(count * cur.getCostSell());
        return true;
    }
}
