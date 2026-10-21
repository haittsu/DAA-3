import connectivity.Connectivity;
import graph.Graph;
import mst.MinimumSpanningTree;
import shortestpath.Dijkstra;

/** Small city example. Optional program argument: source district, 0 to 5. */
public class Main {
    public static void main(String[] args) {
        int source = args.length == 0 ? 0 : Integer.parseInt(args[0]);
        Graph roads = new Graph(6, false);
        int[][] connections = {{0,1,4}, {0,2,3}, {1,2,1}, {1,3,2},
                {2,3,4}, {3,4,2}, {4,5,6}, {3,5,5}};
        for (int[] e : connections) roads.addEdge(e[0], e[1], e[2]);
        roads.checkVertex(source);
        System.out.println("SMART CITY: districts 0 through 5\n");
        var kruskal = MinimumSpanningTree.kruskal(roads);
        var prim = MinimumSpanningTree.prim(roads);
        System.out.println("Infrastructure construction costs (undirected graph)");
        printMst("Kruskal", kruskal);
        printMst("Prim", prim);
        System.out.println("Same minimum cost: " + (kruskal.weight() == prim.weight()));

        Graph oneWay = new Graph(6, true);
        int[][] directed = {{0,1,4}, {1,2,1}, {2,0,2}, {1,3,2}, {3,4,3}, {4,3,1}};
        for (int[] e : directed) oneWay.addEdge(e[0], e[1], e[2]);
        System.out.println("\nOne-way road connectivity");
        var scc = Connectivity.stronglyConnectedComponents(oneWay);
        for (int i = 0; i < scc.components().size(); i++) {
            System.out.println("SCC " + (i+1) + ": " + scc.components().get(i));
        }
        var cyclic = Connectivity.topologicalSort(oneWay);
        System.out.println(cyclic.dag() ? "Order: " + cyclic.order()
                : "Topological ordering is impossible: the directed graph contains a cycle.");

        Graph dependencies = new Graph(6, true);
        int[][] services = {{0,2,1}, {1,2,1}, {2,3,1}, {1,4,1}, {4,5,1}};
        for (int[] e : services) dependencies.addEdge(e[0], e[1], e[2]);
        System.out.println("Service dependencies (DAG): "
                + Connectivity.topologicalSort(dependencies).order());

        System.out.println("\nShortest travel distances from district " + source);
        var paths = Dijkstra.shortestPaths(oneWay, source);
        for (int district = 0; district < oneWay.vertices(); district++) {
            if (paths.distanceTo(district) == Dijkstra.UNREACHABLE) {
                System.out.println(district + ": unreachable; no path");
            } else {
                System.out.println(district + ": distance=" + paths.distanceTo(district)
                        + ", path=" + paths.pathTo(district));
            }
        }
    }

    private static void printMst(String name, MinimumSpanningTree.Result result) {
        System.out.println(name + ": weight=" + result.weight() + ", connected="
                + result.connected() + ", components=" + result.components());
        System.out.println((result.connected() ? "MST edges: " : "Forest edges: ") + result.edges());
    }
}
