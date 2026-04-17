import models.Graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopologyServiceTest {
    @Test
    void addNodeReturnsNewGraphWithoutChangingOriginal() {
        Graph graph = new Graph();
        graph.addNode("A");

        Graph nextGraph = LSRTopologyService.addNode(graph, "B");

        assertNotSame(graph, nextGraph);
        assertTrue(nextGraph.containsNode("B"));
        assertFalse(graph.containsNode("B"));
    }

    @Test
    void addOrUpdateLinkKeepsLinksSymmetric() {
        Graph graph = new Graph();
        graph.addNode("A");
        graph.addNode("B");

        Graph nextGraph = LSRTopologyService.addOrUpdateLink(graph, "A", "B", 7);

        assertEquals(7, nextGraph.getCost("A", "B").orElseThrow());
        assertEquals(7, nextGraph.getCost("B", "A").orElseThrow());
        assertFalse(graph.containsEdge("A", "B"));
    }

    @Test
    void breakLinkRemovesBothDirections() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 4);

        Graph nextGraph = LSRTopologyService.breakLink(graph, "A", "B");

        assertFalse(nextGraph.containsEdge("A", "B"));
        assertFalse(nextGraph.containsEdge("B", "A"));
        assertTrue(graph.containsEdge("A", "B"));
    }

    @Test
    void removeNodeRemovesIncidentLinks() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 4);
        graph.addEdge("B", "C", 2);

        Graph nextGraph = LSRTopologyService.removeNode(graph, "B");

        assertFalse(nextGraph.containsNode("B"));
        assertFalse(nextGraph.containsEdge("A", "B"));
        assertFalse(nextGraph.containsEdge("C", "B"));
        assertTrue(graph.containsNode("B"));
    }
}
