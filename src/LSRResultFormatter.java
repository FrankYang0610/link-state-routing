import models.DijkstraResult;
import models.DijkstraStep;
import models.RouteResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public final class LSRResultFormatter {
    private LSRResultFormatter() {
    }

    public static void printSingleStepResult(DijkstraResult result) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (DijkstraStep step : result.getSteps()) {
            System.out.println(formatStep(step) + " [press Enter to continue]");
            reader.readLine();
        }
        printSummary(result);
    }

    public static void printSummary(DijkstraResult result) {
        System.out.print(formatSummary(result));
    }

    public static String formatStep(DijkstraStep step) {
        return "Found " + step.getFoundNode()
                + ": Path: " + formatPath(step.getPath())
                + " Cost: " + step.getCost();
    }

    public static String formatSingleStepResult(DijkstraResult result) {
        StringBuilder text = new StringBuilder();
        for (DijkstraStep step : result.getSteps()) {
            text.append(formatStep(step)).append("\n");
        }
        text.append("\n").append(formatSummary(result));
        return text.toString();
    }

    public static String formatSummary(DijkstraResult result) {
        StringBuilder text = new StringBuilder();
        text.append("Source ").append(result.getSourceNode()).append(":\n");
        for (RouteResult route : result.getRoutes().values()) {
            if (route.isReachable()) {
                text.append(route.getDestinationNode())
                        .append(": Path: ")
                        .append(formatPath(route.getPath()))
                        .append(" Cost: ")
                        .append(route.getCost())
                        .append("\n");
            } else {
                text.append(route.getDestinationNode()).append(": Unreachable\n");
            }
        }
        return text.toString();
    }

    public static String formatPath(List<String> path) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                text.append(">");
            }
            text.append(path.get(i));
        }
        return text.toString();
    }
}
