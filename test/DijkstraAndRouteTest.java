import models.DijkstraResult;
import models.DijkstraStep;
import models.Graph;
import models.DijkstraRouteResult;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DijkstraAndRouteTest {
    @Test
    void computesShortestRoutesFromSource() {
        Graph graph = createProjectExampleGraph();

        DijkstraResult result = LSRDijkstraCalculator.compute(graph, "A");

        assertEquals("A", result.getSourceNode());
        assertRoute(result, "B", 5, "A", "B");
        assertRoute(result, "C", 3, "A", "C");
        assertRoute(result, "D", 4, "A", "C", "D");
        assertRoute(result, "E", 7, "A", "C", "D", "E");
        assertRoute(result, "F", 7, "A", "B", "F");
    }

    @Test
    void recordsSingleStepDiscoveryOrder() {
        Graph graph = createProjectExampleGraph();

        DijkstraResult result = LSRDijkstraCalculator.compute(graph, "A");

        assertEquals(5, result.getSteps().size());
        assertStep(result.getSteps().get(0), "C", 3, "A", "C");
        assertStep(result.getSteps().get(1), "D", 4, "A", "C", "D");
        assertStep(result.getSteps().get(2), "B", 5, "A", "B");
        assertStep(result.getSteps().get(3), "E", 7, "A", "C", "D", "E");
        assertStep(result.getSteps().get(4), "F", 7, "A", "B", "F");
    }

    @Test
    void reportsUnreachableRoutes() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 2);
        graph.addNode("C");

        DijkstraResult result = LSRDijkstraCalculator.compute(graph, "A");

        assertRoute(result, "B", 2, "A", "B");
        DijkstraRouteResult routeToC = result.getRoutes().get("C");
        assertFalse(routeToC.isReachable());
        assertTrue(routeToC.getPath().isEmpty());
        assertEquals(Integer.MAX_VALUE, routeToC.getCost());
        assertEquals(1, result.getSteps().size());
    }

    @Test
    void rejectsInvalidComputeInputs() {
        Graph graph = new Graph();
        graph.addNode("A");

        assertThrows(IllegalArgumentException.class, () -> LSRDijkstraCalculator.compute(null, "A"));
        assertThrows(IllegalArgumentException.class, () -> LSRDijkstraCalculator.compute(graph, "Z"));
    }

    @Test
    void routeResultReachabilityDependsOnPath() {
        DijkstraRouteResult reachable = new DijkstraRouteResult("B", List.of("A", "B"), 4);
        DijkstraRouteResult unreachable = new DijkstraRouteResult("C", List.of(), Integer.MAX_VALUE);

        assertTrue(reachable.isReachable());
        assertFalse(unreachable.isReachable());
    }

    @Test
    void resultObjectsCopyAndProtectTheirCollections() {
        List<String> path = new ArrayList<>(List.of("A", "B"));
        DijkstraRouteResult route = new DijkstraRouteResult("B", path, 4);
        path.add("C");

        assertEquals(List.of("A", "B"), route.getPath());
        assertThrows(UnsupportedOperationException.class, () -> route.getPath().add("C"));

        List<DijkstraStep> steps = new ArrayList<>(List.of(new DijkstraStep("B", List.of("A", "B"), 4)));
        Map<String, DijkstraRouteResult> routes = new LinkedHashMap<>();
        routes.put("B", route);

        DijkstraResult result = new DijkstraResult("A", routes, steps);
        routes.clear();
        steps.clear();

        assertEquals(1, result.getRoutes().size());
        assertEquals(1, result.getSteps().size());
        assertThrows(UnsupportedOperationException.class, () -> result.getRoutes().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.getSteps().clear());
    }

    private static Graph createProjectExampleGraph() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 5);
        graph.addEdge("A", "C", 3);
        graph.addEdge("A", "D", 5);
        graph.addEdge("B", "C", 4);
        graph.addEdge("B", "E", 3);
        graph.addEdge("B", "F", 2);
        graph.addEdge("C", "D", 1);
        graph.addEdge("C", "E", 6);
        graph.addEdge("D", "E", 3);
        graph.addEdge("E", "F", 5);
        return graph;
    }

    private static void assertRoute(DijkstraResult result, String destination, int cost, String... path) {
        DijkstraRouteResult route = result.getRoutes().get(destination);

        assertEquals(cost, route.getCost());
        assertEquals(List.of(path), route.getPath());
        assertTrue(route.isReachable());
    }

    private static void assertStep(DijkstraStep step, String foundNode, int cost, String... path) {
        assertEquals(foundNode, step.getFoundNode());
        assertEquals(cost, step.getCost());
        assertEquals(List.of(path), step.getPath());
    }
}
