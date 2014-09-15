package ru.ifmo.ctddev.games.state;

import java.util.Random;


/**
 * Created by Aksenov239 on 30.08.2014.
 */
public class MapState {
    public enum Field {
        GRASS, DESERT, MOUNTAIN;
    }

    private Field[][] map;
    private static int defaultX = 5;
    private static int defaultY = 5;
    public static int[] dx = {1, 0, -1, 0};
    public static int[] dy = {0, 1, 0, -1};

    private Random rnd = new Random(239);

    public MapState() {
        map = new Field[11][11];

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                map[i][j] = rnd.nextBoolean() ? Field.GRASS : Field.DESERT;
            }
        }

        defaultX = 5;
        defaultY = 5;
    }

    public static int getDefaultX() {
        return defaultX;
    }

    public static int getDefaultY() {
        return defaultY;
    }

    public Field getValue(int x, int y) {
        return 0 <= x && x < map.length && 0 <= y && y < map[0].length ? map[x][y] : Field.MOUNTAIN;
    }

    public boolean canMove(PlayerState state, int direction) {
        int x = state.getX() + dx[direction];
        int y = state.getY() + dy[direction];

        return getValue(x, y) != Field.MOUNTAIN;
    }

    public int[][] getVision(PlayerState state, int vision) {
        int[][] part = new int[2 * vision + 1][2 * vision + 1];
        for (int i = state.getX() - vision; i <= state.getX() + vision; i++) {
            for (int j = state.getY() - vision; j <= state.getY() + vision; j++) {
                part[i - (state.getX() - vision)][j - (state.getY() - vision)] = getValue(i, j).ordinal();
            }
        }
        return part;
    }

    public int[] getNextLayer(PlayerState state, int vision, int direction) {
        int[] layer = new int[2 * vision + 1];

        int fixed = (direction % 2 == 0 ? state.getX() : state.getY()) + (direction < 2 ? 1 : -1) * (vision + 1);
        for (int i = 0; i < layer.length; i++) {
            layer[i] = (direction % 2 == 0 ? getValue(fixed, i - vision + state.getY()) : getValue(i - vision + state.getX(), fixed)).ordinal();
        }
        return layer;
    }
}
