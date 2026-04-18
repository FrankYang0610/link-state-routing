package models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DijkstraResult {
    private final String sourceNode;
    private final Map<String, DijkstraRouteResult> routes;
    private final List<DijkstraStep> steps;

    public DijkstraResult(String sourceNode, Map<String, DijkstraRouteResult> routes, List<DijkstraStep> steps) {
        this.sourceNode = sourceNode;
        this.routes = Collections.unmodifiableMap(new LinkedHashMap<String, DijkstraRouteResult>(routes));
        this.steps = List.copyOf(steps);
    }

    public String getSourceNode() {
        return sourceNode;
    }

    public Map<String, DijkstraRouteResult> getRoutes() {
        return routes;
    }

    public List<DijkstraStep> getSteps() {
        return steps;
    }
}
