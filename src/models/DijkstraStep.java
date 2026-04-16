package models;

import java.util.List;

public class DijkstraStep {
    private final String foundNode;
    private final List<String> path;
    private final int cost;

    public DijkstraStep(String foundNode, List<String> path, int cost) {
        this.foundNode = foundNode;
        this.path = List.copyOf(path);
        this.cost = cost;
    }

    public String getFoundNode() {
        return foundNode;
    }

    public List<String> getPath() {
        return path;
    }

    public int getCost() {
        return cost;
    }
}
