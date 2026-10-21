package mst;

import dsu.DisjointSetUnion;
import graph.Graph;
import graph.Graph.Edge;
import graph.Metrics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class MinimumSpanningTree {
    public record Result(List<Edge> edges, long weight, int components, Metrics metrics) {
        public boolean connected() { return components == 1; }
    }

    private static Comparator<Edge> comparator(Metrics metrics) {
        return (a, b) -> {
            metrics.comparisons++;
            int order = Integer.compare(a.weight(), b.weight());
            if (order == 0) order = Integer.compare(a.from(), b.from());
            if (order == 0) order = Integer.compare(a.to(), b.to());
            return order;
        };
    }

    private static void requireUndirected(Graph graph) {
        if (graph.directed()) throw new IllegalArgumentException("MST requires an undirected graph");
    }

    public static Result kruskal(Graph graph) {
        requireUndirected(graph);
        Metrics metrics = new Metrics();
        DisjointSetUnion dsu = new DisjointSetUnion(graph.vertices(), metrics);
        List<Edge> sorted = new ArrayList<>(graph.edges());
        sorted.sort(comparator(metrics));
        List<Edge> chosen = new ArrayList<>();
        long weight = 0;
        for (Edge edge : sorted) {
            if (chosen.size() == graph.vertices() - 1) break;
            metrics.edgeExaminations++;
            if (dsu.union(edge.from(), edge.to())) {
                chosen.add(edge);
                weight += edge.weight();
            }
        }
        return new Result(List.copyOf(chosen), weight, dsu.components(), metrics);
    }

    public static Result prim(Graph graph) {
        requireUndirected(graph);
        Metrics metrics = new Metrics();
        boolean[] visited = new boolean[graph.vertices()];
        PriorityQueue<Edge> queue = new PriorityQueue<>(comparator(metrics));
        List<Edge> chosen = new ArrayList<>();
        long weight = 0;
        int components = 0;
        // Restart at each component so a disconnected graph returns a forest.
        for (int start = 0; start < graph.vertices(); start++) {
            if (visited[start]) continue;
            components++;
            visit(graph, start, visited, queue, metrics);
            while (!queue.isEmpty()) {
                Edge edge = queue.remove();
                metrics.queueRemoves++;
                if (visited[edge.to()]) continue;
                chosen.add(edge);
                weight += edge.weight();
                visit(graph, edge.to(), visited, queue, metrics);
            }
        }
        return new Result(List.copyOf(chosen), weight, components, metrics);
    }

    private static void visit(Graph graph, int vertex, boolean[] visited,
                              PriorityQueue<Edge> queue, Metrics metrics) {
        visited[vertex] = true;
        metrics.verticesProcessed++;
        for (Edge edge : graph.neighbors(vertex)) {
            metrics.edgeExaminations++;
            if (!visited[edge.to()]) {
                queue.add(edge);
                metrics.queueAdds++;
            }
        }
    }
}
