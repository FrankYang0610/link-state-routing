import models.Graph;
import models.GraphLink;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

public final class LSRGraphValidator {
    private LSRGraphValidator() { }

    public static boolean isValid(Graph graph) {
        return findErrors(graph).isEmpty();
    }

    public static void validate(Graph graph) {
        List<String> errors = findErrors(graph);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.get(0));
        }
    }

    public static List<String> findErrors(Graph graph) {
        List<String> errors = new ArrayList<String>();

        if (graph == null) {
            errors.add("Graph is null.");
            return errors;
        }

        Map<String, Set<String>> checkedLinks = new LinkedHashMap<String, Set<String>>();

        for (String fromNode : graph.getNodes()) {
            if (isBlank(fromNode)) {
                errors.add("Bad node (" + fromNode + ")");
                continue;
            }

            Set<String> neighbors = new LinkedHashSet<String>();
            for (GraphLink link : graph.getNeighbors(fromNode)) {
                if (link == null) {
                    errors.add("Bad link (" + fromNode + ")");
                    continue;
                }

                String toNode = link.getToNode();
                if (isBlank(toNode)) {
                    errors.add("Bad link (" + fromNode + "->" + toNode + ")");
                    continue;
                }
                if (!neighbors.add(toNode)) {
                    errors.add("Duplicate link (" + fromNode + "->" + toNode + ")");
                }
                if (fromNode.equals(toNode)) {
                    errors.add("Self link (" + fromNode + ")");
                }
                if (!graph.containsNode(toNode)) {
                    errors.add("Missing node (" + toNode + ")");
                    continue;
                }

                if (hasChecked(checkedLinks, fromNode, toNode)) {
                    continue;
                }
                markChecked(checkedLinks, fromNode, toNode);

                OptionalInt reverseCost = graph.getCost(toNode, fromNode);
                if (reverseCost.isEmpty()) {
                    errors.add("Missing reverse link (" + toNode + "->" + fromNode + ")");
                } else if (reverseCost.getAsInt() != link.getCost()) {
                    errors.add("Cost mismatch (" + fromNode + "->" + toNode + "=" + link.getCost()
                            + ", " + toNode + "->" + fromNode + "=" + reverseCost.getAsInt() + ")");
                }
            }
        }

        return errors;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean hasChecked(Map<String, Set<String>> checkedLinks, String fromNode, String toNode) {
        Set<String> neighbors = checkedLinks.get(fromNode);
        return neighbors != null && neighbors.contains(toNode);
    }

    private static void markChecked(Map<String, Set<String>> checkedLinks, String fromNode, String toNode) {
        addCheckedLink(checkedLinks, fromNode, toNode);
        addCheckedLink(checkedLinks, toNode, fromNode);
    }

    private static void addCheckedLink(Map<String, Set<String>> checkedLinks, String fromNode, String toNode) {
        Set<String> neighbors = checkedLinks.computeIfAbsent(fromNode, k -> new LinkedHashSet<String>());
        neighbors.add(toNode);
    }
}
