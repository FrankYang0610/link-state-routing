package models;

public class GraphLink {
    private final String toNode;
    private final int cost;

    public GraphLink(String toNode, int cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("Negative cost.");
        }

        this.toNode = toNode;
        this.cost = cost;
    }

    public String getToNode() {
        return toNode;
    }

    public int getCost() {
        return cost;
    }
}
