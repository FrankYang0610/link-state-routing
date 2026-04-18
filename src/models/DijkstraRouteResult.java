package models;

import java.util.List;

public class DijkstraRouteResult {
    private final String destinationNode;
    private final List<String> path;
    private final int cost;

    public DijkstraRouteResult(String destinationNode, List<String> path, int cost) {
        this.destinationNode = destinationNode;
        this.path = List.copyOf(path);
        this.cost = cost;
    }

    public String getDestinationNode() {
        return destinationNode;
    }

    public List<String> getPath() {
        return path;
    }

    public int getCost() {
        return cost;
    }

    public boolean isReachable() {
        return !path.isEmpty();
    }
}
