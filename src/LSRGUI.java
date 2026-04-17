import models.DijkstraResult;
import models.DijkstraStep;
import models.Graph;
import models.RouteResult;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class LSRGUI extends JFrame {
    private final JTextField filePathField;
    private final JButton browseButton;
    private final JButton parseButton;
    private final JButton editButton;
    private final JLabel sourceLabel;
    private final JComboBox<String> sourceComboBox;
    private final JRadioButton ssRadioButton;
    private final JRadioButton caRadioButton;
    private final JButton startButton;
    private final JButton nextButton;
    private final JButton stopButton;
    private final JButton resetButton;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JTextArea logTextArea;
    private final TitledBorder tableBorder;

    private Graph graph;
    private List<String> nodes;
    private DijkstraResult singleStepResult;
    private int shownStepCount;

    public LSRGUI() {
        super("LSA Routing");

        filePathField = new JTextField();
        browseButton = new JButton("Browse");
        parseButton = new JButton("Parse");
        editButton = new JButton("Edit");
        sourceLabel = new JLabel("Source:");
        sourceComboBox = new JComboBox<String>();
        ssRadioButton = new JRadioButton("SS");
        caRadioButton = new JRadioButton("CA", true);
        startButton = new JButton("Start Calculation");
        nextButton = new JButton("Next Step");
        stopButton = new JButton("Stop");
        resetButton = new JButton("Reset");
        tableModel = new DefaultTableModel() {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        logTextArea = new JTextArea();
        tableBorder = BorderFactory.createTitledBorder("Initial Topology");
        nodes = new ArrayList<String>();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 640));
        setLayout(new BorderLayout(8, 8));

        add(createControlPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);

        setupTable();
        setupLog();
        setupActions();
        clearGraph();

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
    }

    public static void open() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }

                new LSRGUI().setVisible(true);
            }
        });
    }

    private JPanel createControlPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));

        JPanel fileRow = new JPanel(new BorderLayout(8, 0));
        fileRow.add(new JLabel("File:"), BorderLayout.WEST);
        fileRow.add(filePathField, BorderLayout.CENTER);

        JPanel fileButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fileButtons.add(browseButton);
        fileButtons.add(parseButton);
        fileButtons.add(editButton);
        fileRow.add(fileButtons, BorderLayout.EAST);

        JPanel calculationRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(ssRadioButton);
        modeGroup.add(caRadioButton);

        calculationRow.add(new JLabel("Mode:"));
        calculationRow.add(ssRadioButton);
        calculationRow.add(caRadioButton);
        calculationRow.add(sourceLabel);
        calculationRow.add(sourceComboBox);
        calculationRow.add(startButton);
        calculationRow.add(nextButton);
        calculationRow.add(stopButton);
        calculationRow.add(resetButton);

        container.add(fileRow, BorderLayout.NORTH);
        container.add(calculationRow, BorderLayout.SOUTH);
        return container;
    }

    private JSplitPane createMainPanel() {
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(tableBorder);

        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScrollPane, logScrollPane);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
        return splitPane;
    }

    private void setupTable() {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }

    private void setupLog() {
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    }

    private void setupActions() {
        browseButton.addActionListener(event -> chooseFile());
        parseButton.addActionListener(event -> parseGraph());
        editButton.addActionListener(event -> editFile());
        ssRadioButton.addActionListener(event -> updateModeControls());
        caRadioButton.addActionListener(event -> updateModeControls());
        startButton.addActionListener(event -> startCalculation());
        nextButton.addActionListener(event -> nextStep());
        stopButton.addActionListener(event -> initial());
        resetButton.addActionListener(event -> initial());
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter("LSA files", "lsa"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            filePathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void parseGraph() {
        String filePath = filePathField.getText().trim();
        if (filePath.isEmpty()) {
            showError("Please choose a .lsa file.");
            return;
        }

        try {
            graph = LSRFileParser.parse(filePath);
            nodes = new ArrayList<String>(graph.getNodes());
            loadSourceNodes();
            initial();
            showValidationResult();
        } catch (Exception e) {
            clearGraph();
            log(e.getMessage());
            showError(e.getMessage());
        }
    }

    private void editFile() {
        String filePath = filePathField.getText().trim();
        if (filePath.isEmpty()) {
            showError("Please choose a .lsa file.");
            return;
        }
        LSRFileEditor.open(filePath, savedPath -> {
            filePathField.setText(savedPath.toAbsolutePath().toString());
            log("File saved. Click Parse to reload the graph.");
        });
    }

    private void startCalculation() {
        if (graph == null) {
            showError("Please parse a graph first.");
            return;
        }

        try {
            LSRGraphValidator.validate(graph);
            if (caRadioButton.isSelected()) {
                computeAll();
            } else {
                startSingleStep();
            }
        } catch (Exception e) {
            log(e.getMessage());
            showError(e.getMessage());
        }
    }

    private void initial() {
        singleStepResult = null;
        shownStepCount = 0;

        if (graph == null) {
            renderEmptyTable();
            showInitialButtons(false);
            return;
        }

        renderInitialTable();
        showInitialButtons(LSRGraphValidator.isValid(graph));
    }

    private void startSingleStep() {
        Object selectedSource = sourceComboBox.getSelectedItem();
        if (selectedSource == null) {
            showError("Please choose a source node.");
            return;
        }

        String sourceNode = String.valueOf(selectedSource);
        singleStepResult = LSRDijkstraCalculator.compute(graph, sourceNode);
        shownStepCount = 0;

        log("Single-step mode started.");
        log("Source: " + sourceNode);
        renderInitialTable();
        setTableTitle("Single-Step Progress");
        showSingleStepButtons();
        nextStep();
    }

    private void nextStep() {
        if (singleStepResult == null) {
            initial();
            return;
        }

        if (shownStepCount >= singleStepResult.getSteps().size()) {
            finishSingleStep();
            return;
        }

        DijkstraStep step = singleStepResult.getSteps().get(shownStepCount);
        shownStepCount++;

        log(LSRResultFormatter.formatStep(step));
        showSingleStepResult(step);

        if (shownStepCount >= singleStepResult.getSteps().size()) {
            finishSingleStep();
        } else {
            log("Click Next Step to continue.");
        }
    }

    private void computeAll() {
        log("Compute-all mode started.");
        renderComputeAllTable();
        log("Compute-all mode completed.");
        showFinishedButtons();
    }

    private void finishSingleStep() {
        log("");
        log(LSRResultFormatter.formatSummary(singleStepResult).trim());
        showFinishedButtons();
    }

    private void renderEmptyTable() {
        setTableTitle("Initial Topology");
        tableModel.setDataVector(new Object[0][0], new Object[0]);
    }

    private void renderInitialTable() {
        setTableTitle("Initial Topology");
        tableModel.setDataVector(createInitialRows(), createTableHeader());
        setColumnWidths();
    }

    private void showSingleStepResult(DijkstraStep step) {
        int sourceRow = nodes.indexOf(singleStepResult.getSourceNode());
        int destinationColumn = nodes.indexOf(step.getFoundNode());
        if (sourceRow >= 0 && destinationColumn >= 0) {
            tableModel.setValueAt(formatPath(step.getPath(), step.getCost()), sourceRow, destinationColumn + 1);
        }
    }

    private void renderComputeAllTable() {
        setTableTitle("Shortest Path Table");
        Object[][] rows = new Object[nodes.size()][nodes.size() + 1];

        for (int row = 0; row < nodes.size(); row++) {
            String sourceNode = nodes.get(row);
            DijkstraResult result = LSRDijkstraCalculator.compute(graph, sourceNode);
            rows[row][0] = sourceNode;

            for (int column = 0; column < nodes.size(); column++) {
                String destinationNode = nodes.get(column);
                rows[row][column + 1] = formatRoute(sourceNode, destinationNode, result);
            }
        }

        tableModel.setDataVector(rows, createTableHeader());
        setColumnWidths();
    }

    private Object[][] createInitialRows() {
        Object[][] rows = new Object[nodes.size()][nodes.size() + 1];

        for (int row = 0; row < nodes.size(); row++) {
            String fromNode = nodes.get(row);
            rows[row][0] = fromNode;

            for (int column = 0; column < nodes.size(); column++) {
                String toNode = nodes.get(column);
                rows[row][column + 1] = formatEdge(fromNode, toNode);
            }
        }

        return rows;
    }

    private String formatEdge(String fromNode, String toNode) {
        if (fromNode.equals(toNode)) { return "0"; }
        OptionalInt cost = graph.getCost(fromNode, toNode);
        if (cost.isEmpty()) { return "-"; }
        return fromNode + "→" + toNode + " (" + cost.getAsInt() + ")";
    }

    private String formatRoute(String sourceNode, String destinationNode, DijkstraResult result) {
        if (sourceNode.equals(destinationNode)) { return "0"; }
        RouteResult route = result.getRoutes().get(destinationNode);
        if (route == null || !route.isReachable()) { return "-"; }
        return formatPath(route.getPath(), route.getCost());
    }

    private String formatPath(List<String> path, int cost) {
        return LSRResultFormatter.formatPath(path) + " (" + cost + ")";
    }

    private Object[] createTableHeader() {
        Object[] header = new Object[nodes.size() + 1];
        header[0] = "From \\ To";
        for (int i = 0; i < nodes.size(); i++) {
            header[i + 1] = nodes.get(i);
        }
        return header;
    }

    private void loadSourceNodes() {
        sourceComboBox.removeAllItems();
        for (String node : nodes) {
            sourceComboBox.addItem(node);
        }
    }

    private void showValidationResult() {
        List<String> errors = LSRGraphValidator.findErrors(graph);

        if (errors.isEmpty()) {
            log("Graph parsed successfully.");
            log("Nodes: " + graph.size());
            log("Choose SS or CA, then click Start Calculation.");
        } else {
            log("Graph is invalid:");
            for (String error : errors) {
                log("- " + error);
            }
        }
    }

    private void clearGraph() {
        graph = null;
        nodes = new ArrayList<String>();
        singleStepResult = null;
        shownStepCount = 0;
        sourceComboBox.removeAllItems();
        renderEmptyTable();
        showInitialButtons(false);
    }

    private void showInitialButtons(boolean canStart) {
        startButton.setVisible(true);
        startButton.setEnabled(canStart);
        nextButton.setVisible(false);
        stopButton.setVisible(false);
        resetButton.setVisible(false);

        ssRadioButton.setEnabled(true);
        caRadioButton.setEnabled(true);
        updateModeControls();
    }

    private void showSingleStepButtons() {
        startButton.setVisible(false);
        nextButton.setVisible(true);
        nextButton.setEnabled(true);
        stopButton.setVisible(true);
        stopButton.setEnabled(true);
        resetButton.setVisible(true);
        resetButton.setEnabled(true);

        ssRadioButton.setEnabled(false);
        caRadioButton.setEnabled(false);
        sourceComboBox.setEnabled(false);
    }

    private void showFinishedButtons() {
        startButton.setVisible(false);
        nextButton.setVisible(false);
        stopButton.setVisible(false);
        resetButton.setVisible(true);
        resetButton.setEnabled(true);

        ssRadioButton.setEnabled(false);
        caRadioButton.setEnabled(false);
        sourceComboBox.setEnabled(false);
    }

    private void updateModeControls() {
        boolean hasGraph = graph != null && sourceComboBox.getItemCount() > 0;
        boolean singleStepMode = ssRadioButton.isSelected();

        sourceLabel.setVisible(singleStepMode);
        sourceComboBox.setVisible(singleStepMode);
        sourceComboBox.setEnabled(singleStepMode && hasGraph && ssRadioButton.isEnabled());

        revalidate();
        repaint();
    }

    private void setColumnWidths() {
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(i == 0 ? 80 : 140);
        }
    }

    private void setTableTitle(String title) {
        tableBorder.setTitle(title);
        repaint();
    }

    private void log(String message) {
        logTextArea.append(message);
        logTextArea.append("\n");
        logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
