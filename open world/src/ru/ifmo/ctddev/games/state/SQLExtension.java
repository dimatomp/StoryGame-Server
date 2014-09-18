package ru.ifmo.ctddev.games.state;

import java.sql.ResultSet;

/**
 * Created by pva701 on 9/18/14.
 */
public class SQLExtension {
    public static int size(ResultSet r) {
        try {
            int size = 0;
            while (r.next())
                ++size;
            r.beforeFirst();
            return size;
        } catch (Exception e) {
            System.err.println("size exception");
            System.exit(0);
        }
        return 0;
    }
}
