package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

public class Graph {
    private final Map<String, List<GraphLink>> adjacencyList;

    public Graph() {
        this.adjacencyList = new LinkedHashMap<String, List<GraphLink>>();
    }

    public void addNode(String node) {
        if (!adjacencyList.containsKey(node)) {
            adjacencyList.put(node, new ArrayList<GraphLink>());
        }
    }

    public void addEdge(String fromNode, String toNode, int cost) {
        addDirectedEdge(fromNode, toNode, cost);
        addDirectedEdge(toNode, fromNode, cost);
    }

    public void addDirectedEdge(String fromNode, String toNode, int cost) {
        addDirectedEdge(fromNode, new GraphLink(toNode, cost));
    }

    public void addDirectedEdge(String fromNode, GraphLink link) {
        addNode(fromNode);
        addNode(link.getToNode());

        List<GraphLink> links = adjacencyList.get(fromNode);
        int index = findLinkIndex(links, link.getToNode());

        if (index == -1) {
            links.add(link);
        } else {
            links.set(index, link);
        }
    }

    public void removeNode(String node) {
        adjacencyList.remove(node);

        for (List<GraphLink> links : adjacencyList.values()) {
            removeLink(links, node);
        }
    }

    public void removeEdge(String fromNode, String toNode) {
        removeDirectedEdge(fromNode, toNode);
        removeDirectedEdge(toNode, fromNode);
    }

    public void removeDirectedEdge(String fromNode, String toNode) {
        List<GraphLink> links = adjacencyList.get(fromNode);
        if (links != null) {
            removeLink(links, toNode);
        }
    }

    public Set<String> getNodes() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(adjacencyList.keySet()));
    }

    public List<GraphLink> getNeighbors(String node) {
        List<GraphLink> links = adjacencyList.get(node);
        if (links == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(links);
    }

    public OptionalInt getCost(String fromNode, String toNode) {
        GraphLink link = getLink(fromNode, toNode);
        if (link == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(link.getCost());
    }

    public boolean containsNode(String node) {
        return adjacencyList.containsKey(node);
    }

    public boolean containsEdge(String fromNode, String toNode) {
        return getLink(fromNode, toNode) != null;
    }

    public int size() {
        return adjacencyList.size();
    }

    public Map<String, List<GraphLink>> getAdjacencyList() {
        Map<String, List<GraphLink>> copy = new LinkedHashMap<String, List<GraphLink>>();

        for (Map.Entry<String, List<GraphLink>> entry : adjacencyList.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        return Collections.unmodifiableMap(copy);
    }

    private GraphLink getLink(String fromNode, String toNode) {
        List<GraphLink> links = adjacencyList.get(fromNode);
        if (links == null) {
            return null;
        }

        int index = findLinkIndex(links, toNode);
        if (index == -1) {
            return null;
        }
        return links.get(index);
    }

    private int findLinkIndex(List<GraphLink> links, String toNode) {
        for (int i = 0; i < links.size(); i++) {
            if (links.get(i).getToNode().equals(toNode)) {
                return i;
            }
        }
        return -1;
    }

    private void removeLink(List<GraphLink> links, String toNode) {
        int index = findLinkIndex(links, toNode);
        if (index != -1) {
            links.remove(index);
        }
    }
}
