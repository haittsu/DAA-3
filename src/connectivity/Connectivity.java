package connectivity;

import graph.Graph;
import graph.Graph.Edge;
import graph.Metrics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class Connectivity {
    public record TopologicalResult(boolean dag, List<Integer> order, Metrics metrics) { }
    public record SccResult(List<List<Integer>> components, Metrics metrics) { }

    private static void requireDirected(Graph graph) {
        if (!graph.directed()) throw new IllegalArgumentException("A directed graph is required");
    }

    /** Kahn's algorithm. A partial ordering is discarded if a cycle exists. */
    public static TopologicalResult topologicalSort(Graph graph) {
        requireDirected(graph);
        Metrics metrics = new Metrics();
        int[] indegree = new int[graph.vertices()];
        for (Edge edge : graph.edges()) {
            metrics.edgeExaminations++;
            indegree[edge.to()]++;
        }
        Queue<Integer> queue = new ArrayDeque<>();
        for (int v = 0; v < graph.vertices(); v++) {
            if (indegree[v] == 0) {
                queue.add(v);
                metrics.queueAdds++;
            }
        }
        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int v = queue.remove();
            metrics.queueRemoves++;
            metrics.verticesProcessed++;
            order.add(v);
            for (Edge edge : graph.neighbors(v)) {
                metrics.edgeExaminations++;
                if (--indegree[edge.to()] == 0) {
                    queue.add(edge.to());
                    metrics.queueAdds++;
                }
            }
        }
        boolean dag = order.size() == graph.vertices();
        return new TopologicalResult(dag, dag ? List.copyOf(order) : List.of(), metrics);
    }

    /** Kosaraju: finishing order, graph transpose, then a second DFS. */
    public static SccResult stronglyConnectedComponents(Graph graph) {
        requireDirected(graph);
        Metrics metrics = new Metrics();
        boolean[] visited = new boolean[graph.vertices()];
        List<Integer> finish = new ArrayList<>();
        for (int v = 0; v < graph.vertices(); v++) {
            if (!visited[v]) finishDfs(graph, v, visited, finish, metrics);
        }
        Graph transpose = new Graph(graph.vertices(), true);
        for (Edge edge : graph.edges()) {
            metrics.edgeExaminations++;
            transpose.addEdge(edge.to(), edge.from(), edge.weight());
        }
        Arrays.fill(visited, false);
        List<List<Integer>> components = new ArrayList<>();
        for (int i = finish.size() - 1; i >= 0; i--) {
            int v = finish.get(i);
            if (!visited[v]) {
                List<Integer> component = new ArrayList<>();
                collectDfs(transpose, v, visited, component, metrics);
                components.add(List.copyOf(component));
            }
        }
        return new SccResult(List.copyOf(components), metrics);
    }

    private static void finishDfs(Graph graph, int v, boolean[] visited,
                                  List<Integer> finish, Metrics metrics) {
        visited[v] = true;
        metrics.verticesProcessed++;
        for (Edge edge : graph.neighbors(v)) {
            metrics.edgeExaminations++;
            if (!visited[edge.to()]) finishDfs(graph, edge.to(), visited, finish, metrics);
        }
        finish.add(v);
    }

    private static void collectDfs(Graph graph, int v, boolean[] visited,
                                   List<Integer> component, Metrics metrics) {
        visited[v] = true;
        metrics.verticesProcessed++;
        component.add(v);
        for (Edge edge : graph.neighbors(v)) {
            metrics.edgeExaminations++;
            if (!visited[edge.to()]) collectDfs(graph, edge.to(), visited, component, metrics);
        }
    }
}
