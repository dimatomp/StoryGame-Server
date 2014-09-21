package ru.ifmo.ctddev.games;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Created by pva701 on 9/21/14.
 */
public class Logger {
    private static PrintWriter out;
    private static boolean opened = false;

    public static void log(String s) {
        try {
            if (!opened) {
                out = new PrintWriter(new File("log.txt"));
                opened = true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        out.println(s);
        out.flush();
    }
}
