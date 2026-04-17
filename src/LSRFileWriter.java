import models.Graph;
import models.GraphLink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LSRFileWriter {
    private LSRFileWriter() {
    }

    public static void write(Graph graph, Path filePath) throws IOException {
        if (graph == null) {
            throw new IllegalArgumentException("Graph is null.");
        }
        if (filePath == null) {
            throw new IllegalArgumentException("File path is null.");
        }

        Files.write(filePath, toLines(graph), StandardCharsets.UTF_8);
    }

    public static List<String> toLines(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph is null.");
        }

        List<String> lines = new ArrayList<String>();
        for (String node : graph.getNodes()) {
            StringBuilder line = new StringBuilder(node).append(":");
            for (GraphLink link : graph.getNeighbors(node)) {
                line.append(" ")
                        .append(link.getToNode())
                        .append(":")
                        .append(link.getCost());
            }
            lines.add(line.toString());
        }
        return lines;
    }
}
