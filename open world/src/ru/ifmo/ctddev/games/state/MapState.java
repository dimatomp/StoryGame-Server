package ru.ifmo.ctddev.games.state;

import java.util.Collections;
import java.util.Random;


/**8
 * Created by Aksenov239 on 30.08.2014.
 */
public class MapState {
    public enum Field {
        GRASS, DESERT, MOUNTAIN, SHOP, WATER
    }

    private int N = 30;
    private int M = 30;
    private Field[][] map;
    private boolean[][] use;
    private int defaultX = N / 2;
    private int defaultY = M / 2;
    private double PERCENT_OF_GRASS = 0.4;
    private int shopX;
    private int shopY;
    private Random rnd = new Random(239);

    public MapState() {
        map = new Field[N][M];
        use = new boolean[N][M];
        /*for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                map[i][j] = rnd.nextBoolean() ? Field.GRASS : Field.DESERT;
            }
        }*/
        //dfs(0, 0);
        for (int i = 0; i < N; ++i)
            for (int j = 0; j < M; ++j)
                if (!use[i][j])
                    map[i][j] = Field.GRASS;
        int sum = 0;
        while (sum < PERCENT_OF_GRASS * N * M) {
            int num = rnd.nextInt(N + M - 1);
            int i = rnd.nextInt(N);
            int j = rnd.nextInt(M);
            /*if (num < M) {
                i = 0;
                j = num;
            } else {
                i = num - M + 1;
                j = 0;
            }*/

            while (i + 1 < N && j + 1 < M) {
                if (map[i][j] == Field.GRASS) ++sum;
                map[i][j] = Field.DESERT;
                int nextDraw = rnd.nextInt(100);
                if (nextDraw < 5)
                    break;
                boolean go = rnd.nextBoolean();
                if (j == M - 1 || go && i + 1 < N) ++i;
                else ++j;
            }
        }

        System.err.println("build field");
        int x = rnd.nextInt(N);
        int y = rnd.nextInt(M);
        while (x == defaultX && y == defaultY) {
            x = rnd.nextInt(N);
            y = rnd.nextInt(M);
        }
        shopX = x;
        shopY = y;
        for (int i = -1; i <= 1; ++i)
            for (int j = -1; j <= 1; ++j)
                if (canMove(x, y, i, j))
                    map[x + i][y + j] = Field.GRASS;
        map[x][y] = Field.SHOP;
    }

    private final int[] dx = new int[]{-1, 0, 1, 0};
    private final int[] dy = new int[]{0, -1, 0, 1};
    private int cntDesert = 0;
    private void dfs(int x, int y) {
        if (cntDesert >= N * M / 2)
            return;
        use[x][y] = true;
        ++cntDesert;
        map[x][y] = Field.DESERT;
        for (int i = 0; i < 10; ++i) {
            int j = Math.abs(rnd.nextInt()) % 4;
            int nx = x + dx[j];
            int ny = y + dy[j];
            if (nx >= 0 & nx < N && ny >= 0 && ny < M && !use[nx][ny])
                dfs(nx, ny);
        }
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
        if (x < 0)
            return Field.WATER;
        return 0 <= x && x < N && 0 <= y && y < M ? map[x][y] : Field.MOUNTAIN;
    }

    public boolean canMove(int x, int y, int dx, int dy) {
        return getValue(x + dx, y + dy) != Field.MOUNTAIN && getValue(x + dx, y + dy) != Field.WATER;
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
            layer[i] = (dx != 0 ? getValue(fixed, i - vision + state.getY()) :
                    getValue(i - vision + state.getX(), fixed)).ordinal();
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
