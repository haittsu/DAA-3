package shortestpath;

import graph.Graph;
import graph.Graph.Edge;
import graph.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class Dijkstra {
    public static final long UNREACHABLE = Long.MAX_VALUE;
    private record Entry(int vertex, long distance) { }

    public record Result(int source, long[] distances, int[] previous, Metrics metrics) {
        public long distanceTo(int vertex) {
            check(vertex);
            return distances[vertex];
        }

        public List<Integer> pathTo(int vertex) {
            check(vertex);
            if (distances[vertex] == UNREACHABLE) return List.of();
            List<Integer> path = new ArrayList<>();
            for (int v = vertex; v != -1; v = previous[v]) path.add(v);
            Collections.reverse(path);
            return List.copyOf(path);
        }

        private void check(int vertex) {
            if (vertex < 0 || vertex >= distances.length) {
                throw new IllegalArgumentException("Invalid vertex: " + vertex);
            }
        }
    }

    public static Result shortestPaths(Graph graph, int source) {
        graph.checkVertex(source);
        if (graph.hasNegativeWeight()) {
            throw new IllegalArgumentException("Dijkstra requires non-negative weights");
        }
        Metrics metrics = new Metrics();
        long[] distances = new long[graph.vertices()];
        int[] previous = new int[graph.vertices()];
        Arrays.fill(distances, UNREACHABLE);
        Arrays.fill(previous, -1);
        PriorityQueue<Entry> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            int order = Long.compare(a.distance(), b.distance());
            return order != 0 ? order : Integer.compare(a.vertex(), b.vertex());
        });
        distances[source] = 0;
        queue.add(new Entry(source, 0));
        metrics.queueAdds++;
        while (!queue.isEmpty()) {
            Entry current = queue.remove();
            metrics.queueRemoves++;
            int v = current.vertex();
            // A shorter entry may already have replaced this one.
            if (current.distance() != distances[v]) continue;
            metrics.verticesProcessed++;
            for (Edge edge : graph.neighbors(v)) {
                metrics.edgeExaminations++;
                long candidate = distances[v] + edge.weight();
                if (candidate < distances[edge.to()]) {
                    distances[edge.to()] = candidate;
                    previous[edge.to()] = v;
                    metrics.relaxations++;
                    queue.add(new Entry(edge.to(), candidate));
                    metrics.queueAdds++;
                }
            }
        }
        return new Result(source, distances, previous, metrics);
    }
}
