import models.Graph;
import models.GraphLink;

public final class LSRTopologyService {
    private LSRTopologyService() {
    }

    public static Graph addNode(Graph graph, String node) {
        Graph nextGraph = copyGraph(graph);
        nextGraph.addNode(node);
        LSRGraphValidator.validate(nextGraph);
        return nextGraph;
    }

    public static Graph removeNode(Graph graph, String node) {
        Graph nextGraph = copyGraph(graph);
        nextGraph.removeNode(node);
        LSRGraphValidator.validate(nextGraph);
        return nextGraph;
    }

    public static Graph addOrUpdateLink(Graph graph, String fromNode, String toNode, int cost) {
        Graph nextGraph = copyGraph(graph);
        nextGraph.addEdge(fromNode, toNode, cost);
        LSRGraphValidator.validate(nextGraph);
        return nextGraph;
    }

    public static Graph breakLink(Graph graph, String fromNode, String toNode) {
        Graph nextGraph = copyGraph(graph);
        nextGraph.removeEdge(fromNode, toNode);
        LSRGraphValidator.validate(nextGraph);
        return nextGraph;
    }

    public static Graph copyGraph(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph is null.");
        }

        Graph copy = new Graph();
        for (String node : graph.getNodes()) {
            copy.addNode(node);
        }
        for (String fromNode : graph.getNodes()) {
            for (GraphLink link : graph.getNeighbors(fromNode)) {
                copy.addDirectedEdge(fromNode, link.getToNode(), link.getCost());
            }
        }
        return copy;
    }
}
