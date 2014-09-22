package ru.ifmo.ctddev.games.state;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ru.ifmo.ctddev.games.Logger;
import ru.ifmo.ctddev.games.state.Node;

/**
 * Created by pva701 on 9/20/14.
 */
public class Tree {
    private ArrayList <ArrayList <Integer> > g;
    private ArrayList <String> name;
    private ArrayList <Double> progress;
    private Map<Integer, Node> nodes = new HashMap<Integer, Node>();
    private int root;
    private int[] level;
    private final int LENGTH_OF_EDGE = 80;
    private Map<Integer, Integer> vertexToId = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> idToVertex = new HashMap<Integer, Integer>();

    public static boolean addVertexesInDatabase(Connection connectionToDB, String parentOption, String[] sonOptions) {
        try {
            PreparedStatement st = connectionToDB.prepareStatement("SELECT * FROM Tree WHERE name = ?;");
            st.setString(1, parentOption);
            ResultSet res = st.executeQuery();
            if (SQLExtension.size(res) == 0)
                return false;
            res.next();
            int parentId = res.getInt("nodeId");
            st.close();
            String query = "INSERT INTO Tree (parentId, name, progress) VALUES ";
            boolean first = true;
            for (int i = 0; i < sonOptions.length; ++i) {
                if (first)
                    query += "(" + parentId + ", '" + sonOptions[i] + "', 0) ";
                else
                    query += ", (" + parentId + ", '" + sonOptions[i] + "', 0) ";
                first = false;
            }
            query += ";";
            st = connectionToDB.prepareStatement(query);
            st.executeUpdate();
            st.close();
        } catch (SQLException e) {
            Logger.log("Exception in addVertexesInDatabase!");
            e.printStackTrace();
            System.exit(0);
        }
        return true;
    }

    public static Tree extractTree(Connection connectionToDB) {
        try {
            PreparedStatement preStatementDB;
            ResultSet resultSetDB;
            preStatementDB = connectionToDB.prepareStatement("SELECT * FROM Tree;");
            resultSetDB = preStatementDB.executeQuery();
            Tree tree = new Tree(SQLExtension.size(resultSetDB));
            while (resultSetDB.next()) {
                int nodeId = resultSetDB.getInt("nodeId");
                int parent = resultSetDB.getInt("parentId");
                String name = resultSetDB.getString("name");
                double progress = resultSetDB.getDouble("progress");
                System.err.println("par = " + parent + " " + nodeId);
                tree.addEdge(parent, nodeId, name, progress);
            }
            preStatementDB.close();
            return tree;
        } catch (SQLException e) {
            Logger.log("extractTree load");
            e.printStackTrace();
            System.exit(0);
        }
        return null;
    }

    public Tree(int n) {
        g = new ArrayList<ArrayList<Integer>>();
        name = new ArrayList<String>();
        progress = new ArrayList<Double>();
        for (int i = 0; i < n; ++i) {
            g.add(new ArrayList<Integer>());
            name.add("");
            progress.add(0.0);
        }
        level = new int[n];
    }

    private boolean levelCached = false;

    public int getLevelByName(String n) {
        if (!levelCached) {
            level[root] = 1;
            calcLevels(root);
        }
        for (int i = 0; i < name.size(); ++i)
            if (name.get(i).equals(n))
                return level[i];
        return -1;
    }

    private int getIdVertex(int v) {
        if (vertexToId.containsKey(v))
            return vertexToId.get(v);
        int size = vertexToId.size();
        vertexToId.put(v, size);
        idToVertex.put(size, v);
        return size;
    }

    public void addEdge(int p, int to, String name, double progress) {
        int v2 = getIdVertex(to);
        if (p == 0) {
            this.name.set(v2, name);
            this.progress.set(v2, progress);
            root = v2;
            return;
        }

        int v1 = getIdVertex(p);
        g.get(v1).add(v2);
        this.name.set(v2, name);
        this.progress.set(v2, progress);
    }

    private double[] getPartition(int v, double angle) {
        int sizeV = g.get(v).size();
        double sumSons = 0;
        for (int i = 0; i < sizeV; ++i) {
            int to = g.get(v).get(i);
            if (g.get(to).size() == 0)
                sumSons += (1.0 + 1.0 / level[v]);
            else
                sumSons += g.get(to).size();
        }

        double[] ret = new double[sizeV];
        for (int i = 0; i < sizeV; ++i) {
            int to = g.get(v).get(i);
            if (g.get(to).size() == 0)
                ret[i] = angle * (1.0 + 1.0 / level[v]) / sumSons;
            else
                ret[i] = angle * g.get(to).size() / sumSons;
        }
        return ret;
    }

    public Node[] getNodeOnPlane() {
        level[root] = 1;
        calcLevels(root);
        int sizeRoot = g.get(root).size();
        nodes.put(idToVertex.get(root), new Node(idToVertex.get(root), 0, name.get(root), progress.get(root), 0, 0));
        double startAngle =  -180.0 / sizeRoot;
        //double angle = 360.0 / sizeRoot;
        //System.err.println("ss " + startAngle + " " + angle);
        double[] part = getPartition(root, 360.0);
        for (int i = 0; i < sizeRoot; ++i) {
            int start = g.get(root).get(i);
            double angle = part[i];
            dfs(start, root, startAngle, angle, LENGTH_OF_EDGE);
            startAngle += angle;
        }
        Node[] ret = new Node[nodes.size()];
        int i = 0;
        for (Map.Entry <Integer, Node> e : nodes.entrySet())
            ret[i++] = e.getValue();
        return ret;
    }

    private void calcLevels(int v) {
        for (int i = 0; i < g.get(v).size(); ++i) {
            int to = g.get(v).get(i);
            level[to] = level[v] + 1;
            calcLevels(to);
        }
    }

    private void dfs(int v, int p, double startAngle, double angle, int curRadius) {
        double res = startAngle + angle * 0.5;
        int x = (int)(curRadius * Math.cos(res / 180 * Math.PI));
        int y = (int)(curRadius * Math.sin(res / 180 * Math.PI));
        nodes.put(idToVertex.get(v), new Node(idToVertex.get(v), idToVertex.get(p), name.get(v), progress.get(v), x, y));
        //double nextAngle = angle / g.get(v).size();

        double[] part = getPartition(v, angle);
        for (int i = 0; i < g.get(v).size(); ++i) {
            int to = g.get(v).get(i);
            double lenNextAngle = part[i];
            dfs(to, v, startAngle, lenNextAngle, curRadius + LENGTH_OF_EDGE);
            startAngle += lenNextAngle;
        }
    }
}