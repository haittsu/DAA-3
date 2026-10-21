package graph;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class GraphGenerator {
    public enum Type { UNDIRECTED, DAG, CYCLIC }
    public static final long BASE_SEED = 20261021L;

    public static long seed(int vertices, int edges, Type type) {
        return BASE_SEED + 31L * vertices + edges + type.ordinal();
    }

    public static Graph generate(int vertices, int edges, long seed, Type type) {
        boolean cyclic = type == Type.CYCLIC;
        long maximum = (long) vertices * (vertices - 1) / (cyclic ? 1 : 2);
        int minimum = cyclic ? vertices : vertices - 1;
        if (vertices < 1 || (cyclic && vertices < 2)
                || edges < minimum || edges > maximum) {
            throw new IllegalArgumentException("Invalid graph size for " + type);
        }
        Graph graph = new Graph(vertices, type != Type.UNDIRECTED);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        if (type == Type.UNDIRECTED) {
            // A random spanning tree guarantees connectivity.
            for (int v = 1; v < vertices; v++) {
                add(graph, used, random.nextInt(v), v, random, type);
            }
        } else {
            // Increasing edges form a DAG; closing the ring guarantees a cycle.
            for (int v = 0; v < vertices - 1; v++) {
                add(graph, used, v, v + 1, random, type);
            }
            if (cyclic) add(graph, used, vertices - 1, 0, random, type);
        }
        while (graph.edgeCount() < edges) {
            int from = random.nextInt(vertices);
            int to = random.nextInt(vertices);
            if (from != to) add(graph, used, from, to, random, type);
        }
        return graph;
    }

    private static void add(Graph graph, Set<Long> used, int from, int to,
                            Random random, Type type) {
        if (type != Type.CYCLIC && from > to) {
            int swap = from;
            from = to;
            to = swap;
        }
        long key = ((long) from << 32) | (to & 0xffffffffL);
        if (used.add(key)) graph.addEdge(from, to, random.nextInt(100) + 1);
    }
}
