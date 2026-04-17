import models.Graph;
import models.GraphLink;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LSRFileParser {
    private LSRFileParser() {
    }

    public static Graph parse(String fileName) throws IOException {
        return parse(Paths.get(fileName));
    }

    public static Graph parse(Path filePath) throws IOException {
        if (!Files.isRegularFile(filePath)) {
            throw new IOException(".lsa file does not exist: " + filePath);
        }

        return parseLines(Files.readAllLines(filePath));
    }

    public static Graph parseText(String content) {
        if (content == null) {
            throw new IllegalArgumentException("LSA content is null.");
        }
        return parseLines(Arrays.asList(content.split("\\R", -1)));
    }

    private static Graph parseLines(List<String> lines) {
        Graph graph = new Graph();
        Set<String> sourceNodes = new LinkedHashSet<String>();
        int lineNumber = 0;

        for (String rawLine : lines) {
            lineNumber++;
            String line = rawLine.trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] columns = line.split("\\s+");
            String fromNode = parseSourceNode(columns[0], lineNumber);

            if (!sourceNodes.add(fromNode)) {
                throw error(lineNumber, "duplicate node", fromNode);
            }
            graph.addNode(fromNode);

            Set<String> neighbors = new LinkedHashSet<String>();
            for (int i = 1; i < columns.length; i++) {
                GraphLink link = parseLink(columns[i], lineNumber);

                if (fromNode.equals(link.getToNode())) {
                    throw error(lineNumber, "self link", fromNode);
                }
                if (!neighbors.add(link.getToNode())) {
                    throw error(lineNumber, "duplicate link", fromNode + "->" + link.getToNode());
                }
                graph.addDirectedEdge(fromNode, link);
            }
        }

        return graph;
    }

    private static String parseSourceNode(String column, int lineNumber) {
        if (!column.endsWith(":") || column.length() == 1) {
            throw error(lineNumber, "bad node", column);
        }
        String node = column.substring(0, column.length() - 1);
        if (node.contains(":")) {
            throw error(lineNumber, "bad node", column);
        }
        return node;
    }

    private static GraphLink parseLink(String column, int lineNumber) {
        String[] parts = column.split(":", -1);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw error(lineNumber, "bad link", column);
        }

        int cost;
        try {
            cost = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw error(lineNumber, e.toString(), column);
        }

        if (cost < 0) {
            throw error(lineNumber, "negative cost", column);
        }

        return new GraphLink(parts[0], cost);
    }

    private static IllegalArgumentException error(int lineNumber, String message, String value) {
        return new IllegalArgumentException("Line " + lineNumber + ": " + message + " (" + value + ")");
    }
}
