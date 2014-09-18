package ru.ifmo.ctddev.games.state;

import ru.ifmo.ctddev.games.messages.UserVote;

import java.sql.Connection;
import java.util.ArrayList;
import java.sql.*;

/**
 * Created by Aksenov239 on 30.08.2014.
 */

public class VotesState {
    static ArrayList <Poll> activePolls;
    //private static int id = 1;//TODO SHIT, must take from database
    // TODO: There obviously should be synchronization

    static Connection connectionToDB;
    static PreparedStatement preStatementDB;
    public static void setDatabase(Connection db) {
        connectionToDB = db;
    }

    public static void loadActivePolls() {
        try {
            preStatementDB = connectionToDB.prepareStatement("SELECT * FROM Polls WHERE active = ?;");
            preStatementDB.setBoolean(1, true);
            ResultSet resultSetDB = preStatementDB.executeQuery();
            while (resultSetDB.next()) {
                int pollId = resultSetDB.getInt("pollId");
                String question = resultSetDB.getString("question");
                preStatementDB = connectionToDB.prepareStatement("SELECT * FROM Options WHERE pollId = ?;");
                preStatementDB.setInt(1, pollId);
                ResultSet allOptions = preStatementDB.executeQuery();
                int size = allOptions.getFetchSize();
                int[] optionsId = new int[size];
                String[] optionsName = new String[size];
                int[] minimalAmount = new int[size];
                int[] investedMoney = new int[size];
                for (int i = 0; allOptions.next(); ++i) {
                    optionsId[i] = allOptions.getInt("optionId");
                    optionsName[i] = allOptions.getString("optionName");
                    minimalAmount[i] = allOptions.getInt("minimalAmount");
                    investedMoney[i] = allOptions.getInt("investedMoney");
                }
                activePolls.add(new Poll(pollId, question, optionsId, optionsName, minimalAmount, investedMoney));
            }
            resultSetDB.close();
        } catch (SQLException e) {
            System.err.println("loadActivePolls exception!");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public VotesState() {
        activePolls = new ArrayList<Poll>();
    }

    public static Poll addActivePoll(String question, String[] optionsName, int minimalAmount[]) {
        try {
            ResultSet resultSetDB;
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
            activePolls.add(ret);
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
            preStatementDB = connectionToDB.prepareStatement("UPDATE Polls SET active = ? WHERE pollId = ?;");
            preStatementDB.setBoolean(1, false);
            preStatementDB.setInt(2, idDel);
            for (int i = 0; i < activePolls.size(); ++i)
                if (activePolls.get(i).getId() == idDel) {
                    activePolls.remove(i);
                    break;
                }
        } catch (SQLException e) {
            System.err.println("closePoll exception!");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static UserVote[] getUserVotes(PlayerState state) {
        try {
            ArrayList <UserVote> ar = new ArrayList<UserVote>();
            preStatementDB = connectionToDB.prepareStatement("SELECT * FROM UserVotes WHERE userId = ?;");
            preStatementDB.setInt(1, state.getUserId());
            ResultSet resultSetDB = preStatementDB.executeQuery();
            while (resultSetDB.next()) {
                int optionId = resultSetDB.getInt("optionId");
                int money = resultSetDB.getInt("money");
                for (int i = 0; i < activePolls.size(); ++i)
                    if (activePolls.get(i).containsOption(optionId))
                        ar.add(new UserVote(activePolls.get(i).getId(), activePolls.get(i).getOptionName(optionId), money));
            }
            UserVote[] ret = new UserVote[ar.size()];
            for (int i = 0; i < ret.length; ++i)
                ret[i] = ar.get(i);
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
        for (int i = 0; i < activePolls.size(); ++i)
            result[i] = activePolls.get(i);
        return result;
    }

    public static boolean vote(PlayerState state, int pollId, String optionName, int amount) {//TODO
        Poll poll = null;
        for (int i = 0; i < activePolls.size(); ++i)
            if (activePolls.get(i).getId() == pollId)
                poll = activePolls.get(i);
        if (poll == null || !poll.containsOption(optionName))
            return false;
        int optionId = poll.getOptionId(optionName);
        if (amount < poll.getMinimalAmountById(optionId))
            return false;

        poll.addInvestedMoney(optionName, amount);
        try {
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
