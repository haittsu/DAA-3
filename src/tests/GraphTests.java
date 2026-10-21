package tests;

import connectivity.Connectivity;
import dsu.DisjointSetUnion;
import graph.Graph;
import graph.Graph.Edge;
import graph.GraphGenerator;
import graph.GraphGenerator.Type;
import graph.Metrics;
import mst.MinimumSpanningTree;
import shortestpath.Dijkstra;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Standalone tests: no external test library or enabled assertions needed. */
public class GraphTests {
    private static int checks;
    private static final List<String> report = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        checks = 0;
        report.clear();
        run("DSU union, rank and path compression", GraphTests::testDsu);
        run("Small graph with known MST weight", GraphTests::testSmallMst);
        run("Disconnected graph and isolated vertex", GraphTests::testDisconnected);
        run("Multiple MSTs and duplicate weights", GraphTests::testEqualWeights);
        run("Single vertex for all algorithms", GraphTests::testSingleVertex);
        run("Shortest distances, paths, zero weights and unreachable vertices", GraphTests::testPaths);
        run("Long distance sums and invalid inputs", GraphTests::testValidation);
        run("Directed cycles, self-loop and partial topological order", GraphTests::testCycles);
        run("Known SCC partition", GraphTests::testScc);
        run("1024 relabeled DAGs", GraphTests::testDags);
        run("Random paths and SCCs against Floyd-Warshall", GraphTests::testRandomDirected);
        run("MSTs against exhaustive spanning-tree search", GraphTests::testMstOracle);
        run("Required generator sizes and fixed-seed reproducibility", GraphTests::testGenerator);
        String summary = report.size() + " test groups passed; " + checks + " correctness checks passed.";
        report.add(summary);
        System.out.println(summary);
        Files.createDirectories(Path.of("results/tables"));
        Files.writeString(Path.of("results/tables/tests.txt"), String.join("\n", report) + "\n");
    }

    private static void run(String name, Runnable test) {
        test.run();
        report.add("PASS: " + name);
        System.out.println("PASS: " + name);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Graph graph(int vertices, boolean directed, int[][] edges) {
        Graph graph = new Graph(vertices, directed);
        for (int[] e : edges) graph.addEdge(e[0], e[1], e[2]);
        return graph;
    }

    private static void checkMst(Graph graph, long weight, int components) {
        var kruskal = MinimumSpanningTree.kruskal(graph);
        var prim = MinimumSpanningTree.prim(graph);
        for (var result : List.of(kruskal, prim)) {
            check(result.weight() == weight, "Incorrect MST/forest weight");
            check(result.components() == components, "Incorrect component count");
            check(result.connected() == (components == 1), "Incorrect connectivity");
            check(result.edges().size() == graph.vertices() - components, "Incorrect forest edge count");
            Graph forest = new Graph(graph.vertices(), false);
            for (Edge edge : result.edges()) forest.addEdge(edge.from(), edge.to(), edge.weight());
            long[][] original = floyd(graph);
            long[][] selected = floyd(forest);
            for (int a = 0; a < graph.vertices(); a++) {
                for (int b = 0; b < graph.vertices(); b++) {
                    check((original[a][b] != Dijkstra.UNREACHABLE)
                            == (selected[a][b] != Dijkstra.UNREACHABLE), "Forest lost connectivity");
                }
            }
        }
    }

    private static void testDsu() {
        Metrics metrics = new Metrics();
        DisjointSetUnion dsu = new DisjointSetUnion(4, metrics);
        check(dsu.components() == 4, "Initial DSU components");
        check(dsu.union(0, 1) && dsu.union(2, 3) && dsu.union(0, 2), "Successful unions");
        long before = metrics.parentTraversals;
        check(dsu.find(3) == dsu.find(0), "DSU root");
        long first = metrics.parentTraversals - before;
        before = metrics.parentTraversals;
        check(dsu.find(3) == dsu.find(0), "Compressed DSU root");
        check(metrics.parentTraversals - before < first, "Compression should shorten the path");
        check(!dsu.union(1, 3) && dsu.components() == 1, "Repeated union");
        check(metrics.unionCalls == 4, "DSU union counter");
    }

    private static void testSmallMst() {
        checkMst(graph(4, false, new int[][]{
                {0,1,10}, {0,2,6}, {0,3,5}, {1,3,15}, {2,3,4}}), 19, 1);
    }

    private static void testDisconnected() {
        checkMst(graph(5, false, new int[][]{{0,1,3}, {2,3,2}}), 5, 3);
        checkMst(new Graph(4, false), 0, 4);
    }

    private static void testEqualWeights() {
        checkMst(graph(4, false, new int[][]{
                {0,1,1}, {1,2,1}, {2,3,1}, {3,0,1}, {0,2,2}, {0,1,1}}), 3, 1);
        // MST algorithms also support negative construction costs.
        var negative = graph(3, false, new int[][]{{0,1,-2}, {1,2,1}, {0,2,5}});
        check(MinimumSpanningTree.kruskal(negative).weight() == -1, "Negative MST weight");
        check(MinimumSpanningTree.prim(negative).weight() == -1, "Negative Prim weight");
    }

    private static void testSingleVertex() {
        checkMst(new Graph(1, false), 0, 1);
        Graph single = new Graph(1, true);
        checkTopo(single);
        checkScc(single);
        var result = Dijkstra.shortestPaths(single, 0);
        check(result.distanceTo(0) == 0 && result.pathTo(0).equals(List.of(0)), "Single source path");
    }

    private static void testPaths() {
        Graph graph = graph(6, true, new int[][]{
                {0,1,10}, {0,2,1}, {2,1,2}, {1,3,1}, {2,3,7}, {3,4,0}, {2,4,8}});
        var result = Dijkstra.shortestPaths(graph, 0);
        check(Arrays.equals(result.distances(), new long[]{0,3,1,4,4,Dijkstra.UNREACHABLE}), "Known distances");
        check(result.pathTo(4).equals(List.of(0,2,1,3,4)), "Known shortest path");
        check(result.pathTo(5).isEmpty(), "Unreachable path must be empty");
        check(Dijkstra.shortestPaths(graph, 2).distanceTo(0) == Dijkstra.UNREACHABLE, "Selected source");
        checkPaths(graph);
    }

    private static void expectInvalid(Runnable action) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Invalid input should be rejected");
    }

    private static void testValidation() {
        Graph largeWeights = graph(3, true, new int[][]{{0,1,2000000000}, {1,2,2000000000}});
        check(Dijkstra.shortestPaths(largeWeights, 0).distanceTo(2) == 4000000000L, "Use long distances");
        expectInvalid(() -> Dijkstra.shortestPaths(graph(2, true, new int[][]{{0,1,-1}}), 0));
        expectInvalid(() -> Dijkstra.shortestPaths(largeWeights, 3));
        expectInvalid(() -> MinimumSpanningTree.kruskal(largeWeights));
        expectInvalid(() -> MinimumSpanningTree.prim(largeWeights));
        expectInvalid(() -> Connectivity.topologicalSort(new Graph(2, false)));
        expectInvalid(() -> Connectivity.stronglyConnectedComponents(new Graph(2, false)));
        expectInvalid(() -> new Graph(0, true));
        expectInvalid(() -> largeWeights.addEdge(0, 3, 1));
    }

    private static void testCycles() {
        Graph cycle = graph(5, true, new int[][]{{0,1,1}, {1,2,1}, {2,1,1}, {3,4,1}});
        var result = Connectivity.topologicalSort(cycle);
        check(!result.dag() && result.order().isEmpty(), "Reject a partial ordering");
        Graph selfLoop = graph(1, true, new int[][]{{0,0,0}});
        check(!Connectivity.topologicalSort(selfLoop).dag(), "Detect self-loop");
        checkScc(cycle);
        checkScc(selfLoop);
    }

    private static void testScc() {
        Graph graph = graph(8, true, new int[][]{
                {0,1,1}, {1,2,1}, {2,0,1}, {2,3,1}, {3,4,1}, {4,3,1}, {4,5,1}, {6,6,1}});
        check(Connectivity.stronglyConnectedComponents(graph).components().size() == 5, "Known SCC count");
        checkScc(graph);
    }

    private static void checkTopo(Graph graph) {
        var result = Connectivity.topologicalSort(graph);
        check(result.dag() && result.order().size() == graph.vertices(), "DAG must have a full order");
        int[] position = new int[graph.vertices()];
        Arrays.fill(position, -1);
        for (int i = 0; i < result.order().size(); i++) {
            int vertex = result.order().get(i);
            check(position[vertex] == -1, "Duplicate vertex in ordering");
            position[vertex] = i;
        }
        for (Edge edge : graph.edges()) check(position[edge.from()] < position[edge.to()], "Invalid topological edge");
    }

    private static void testDags() {
        int[] labels = {2,4,1,3,0};
        for (int mask = 0; mask < 1024; mask++) {
            Graph dag = new Graph(5, true);
            int bit = 0;
            for (int a = 0; a < 5; a++) {
                for (int b = a + 1; b < 5; b++) {
                    if ((mask & (1 << bit)) != 0) dag.addEdge(labels[a], labels[b], 1);
                    bit++;
                }
            }
            checkTopo(dag);
        }
    }

    /** Independent all-pairs reference, used only by tests. */
    private static long[][] floyd(Graph graph) {
        int n = graph.vertices();
        long[][] distances = new long[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(distances[i], Dijkstra.UNREACHABLE);
            distances[i][i] = 0;
            for (Edge edge : graph.neighbors(i)) {
                distances[i][edge.to()] = Math.min(distances[i][edge.to()], edge.weight());
            }
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (distances[i][k] != Dijkstra.UNREACHABLE && distances[k][j] != Dijkstra.UNREACHABLE) {
                        distances[i][j] = Math.min(distances[i][j], distances[i][k] + distances[k][j]);
                    }
                }
            }
        }
        return distances;
    }

    private static void checkPaths(Graph graph) {
        long[][] expected = floyd(graph);
        for (int source = 0; source < graph.vertices(); source++) {
            var result = Dijkstra.shortestPaths(graph, source);
            for (int target = 0; target < graph.vertices(); target++) {
                check(result.distanceTo(target) == expected[source][target], "Dijkstra differs from reference");
                List<Integer> path = result.pathTo(target);
                if (expected[source][target] == Dijkstra.UNREACHABLE) {
                    check(path.isEmpty(), "Unreachable vertex has a path");
                    continue;
                }
                check(!path.isEmpty() && path.get(0) == source && path.get(path.size()-1) == target, "Path endpoints");
                long weight = 0;
                for (int i = 1; i < path.size(); i++) {
                    long best = Dijkstra.UNREACHABLE;
                    for (Edge edge : graph.neighbors(path.get(i-1))) {
                        if (edge.to() == path.get(i)) best = Math.min(best, edge.weight());
                    }
                    check(best != Dijkstra.UNREACHABLE, "Path uses a missing edge");
                    weight += best;
                }
                check(weight == result.distanceTo(target), "Path weight differs from distance");
            }
        }
    }

    private static void checkScc(Graph graph) {
        long[][] reach = floyd(graph);
        int[] owner = new int[graph.vertices()];
        Arrays.fill(owner, -1);
        var components = Connectivity.stronglyConnectedComponents(graph).components();
        for (int i = 0; i < components.size(); i++) {
            check(!components.get(i).isEmpty(), "Empty SCC");
            for (int v : components.get(i)) {
                check(owner[v] == -1, "Vertex belongs to two SCCs");
                owner[v] = i;
            }
        }
        for (int a = 0; a < graph.vertices(); a++) {
            check(owner[a] != -1, "Vertex missing from SCCs");
            for (int b = 0; b < graph.vertices(); b++) {
                boolean mutual = reach[a][b] != Dijkstra.UNREACHABLE && reach[b][a] != Dijkstra.UNREACHABLE;
                check((owner[a] == owner[b]) == mutual, "SCC differs from mutual reachability");
            }
        }
    }

    private static void testRandomDirected() {
        for (int seed = 0; seed < 40; seed++) {
            Random random = new Random(seed);
            Graph graph = new Graph(8, true);
            for (int a = 0; a < 8; a++) {
                for (int b = 0; b < 8; b++) {
                    if (random.nextInt(4) == 0) graph.addEdge(a, b, random.nextInt(10));
                }
            }
            checkPaths(graph);
            checkScc(graph);
        }
    }

    private static void testMstOracle() {
        for (int seed = 0; seed < 20; seed++) {
            Graph graph = GraphGenerator.generate(5, 7, seed, Type.UNDIRECTED);
            long best = Long.MAX_VALUE;
            for (int mask = 0; mask < (1 << graph.edgeCount()); mask++) {
                if (Integer.bitCount(mask) != graph.vertices() - 1) continue;
                Graph candidate = new Graph(5, false);
                long weight = 0;
                for (int i = 0; i < graph.edgeCount(); i++) {
                    if ((mask & (1 << i)) == 0) continue;
                    Edge edge = graph.edges().get(i);
                    candidate.addEdge(edge.from(), edge.to(), edge.weight());
                    weight += edge.weight();
                }
                long[][] reach = floyd(candidate);
                boolean connected = true;
                for (int v = 0; v < 5; v++) connected &= reach[0][v] != Dijkstra.UNREACHABLE;
                if (connected) best = Math.min(best, weight);
            }
            checkMst(graph, best, 1);
        }
    }

    private static void testGenerator() {
        int[][] sizes = {{10,30}, {100,500}, {1000,5000}};
        for (int[] size : sizes) {
            for (Type type : Type.values()) {
                long seed = GraphGenerator.seed(size[0], size[1], type);
                Graph first = GraphGenerator.generate(size[0], size[1], seed, type);
                Graph second = GraphGenerator.generate(size[0], size[1], seed, type);
                check(first.vertices() == size[0] && first.edgeCount() == size[1], "Exact generator size");
                check(first.edges().equals(second.edges()), "Fixed seed must reproduce edges and weights");
                if (type == Type.UNDIRECTED) {
                    var a = MinimumSpanningTree.kruskal(first);
                    var b = MinimumSpanningTree.prim(first);
                    check(a.connected() && b.connected() && a.weight() == b.weight(), "Generated connected MSTs");
                } else if (type == Type.DAG) {
                    checkTopo(first);
                    check(Connectivity.stronglyConnectedComponents(first).components().size() == size[0], "DAG SCCs");
                } else {
                    check(!Connectivity.topologicalSort(first).dag(), "Generated cycle");
                    check(Connectivity.stronglyConnectedComponents(first).components().size() == 1, "Ring SCC");
                }
            }
        }
        expectInvalid(() -> GraphGenerator.generate(3, 4, 1, Type.DAG));
    }
}
