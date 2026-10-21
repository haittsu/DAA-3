package dsu;

import graph.Metrics;

/** Disjoint sets with path compression and union by rank. */
public class DisjointSetUnion {
    private final int[] parent;
    private final int[] rank;
    private final Metrics metrics;
    private int components;

    public DisjointSetUnion(int size, Metrics metrics) {
        if (size < 1) throw new IllegalArgumentException("Invalid DSU size");
        parent = new int[size];
        rank = new int[size];
        components = size;
        this.metrics = metrics;
        for (int i = 0; i < size; i++) parent[i] = i;
    }

    public int find(int vertex) {
        metrics.findCalls++;
        if (vertex < 0 || vertex >= parent.length) {
            throw new IllegalArgumentException("Invalid vertex: " + vertex);
        }
        int root = vertex;
        while (parent[root] != root) {
            metrics.parentTraversals++;
            root = parent[root];
        }
        // Make every vertex on this path point directly to its root.
        while (parent[vertex] != vertex) {
            metrics.parentTraversals++;
            int next = parent[vertex];
            parent[vertex] = root;
            vertex = next;
        }
        return root;
    }

    public boolean union(int first, int second) {
        metrics.unionCalls++;
        int a = find(first);
        int b = find(second);
        if (a == b) return false;
        if (rank[a] < rank[b]) {
            parent[a] = b;
        } else {
            parent[b] = a;
            if (rank[a] == rank[b]) rank[a]++;
        }
        components--;
        return true;
    }

    public int components() { return components; }
}
