import Argparser.Argparser;
import Argparser.Command;
import Argparser.Parameters;
import models.DijkstraResult;
import models.Graph;

public class LSRCompute {
    @Command(name = "LSRCompute", description = "Link State Routing calculator")
    private static class Options {
        @Parameters(index = 0, name = "file.lsa", description = "LSA input file")
        private String fileName;

        @Parameters(index = 1, name = "source", description = "source router")
        private String sourceNode;

        @Parameters(index = 2, required = false, defaultValue = "CA", choices = {"SS", "CA"})
        private String mode;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            LSRGUI.open();
            return;
        }

        try {
            Options options = Argparser.parse(args, Options.class);
            Graph graph = LSRFileParser.parse(options.fileName);
            LSRGraphValidator.validate(graph);

            if (!graph.containsNode(options.sourceNode)) {
                System.out.println("Unknown source (" + options.sourceNode + ")");
                return;
            }

            DijkstraResult result = LSRDijkstraCalculator.compute(graph, options.sourceNode);
            if (options.mode.equals("SS")) {
                LSRResultFormatter.printSingleStepResult(result);
            } else {
                LSRResultFormatter.printSummary(result);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(Argparser.usage(Options.class));
        }
    }
}
