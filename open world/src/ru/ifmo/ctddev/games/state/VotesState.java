package ru.ifmo.ctddev.games.state;

import ru.ifmo.ctddev.games.messages.UserVote;

import java.sql.Connection;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Aksenov239 on 30.08.2014.
 */

public class VotesState {
    private static Map<Integer, Poll> activePolls;
    // TODO: There obviously should be synchronization

    static Connection connectionToDB;
    public static void setDatabase(Connection db) {
        connectionToDB = db;
    }

    public static void loadActivePolls() {
        activePolls = new HashMap<Integer, Poll>();
        try {
            PreparedStatement preStatementDB = connectionToDB.prepareStatement("SELECT * FROM Polls WHERE active = ?;");
            preStatementDB.setBoolean(1, true);
            ResultSet resultSetDB = preStatementDB.executeQuery();
            while (resultSetDB.next()) {
                int pollId = resultSetDB.getInt("pollId");
                String question = resultSetDB.getString("question");
                preStatementDB = connectionToDB.prepareStatement("SELECT * FROM Options WHERE pollId = ?;");
                preStatementDB.setInt(1, pollId);
                ResultSet allOptions = preStatementDB.executeQuery();
                int size = SQLExtension.size(allOptions);
                int[] optionsId = new int[size];
                String[] optionsName = new String[size];
                int[] minimalAmount = new int[size];
                int[] investedMoney = new int[size];
                for (int i = 0; i < size; ++i) {
                    allOptions.next();
                    optionsId[i] = allOptions.getInt("optionId");
                    optionsName[i] = allOptions.getString("optionName");
                    minimalAmount[i] = allOptions.getInt("minimalAmount");
                    investedMoney[i] = allOptions.getInt("investedMoney");
                }
                activePolls.put(pollId, new Poll(pollId, question, optionsId, optionsName, minimalAmount, investedMoney));
            }
            resultSetDB.close();
        } catch (SQLException e) {
            System.err.println("loadActivePolls exception!");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public VotesState() {

    }

    public static Poll addActivePoll(String question, String[] optionsName, int minimalAmount[]) {
        try {
            ResultSet resultSetDB;
            PreparedStatement preStatementDB;
            preStatementDB = connectionToDB.prepareStatement("INSERT INTO Polls (question, active) VALUES (?, ?);");
            preStatementDB.setString(1, question);
            preStatementDB.setBoolean(2, true);
            preStatementDB.executeUpdate();

            preStatementDB = connectionToDB.prepareStatement("SELECT LAST_INSERT_ID();");
            resultSetDB = preStatementDB.executeQuery();
            resultSetDB.next();
            int pollId = resultSetDB.getInt(1);
            int[] optionsId = new int[optionsName.length];
            int[] investedMoney = new int[optionsName.length];
            for (int i = 0; i < optionsName.length; ++i) {
                preStatementDB = connectionToDB.prepareStatement("INSERT INTO Options (pollId, optionName, minimalAmount, investedMoney) VALUES (?, ?, ?, ?);");
                preStatementDB.setInt(1, pollId);
                preStatementDB.setString(2, optionsName[i]);
                preStatementDB.setInt(3, minimalAmount[i]);
                preStatementDB.setInt(4, 0);
                preStatementDB.executeUpdate();

                preStatementDB = connectionToDB.prepareStatement("SELECT LAST_INSERT_ID();");
                resultSetDB = preStatementDB.executeQuery();
                resultSetDB.next();
                optionsId[i] = resultSetDB.getInt(1);
                investedMoney[i] = 0;
            }
            Poll ret = new Poll(pollId, question, optionsId, optionsName, minimalAmount, investedMoney);
            activePolls.put(pollId, ret);
            preStatementDB.close();
            return ret;
        } catch (SQLException e) {
            System.err.println("addActivePoll exception!");
            e.printStackTrace();
            System.exit(0);
        }
        return null;
    }

    public static void closePoll(int idDel) {
        try {
            PreparedStatement preStatementDB;
            preStatementDB = connectionToDB.prepareStatement("UPDATE Polls SET active = ? WHERE pollId = ?;");
            preStatementDB.setBoolean(1, false);
            preStatementDB.setInt(2, idDel);
            preStatementDB.executeUpdate();
            activePolls.remove(idDel);
        } catch (SQLException e) {
            System.err.println("closePoll exception!");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static Map <Integer, UserVote> getUserVotes(Player state) {
        try {
            Map <Integer, UserVote> ret = new HashMap<Integer, UserVote>();
            PreparedStatement preStatementDB;
            preStatementDB = connectionToDB.prepareStatement("SELECT * FROM UserVotes WHERE userId = ?;");
            preStatementDB.setInt(1, state.getUserId());
            ResultSet resultSetDB = preStatementDB.executeQuery();
            while (resultSetDB.next()) {
                int optionId = resultSetDB.getInt("optionId");
                int money = resultSetDB.getInt("amount");
                for (Map.Entry <Integer, Poll> e : activePolls.entrySet()) {
                    Poll pollIt = e.getValue();
                    if (pollIt.containsOption(optionId))
                        ret.put(pollIt.getId(), new UserVote(pollIt.getId(), pollIt.getOptionName(optionId), money));
                }
            }
            return ret;
        } catch (SQLException e) {
            System.err.println("getUserVotes exception!");
            e.printStackTrace();
            System.exit(0);
        }
        return null;
    }

    public static Poll[] getActivePolls() {
        Poll[] result = new Poll[activePolls.size()];
        int i = 0;
        for (Map.Entry <Integer, Poll> e : activePolls.entrySet())
            result[i++] = e.getValue();
        return result;
    }

    public static boolean vote(Player state, int pollId, String optionName, int amount) {//TODO
        Poll poll = null;
        for (int i = 0; i < activePolls.size(); ++i)
            if (activePolls.get(i).getId() == pollId)
                poll = activePolls.get(i);
        if (poll == null || !poll.containsOption(optionName))
            return false;
        int optionId = poll.getOptionId(optionName);
        if (amount < poll.getMinimalAmountById(optionId))
            return false;

        poll.vote(optionId, amount);
        try {
            PreparedStatement preStatementDB;
            preStatementDB = connectionToDB.prepareStatement("UPDATE Options SET investedMoney = investedMoney + ? WHERE optionId = ?;");
            preStatementDB.setInt(1, amount);
            preStatementDB.setInt(2, optionId);
            preStatementDB.executeUpdate();

            preStatementDB = connectionToDB.prepareStatement("INSERT INTO UserVotes (userId, optionId, amount) VALUES (?, ?, ?);");
            preStatementDB.setInt(1, state.getUserId());
            preStatementDB.setInt(2, optionId);
            preStatementDB.setInt(3, amount);
            preStatementDB.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("vote exception!");
            e.printStackTrace();
            System.exit(0);
        }
        return false;
    }
}
