import models.Graph;
import models.GraphLink;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphAndLinkTest {
    @Test
    void newGraphStartsEmpty() {
        Graph graph = new Graph();

        assertEquals(0, graph.size());
        assertTrue(graph.getNodes().isEmpty());
        assertTrue(graph.getNeighbors("A").isEmpty());
        assertTrue(graph.getCost("A", "B").isEmpty());
        assertFalse(graph.containsNode("A"));
        assertFalse(graph.containsEdge("A", "B"));
    }

    @Test
    void addNodeIsIdempotent() {
        Graph graph = new Graph();

        graph.addNode("A");
        graph.addNode("A");

        assertEquals(1, graph.size());
        assertEquals(Set.of("A"), graph.getNodes());
    }

    @Test
    void addEdgeCreatesTwoDirectedLinks() {
        Graph graph = new Graph();

        graph.addEdge("A", "B", 5);

        assertTrue(graph.containsNode("A"));
        assertTrue(graph.containsNode("B"));
        assertTrue(graph.containsEdge("A", "B"));
        assertTrue(graph.containsEdge("B", "A"));
        assertEquals(5, graph.getCost("A", "B").orElseThrow());
        assertEquals(5, graph.getCost("B", "A").orElseThrow());
    }

    @Test
    void addDirectedEdgeCreatesOnlyOneDirection() {
        Graph graph = new Graph();

        graph.addDirectedEdge("A", "B", 7);

        assertTrue(graph.containsNode("A"));
        assertTrue(graph.containsNode("B"));
        assertTrue(graph.containsEdge("A", "B"));
        assertFalse(graph.containsEdge("B", "A"));
        assertEquals(7, graph.getCost("A", "B").orElseThrow());
    }

    @Test
    void addingSameEdgeUpdatesExistingCost() {
        Graph graph = new Graph();

        graph.addEdge("A", "B", 5);
        graph.addEdge("A", "B", 9);

        assertEquals(9, graph.getCost("A", "B").orElseThrow());
        assertEquals(9, graph.getCost("B", "A").orElseThrow());
        assertEquals(1, graph.getNeighbors("A").size());
        assertEquals(1, graph.getNeighbors("B").size());
    }

    @Test
    void removeEdgeRemovesBothDirections() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 5);
        graph.addEdge("A", "C", 3);

        graph.removeEdge("A", "B");

        assertFalse(graph.containsEdge("A", "B"));
        assertFalse(graph.containsEdge("B", "A"));
        assertTrue(graph.containsEdge("A", "C"));
    }

    @Test
    void removeDirectedEdgeRemovesOnlyRequestedDirection() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 5);

        graph.removeDirectedEdge("A", "B");

        assertFalse(graph.containsEdge("A", "B"));
        assertTrue(graph.containsEdge("B", "A"));
    }

    @Test
    void removeNodeRemovesIncidentLinks() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 5);
        graph.addEdge("A", "C", 3);

        graph.removeNode("A");

        assertFalse(graph.containsNode("A"));
        assertFalse(graph.containsEdge("B", "A"));
        assertFalse(graph.containsEdge("C", "A"));
        assertTrue(graph.containsNode("B"));
        assertTrue(graph.containsNode("C"));
    }

    @Test
    void graphViewsAreReadOnly() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 5);

        Set<String> nodes = graph.getNodes();
        List<GraphLink> neighbors = graph.getNeighbors("A");
        Map<String, List<GraphLink>> adjacencyList = graph.getAdjacencyList();

        assertThrows(UnsupportedOperationException.class, () -> nodes.add("C"));
        assertThrows(UnsupportedOperationException.class, () -> neighbors.add(new GraphLink("C", 1)));
        assertThrows(UnsupportedOperationException.class, () -> adjacencyList.put("C", List.of()));
        assertThrows(UnsupportedOperationException.class, () -> adjacencyList.get("A").add(new GraphLink("C", 1)));
    }

    @Test
    void graphLinkRejectsNegativeCost() {
        assertThrows(IllegalArgumentException.class, () -> new GraphLink("B", -1));
    }
}
