package ru.ifmo.ctddev.games.state;

import java.util.Random;


/**8
 * Created by Aksenov239 on 30.08.2014.
 */
public class MapState {
    public enum Field {
        GRASS, DESERT, MOUNTAIN, SHOP
    }

    private int N = 30;
    private int M = 30;
    private Field[][] map;
    private int defaultX = N / 2;
    private int defaultY = M / 2;
    private int shopX;
    private int shopY;
    private Random rnd = new Random(System.currentTimeMillis());

    public MapState() {
        map = new Field[N][M];

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                map[i][j] = rnd.nextBoolean() ? Field.GRASS : Field.DESERT;
            }
        }
        int x = rnd.nextInt(N);
        int y = rnd.nextInt(M);
        while (x == defaultX && y == defaultY) {
            x = rnd.nextInt(N);
            y = rnd.nextInt(M);
        }
        shopX = x;
        shopY = y;
        map[x][y] = Field.SHOP;
    }

    public int getShopX() {
        return shopX;
    }

    public int getShopY() {
        return shopY;
    }

    public int getDefaultX() {
        return defaultX;
    }

    public int getDefaultY() {
        return defaultY;
    }

    public Field getValue(int x, int y) {
        return 0 <= x && x < map.length && 0 <= y && y < map[0].length ? map[x][y] : Field.MOUNTAIN;
    }

    /*public int digEnergy(int x, int y) {
        if (map[x][y] == Field.DESERT)
            return 7;
        if (map[x][y] == Field.GRASS)
            return 10;
        return 0;
    }*/

    public boolean canMove(int x, int y, int dx, int dy) {
        return getValue(x + dx, y + dy) != Field.MOUNTAIN;
    }

    public int[][] getVision(Player state) {
        int x = state.getX(), y = state.getY();
        int vision = state.getVision();
        int[][] part = new int[2 * vision + 1][2 * vision + 1];
        for (int i = x - vision; i <= x + vision; i++)
            for (int j = y - vision; j <= y + vision; j++)
                part[i - (x - vision)][j - (y - vision)] = getValue(i, j).ordinal();
        return part;
    }

    public int[] getNextLayer(Player state, int dx, int dy) {
        int vision = state.getVision();
        int[] layer = new int[2 * vision + 1];
        int fixed = (dx != 0 ? state.getX() : state.getY()) + (dx + dy) * (vision + 1);
        for (int i = 0; i < layer.length; i++)
            layer[i] = (dx != 0 ? getValue(fixed, i - vision + state.getY()) : getValue(i - vision + state.getX(), fixed)).ordinal();
        return layer;
    }

    public int[][] getField() {
        int[][] retField = new int[N][M];
        for (int i = 0; i < N; ++i)
            for (int j = 0; j < M; ++j)
                retField[i][j] = map[i][j].ordinal();
        return retField;
    }
}
