package graph;

public class Metrics {
    public long edgeExaminations;
    public long comparisons;
    public long findCalls;
    public long unionCalls;
    public long parentTraversals;
    public long queueAdds;
    public long queueRemoves;
    public long relaxations;
    public long verticesProcessed;

    public String csv() {
        return edgeExaminations + "," + comparisons + "," + findCalls + ","
                + unionCalls + "," + parentTraversals + "," + queueAdds + ","
                + queueRemoves + "," + relaxations + "," + verticesProcessed;
    }

    public static String header() {
        return "edge_examinations,comparisons,find_calls,union_calls,parent_traversals,"
                + "queue_adds,queue_removes,relaxations,vertices_processed";
    }
}
