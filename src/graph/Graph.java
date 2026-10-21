package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Graph {
    public record Edge(int from, int to, int weight) { }

    private final int vertices;
    private final boolean directed;
    private final List<List<Edge>> adjacency = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private boolean negativeWeight;

    public Graph(int vertices, boolean directed) {
        if (vertices < 1) {
            throw new IllegalArgumentException("A graph needs at least one vertex");
        }
        this.vertices = vertices;
        this.directed = directed;
        for (int v = 0; v < vertices; v++) {
            adjacency.add(new ArrayList<>());
        }
    }

    public void addEdge(int from, int to, int weight) {
        checkVertex(from);
        checkVertex(to);
        Edge edge = new Edge(from, to, weight);
        edges.add(edge);
        adjacency.get(from).add(edge);
        if (!directed) {
            adjacency.get(to).add(new Edge(to, from, weight));
        }
        negativeWeight |= weight < 0;
    }

    public void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= vertices) {
            throw new IllegalArgumentException("Invalid vertex: " + vertex);
        }
    }

    public int vertices() { return vertices; }
    public int edgeCount() { return edges.size(); }
    public boolean directed() { return directed; }
    public boolean hasNegativeWeight() { return negativeWeight; }

    /** Each undirected edge occurs once here and twice in adjacency lists. */
    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    public List<Edge> neighbors(int vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }
}
