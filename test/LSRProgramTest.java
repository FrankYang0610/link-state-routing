import Argparser.Argparser;
import Argparser.Command;
import Argparser.Parameters;

import models.DijkstraResult;
import models.Graph;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LSRProgramTest {
    @Command(name = "DemoCommand")
    private static class DemoOptions {
        @Parameters(index = 0, name = "file")
        private String file;

        @Parameters(index = 1, required = false, defaultValue = "CA", choices = {"SS", "CA"})
        private String mode;
    }

    @Test
    void fileParserReadsValidLsaFile() throws Exception {
        Path file = writeTempLsa(
                "A: B:5 C:3",
                "B: A:5 C:4",
                "C: A:3 B:4"
        );

        Graph graph = LSRFileParser.parse(file);

        assertEquals(3, graph.size());
        assertEquals(5, graph.getCost("A", "B").orElseThrow());
        assertEquals(4, graph.getCost("B", "C").orElseThrow());
    }

    @Test
    void fileParserRejectsBadInput() {
        assertThrows(IOException.class, () -> LSRFileParser.parse(Path.of("missing-file.lsa")));
        assertThrows(IllegalArgumentException.class, () -> LSRFileParser.parse(writeTempLsa("A B:5")));
        assertThrows(IllegalArgumentException.class, () -> LSRFileParser.parse(writeTempLsa("A: B")));
        assertThrows(IllegalArgumentException.class, () -> LSRFileParser.parse(writeTempLsa("A: B:-1")));
        assertThrows(IllegalArgumentException.class, () -> LSRFileParser.parse(writeTempLsa("A: B:1 B:2")));
        assertThrows(IllegalArgumentException.class, () -> LSRFileParser.parse(writeTempLsa("A: A:1")));
    }

    @Test
    void validatorAcceptsValidGraphAndReportsBrokenGraph() throws Exception {
        Graph valid = LSRFileParser.parse(writeTempLsa(
                "A: B:5",
                "B: A:5"
        ));

        Graph missingReverse = LSRFileParser.parse(writeTempLsa(
                "A: B:5",
                "B:"
        ));

        Graph costMismatch = LSRFileParser.parse(writeTempLsa(
                "A: B:5",
                "B: A:4"
        ));

        assertTrue(LSRGraphValidator.isValid(valid));
        assertTrue(LSRGraphValidator.findErrors(valid).isEmpty());

        assertFalse(LSRGraphValidator.isValid(missingReverse));
        assertTrue(LSRGraphValidator.findErrors(missingReverse).get(0).contains("Missing reverse link"));

        assertFalse(LSRGraphValidator.isValid(costMismatch));
        assertTrue(LSRGraphValidator.findErrors(costMismatch).get(0).contains("Cost mismatch"));
    }

    @Test
    void formatterPrintsStepsSummaryAndUnreachableRoutes() throws Exception {
        Graph graph = LSRFileParser.parse(writeTempLsa(
                "A: B:2",
                "B: A:2",
                "C:"
        ));

        DijkstraResult result = LSRDijkstraCalculator.compute(graph, "A");
        String pathToB = LSRResultFormatter.formatPath(List.of("A", "B"));
        String firstStep = LSRResultFormatter.formatStep(result.getSteps().get(0));
        String summary = LSRResultFormatter.formatSummary(result);

        assertEquals(pathToB, LSRResultFormatter.formatPath(List.of("A", "B")));
        assertTrue(firstStep.contains("Found B: Path: " + pathToB));
        assertTrue(summary.contains("B: Path: " + pathToB + " Cost: 2"));
        assertTrue(summary.contains("C: Unreachable"));
    }

    @Test
    void argparserParsesDefaultsChoicesAndUsage() {
        DemoOptions options = Argparser.parse(new String[]{"routes.lsa"}, DemoOptions.class);

        assertEquals("routes.lsa", options.file);
        assertEquals("CA", options.mode);
        assertEquals("Usage: java DemoCommand <file> [SS|CA]", Argparser.usage(DemoOptions.class));

        DemoOptions ssOptions = Argparser.parse(new String[]{"routes.lsa", "ss"}, DemoOptions.class);
        assertEquals("SS", ssOptions.mode);

        assertThrows(IllegalArgumentException.class, () -> Argparser.parse(new String[]{}, DemoOptions.class));
        assertThrows(IllegalArgumentException.class, () -> Argparser.parse(new String[]{"routes.lsa", "BAD"}, DemoOptions.class));
    }

    @Test
    void lsrComputeRunsComputeAllMode() throws Exception {
        Path file = writeTempLsa(
                "A: B:5 C:3 D:5",
                "B: A:5 C:4 E:3 F:2",
                "C: A:3 B:4 D:1 E:6",
                "D: A:5 C:1 E:3",
                "E: B:3 C:6 D:3 F:5",
                "F: B:2 E:5"
        );

        String output = captureStdout(() -> LSRCompute.main(new String[]{file.toString(), "A", "CA"}));

        assertTrue(output.contains("Source A:"));
        assertTrue(output.contains("D: Path: " + LSRResultFormatter.formatPath(List.of("A", "C", "D")) + " Cost: 4"));
        assertTrue(output.contains("F: Path: " + LSRResultFormatter.formatPath(List.of("A", "B", "F")) + " Cost: 7"));
    }

    @Test
    void lsrComputeReportsUnknownSourceAndUsageErrors() throws Exception {
        Path file = writeTempLsa(
                "A: B:5",
                "B: A:5"
        );

        String unknownSourceOutput = captureStdout(() -> LSRCompute.main(new String[]{file.toString(), "Z", "CA"}));
        String badModeOutput = captureStdout(() -> LSRCompute.main(new String[]{file.toString(), "A", "BAD"}));

        assertTrue(unknownSourceOutput.contains("Unknown source (Z)"));
        assertTrue(badModeOutput.contains("Bad value (BAD)"));
        assertTrue(badModeOutput.contains("Usage: java LSRCompute <file.lsa> <source> [SS|CA]"));
    }

    private static Path writeTempLsa(String... lines) throws IOException {
        Path file = Files.createTempFile("lsr-test-", ".lsa");
        Files.write(file, List.of(lines));
        file.toFile().deleteOnExit();
        return file;
    }

    private static String captureStdout(ThrowingRunnable action) throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output));
            action.run();
        } finally {
            System.setOut(originalOut);
        }

        return output.toString();
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
