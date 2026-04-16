package models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DijkstraResult {
    private final String sourceNode;
    private final Map<String, RouteResult> routes;
    private final List<DijkstraStep> steps;

    public DijkstraResult(String sourceNode, Map<String, RouteResult> routes, List<DijkstraStep> steps) {
        this.sourceNode = sourceNode;
        this.routes = Collections.unmodifiableMap(new LinkedHashMap<String, RouteResult>(routes));
        this.steps = List.copyOf(steps);
    }

    public String getSourceNode() {
        return sourceNode;
    }

    public Map<String, RouteResult> getRoutes() {
        return routes;
    }

    public List<DijkstraStep> getSteps() {
        return steps;
    }
}
