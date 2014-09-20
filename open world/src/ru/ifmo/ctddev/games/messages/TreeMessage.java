package ru.ifmo.ctddev.games.messages;

import ru.ifmo.ctddev.games.state.Node;
import sun.reflect.generics.tree.Tree;

/**
 * Created by pva701 on 9/20/14.
 */
public class TreeMessage {
    private int[] id;
    private int[] parent;
    private String[] name;
    private int[] progress;
    private int[] x;
    private int[] y;
    public TreeMessage() {}
    public TreeMessage(Node[] nodes) {
        id = new int[nodes.length];
        parent = new int[nodes.length];
        name = new String[nodes.length];
        progress = new int[nodes.length];
        x = new int[nodes.length];
        y = new int[nodes.length];
        for (int i = 0; i < nodes.length; ++i) {
            id[i] = nodes[i].getId();
            parent[i] = nodes[i].getParent();
            name[i] = nodes[i].getName();
            progress[i] = nodes[i].getProgress();
            x[i] = nodes[i].getX();
            y[i] = nodes[i].getY();
        }
    }

    public int[] getId() {
        return id;
    }

    public int[] getParent() {
        return parent;
    }

    public String[] getName() {
        return name;
    }

    public int[] getProgress() {
        return progress;
    }

    public int[] getX() {
        return x;
    }

    public int[] getY() {
        return y;
    }

    public void setId(int[] id) {
        this.id = id;
    }

    public void setParent(int[] parent) {
        this.parent = parent;
    }

    public void setName(String[] name) {
        this.name = name;
    }

    public void setProgress(int[] progress) {
        this.progress = progress;
    }

    public void setX(int[] x) {
        this.x = x;
    }

    public void setY(int[] y) {
        this.y = y;
    }
}
