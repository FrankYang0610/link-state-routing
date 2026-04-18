import models.DijkstraResult;
import models.DijkstraStep;
import models.Graph;
import models.GraphLink;
import models.DijkstraRouteResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class LSRDijkstraCalculator {
    private static final int INFINITY = Integer.MAX_VALUE;

    private static final class RouteState {
        private final String node;
        private final int cost;

        private RouteState(String node, int cost) {
            this.node = node;
            this.cost = cost;
        }

        private String getNode() {
            return node;
        }

        private int getCost() {
            return cost;
        }
    }

    private LSRDijkstraCalculator() { }

    public static DijkstraResult compute(Graph graph, String sourceNode) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph is null.");
        }
        if (!graph.containsNode(sourceNode)) {
            throw new IllegalArgumentException("Unknown source (" + sourceNode + ")");
        }

        Map<String, Integer> distances = new LinkedHashMap<String, Integer>();
        Map<String, String> previousNodes = new HashMap<String, String>();
        Set<String> visitedNodes = new HashSet<String>();
        List<DijkstraStep> steps = new ArrayList<DijkstraStep>();

        for (String node : graph.getNodes()) {
            distances.put(node, INFINITY);
        }
        distances.put(sourceNode, 0);

        PriorityQueue<RouteState> queue = new PriorityQueue<RouteState>(new Comparator<RouteState>() {
            public int compare(RouteState left, RouteState right) {
                int distanceCompare = Integer.compare(left.getCost(), right.getCost());
                if (distanceCompare != 0) {
                    return distanceCompare;
                }
                return left.getNode().compareTo(right.getNode());
            }
        });
        queue.add(new RouteState(sourceNode, 0));

        while (!queue.isEmpty()) {
            RouteState current = queue.poll();

            if (visitedNodes.contains(current.getNode())) {
                continue;
            }

            visitedNodes.add(current.getNode());
            if (!current.getNode().equals(sourceNode)) {
                steps.add(new DijkstraStep(
                        current.getNode(),
                        buildPath(sourceNode, current.getNode(), previousNodes),
                        current.getCost()
                ));
            }

            for (GraphLink link : graph.getNeighbors(current.getNode())) {
                String nextNode = link.getToNode();
                if (visitedNodes.contains(nextNode)) {
                    continue;
                }

                int newCost = current.getCost() + link.getCost();
                if (newCost < distances.get(nextNode)) {
                    distances.put(nextNode, newCost);
                    previousNodes.put(nextNode, current.getNode());
                    queue.add(new RouteState(nextNode, newCost));
                }
            }
        }

        return new DijkstraResult(sourceNode, buildRoutes(graph, sourceNode, distances, previousNodes), steps);
    }

    private static Map<String, DijkstraRouteResult> buildRoutes(
            Graph graph,
            String sourceNode,
            Map<String, Integer> distances,
            Map<String, String> previousNodes
    ) {
        Map<String, DijkstraRouteResult> routes = new LinkedHashMap<String, DijkstraRouteResult>();

        for (String node : graph.getNodes()) {
            if (node.equals(sourceNode)) {
                continue;
            }

            int cost = distances.get(node);
            List<String> path = cost == INFINITY
                    ? Collections.<String>emptyList()
                    : buildPath(sourceNode, node, previousNodes);

            routes.put(node, new DijkstraRouteResult(node, path, cost));
        }

        return routes;
    }

    private static List<String> buildPath(
            String sourceNode,
            String destinationNode,
            Map<String, String> previousNodes)
    {
        LinkedList<String> path = new LinkedList<String>();
        String currentNode = destinationNode;

        while (currentNode != null) {
            path.addFirst(currentNode);
            if (currentNode.equals(sourceNode)) {
                return path;
            }
            currentNode = previousNodes.get(currentNode);
        }

        return Collections.emptyList();
    }
}
