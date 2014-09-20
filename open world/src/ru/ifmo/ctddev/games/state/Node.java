package ru.ifmo.ctddev.games.state;

/**
 * Created by pva701 on 9/20/14.
 */
public class Node {
    private int id;
    private int parent;
    private String name;
    private int progress;
    private int x;
    private int y;

    public Node() {}

    public Node(int id, int parent, String name, int progress, int x, int y) {
        this.id = id;
        this.parent = parent;
        this.name = name;
        this.progress = progress;
        this.x = x;
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public int getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public int getProgress() {
        return progress;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}
