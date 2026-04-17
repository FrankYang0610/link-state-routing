import models.Graph;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class LSREditorGUI extends JFrame {
    private static final int GRAPH_TAB_INDEX = 0;
    private static final int RAW_TAB_INDEX = 1;

    private Path filePath;
    private final Consumer<Path> saveCallback;
    private final JLabel pathLabel;
    private final JTabbedPane previewTabbedPane;
    private final JTable graphTable;
    private final DefaultTableModel graphTableModel;
    private final JTextArea rawTextArea;
    private final UndoManager undoManager;
    private final JButton undoButton;
    private final JButton redoButton;
    private final JTextArea statusTextArea;
    private final Color statusDefaultColor;
    private final JTextField nodeField;
    private final JTextField costField;
    private final JComboBox<String> removeNodeComboBox;
    private final JComboBox<String> linkFromComboBox;
    private final JComboBox<String> linkToComboBox;
    private final JComboBox<String> breakFromComboBox;
    private final JComboBox<String> breakToComboBox;
    private final JButton removeNodeButton;
    private final JButton saveLinkButton;
    private final JButton breakLinkButton;

    private Graph parsedGraph;
    private List<String> nodes;
    private boolean dirty;
    private boolean loading;
    private boolean replacingText;
    private boolean lastParseValid;
    private final boolean ready;

    public LSREditorGUI(Path filePath, Consumer<Path> saveCallback) {
        super("Edit LSA File - " + filePath.getFileName());

        this.filePath = filePath;
        this.saveCallback = saveCallback;
        pathLabel = new JLabel();
        previewTabbedPane = new JTabbedPane();
        graphTableModel = new DefaultTableModel() {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        graphTable = new JTable(graphTableModel);
        rawTextArea = new JTextArea();
        undoManager = new UndoManager();
        undoButton = new JButton("Undo");
        redoButton = new JButton("Redo");
        statusTextArea = new JTextArea(7, 20);
        statusDefaultColor = statusTextArea.getForeground();
        nodeField = new JTextField(10);
        costField = new JTextField(6);
        removeNodeComboBox = new JComboBox<String>();
        linkFromComboBox = new JComboBox<String>();
        linkToComboBox = new JComboBox<String>();
        breakFromComboBox = new JComboBox<String>();
        breakToComboBox = new JComboBox<String>();
        removeNodeButton = new JButton("Remove Node");
        saveLinkButton = new JButton("Save Link");
        breakLinkButton = new JButton("Break Link");
        nodes = new ArrayList<String>();
        updateFileDisplay();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setLayout(new BorderLayout(8, 8));

        add(createToolbar(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);

        setupEditor();
        setupGraphTable();
        setupStatusArea();
        setupCloseConfirmation();
        ready = loadFile(true);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
    }

    public static void open(String fileName, Consumer<Path> saveCallback) {
        final Path path = Paths.get(fileName);
        SwingUtilities.invokeLater(() -> {
            LSREditorGUI editor = new LSREditorGUI(path, saveCallback);
            if (editor.isReady()) {
                editor.setVisible(true);
            }
        });
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton saveButton = new JButton("Save");
        JButton saveAsButton = new JButton("Save As");
        JButton reloadButton = new JButton("Reload");
        JButton closeButton = new JButton("Close");

        undoButton.addActionListener(event -> undoText());
        redoButton.addActionListener(event -> redoText());
        saveButton.addActionListener(event -> saveFile());
        saveAsButton.addActionListener(event -> saveFileAs());
        reloadButton.addActionListener(event -> reloadFile());
        closeButton.addActionListener(event -> closeEditor());

        buttons.add(undoButton);
        buttons.add(redoButton);
        buttons.add(saveButton);
        buttons.add(saveAsButton);
        buttons.add(reloadButton);
        buttons.add(closeButton);

        toolbar.add(pathLabel, BorderLayout.CENTER);
        toolbar.add(buttons, BorderLayout.EAST);
        return toolbar;
    }

    private JSplitPane createMainPanel() {
        JPanel topologyPanel = createTopologyPanel();
        topologyPanel.setPreferredSize(new Dimension(360, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createPreviewPanel(), topologyPanel);
        splitPane.setResizeWeight(0.72);
        splitPane.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
        return splitPane;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JScrollPane graphScrollPane = new JScrollPane(graphTable);
        previewTabbedPane.addTab("Parsed Graph", graphScrollPane);
        previewTabbedPane.addTab("Raw LSA", createRawTextPane());

        JScrollPane statusScrollPane = new JScrollPane(statusTextArea);
        statusScrollPane.setBorder(BorderFactory.createTitledBorder("Parse Status (auto refresh)"));

        panel.add(previewTabbedPane, BorderLayout.CENTER);
        panel.add(statusScrollPane, BorderLayout.SOUTH);
        return panel;
    }

    private JScrollPane createRawTextPane() {
        JScrollPane scrollPane = new JScrollPane(rawTextArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private JPanel createTopologyPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Topology Operations"));
        panel.add(createAddNodePanel());
        panel.add(createRemoveNodePanel());
        panel.add(createAddLinkPanel());
        panel.add(createBreakLinkPanel());
        return panel;
    }

    private JPanel createAddNodePanel() {
        JPanel panel = createSection("Add Node");
        JButton addButton = new JButton("Add Node");
        addButton.addActionListener(event -> addNode());

        panel.add(new JLabel("Node:"));
        panel.add(nodeField);
        panel.add(addButton);
        return panel;
    }

    private JPanel createRemoveNodePanel() {
        JPanel panel = createSection("Remove Node");
        removeNodeButton.addActionListener(event -> removeNode());

        panel.add(new JLabel("Node:"));
        panel.add(removeNodeComboBox);
        panel.add(removeNodeButton);
        return panel;
    }

    private JPanel createAddLinkPanel() {
        JPanel panel = createSection("Add / Update Link");
        saveLinkButton.addActionListener(event -> addOrUpdateLink());

        panel.add(new JLabel("From:"));
        panel.add(linkFromComboBox);
        panel.add(new JLabel("To:"));
        panel.add(linkToComboBox);
        panel.add(new JLabel("Cost:"));
        panel.add(costField);
        panel.add(saveLinkButton);
        return panel;
    }

    private JPanel createBreakLinkPanel() {
        JPanel panel = createSection("Break Link");
        breakLinkButton.addActionListener(event -> breakLink());

        panel.add(new JLabel("From:"));
        panel.add(breakFromComboBox);
        panel.add(new JLabel("To:"));
        panel.add(breakToComboBox);
        panel.add(breakLinkButton);
        return panel;
    }

    private JPanel createSection(String title) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private void setupEditor() {
        rawTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        rawTextArea.setLineWrap(false);
        rawTextArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { textChanged(); }
            public void removeUpdate(DocumentEvent event) { textChanged(); }
            public void changedUpdate(DocumentEvent event) { textChanged(); }
        });
        rawTextArea.getDocument().addUndoableEditListener(event -> {
            if (!loading && !replacingText) {
                undoManager.addEdit(event.getEdit());
                updateUndoRedoButtons();
            }
        });
        updateUndoRedoButtons();
    }

    private void setupGraphTable() {
        graphTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        graphTable.setRowHeight(24);
        graphTable.getTableHeader().setReorderingAllowed(false);
        graphTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }

    private void setupStatusArea() {
        statusTextArea.setEditable(false);
        statusTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        statusTextArea.setLineWrap(true);
        statusTextArea.setWrapStyleWord(true);
    }

    private void setupCloseConfirmation() {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                closeEditor();
            }
        });
    }

    private boolean isReady() {
        return ready;
    }

    private boolean loadFile(boolean chooseInitialTab) {
        if (!Files.isRegularFile(filePath)) {
            showError(".lsa file does not exist: " + filePath);
            dispose();
            return false;
        }

        try {
            loading = true;
            rawTextArea.setText(Files.readString(filePath));
            rawTextArea.setCaretPosition(0);
            dirty = false;
            resetUndoHistory();
            parseCurrentText(chooseInitialTab);
            return true;
        } catch (IOException e) {
            showError(e.getMessage());
            dispose();
            return false;
        } finally {
            loading = false;
        }
    }

    private void reloadFile() {
        if (dirty && !confirmDiscardChanges()) { return; }
        loadFile(false);
    }

    private void textChanged() {
        if (loading || replacingText) {
            return;
        }
        dirty = true;
        parseCurrentText(false);
        updateUndoRedoButtons();
    }

    private void undoText() {
        try {
            if (undoManager.canUndo()) {
                undoManager.undo();
            }
        } catch (CannotUndoException e) {
            showError("Cannot undo this change.");
        } finally {
            updateUndoRedoButtons();
        }
    }

    private void redoText() {
        try {
            if (undoManager.canRedo()) {
                undoManager.redo();
            }
        } catch (CannotRedoException e) {
            showError("Cannot redo this change.");
        } finally {
            updateUndoRedoButtons();
        }
    }

    private void resetUndoHistory() {
        undoManager.discardAllEdits();
        updateUndoRedoButtons();
    }

    private void updateUndoRedoButtons() {
        undoButton.setEnabled(undoManager.canUndo());
        redoButton.setEnabled(undoManager.canRedo());
    }

    private void parseCurrentText(boolean chooseTab) {
        try {
            Graph graph = LSRFileParser.parseText(rawTextArea.getText());
            List<String> errors = LSRGraphValidator.findErrors(graph);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(String.join("\n", errors));
            }

            parsedGraph = graph;
            nodes = new ArrayList<String>(graph.getNodes());
            lastParseValid = true;
            renderGraphTable();
            refreshTopologyControls(true);
            if (chooseTab) {
                previewTabbedPane.setSelectedIndex(GRAPH_TAB_INDEX);
            }
            setStatus("Graph parsed successfully.\nNodes: " + graph.size()
                    + "\nUnsaved changes: " + (dirty ? "yes" : "no"), false);
        } catch (Exception e) {
            parsedGraph = null;
            nodes = new ArrayList<String>();
            lastParseValid = false;
            renderEmptyGraphTable();
            refreshTopologyControls(false);
            if (chooseTab) {
                previewTabbedPane.setSelectedIndex(RAW_TAB_INDEX);
            }
            setStatus("Parse failed. Fix the raw LSA text or reload the file.\n" + e.getMessage(), true);
        }
    }

    private void renderGraphTable() {
        Object[][] rows = new Object[nodes.size()][nodes.size() + 1];
        for (int row = 0; row < nodes.size(); row++) {
            String fromNode = nodes.get(row);
            rows[row][0] = fromNode;
            for (int column = 0; column < nodes.size(); column++) {
                rows[row][column + 1] = formatEdge(fromNode, nodes.get(column));
            }
        }
        graphTableModel.setDataVector(rows, createTableHeader());
        setColumnWidths();
    }

    private void renderEmptyGraphTable() {
        graphTableModel.setDataVector(new Object[0][0], new Object[0]);
    }

    private String formatEdge(String fromNode, String toNode) {
        if (fromNode.equals(toNode)) { return "0"; }
        OptionalInt cost = parsedGraph.getCost(fromNode, toNode);
        if (cost.isEmpty()) { return "-"; }
        return fromNode + "→" + toNode + " (" + cost.getAsInt() + ")";
    }

    private Object[] createTableHeader() {
        Object[] header = new Object[nodes.size() + 1];
        header[0] = "From \\ To";
        for (int i = 0; i < nodes.size(); i++) {
            header[i + 1] = nodes.get(i);
        }
        return header;
    }

    private void setColumnWidths() {
        for (int i = 0; i < graphTable.getColumnModel().getColumnCount(); i++) {
            graphTable.getColumnModel().getColumn(i).setPreferredWidth(i == 0 ? 80 : 140);
        }
    }

    private void refreshTopologyControls(boolean enabled) {
        refreshComboBox(removeNodeComboBox);
        refreshComboBox(linkFromComboBox);
        refreshComboBox(linkToComboBox);
        refreshComboBox(breakFromComboBox);
        refreshComboBox(breakToComboBox);

        removeNodeComboBox.setEnabled(enabled);
        linkFromComboBox.setEnabled(enabled);
        linkToComboBox.setEnabled(enabled);
        breakFromComboBox.setEnabled(enabled);
        breakToComboBox.setEnabled(enabled);
        removeNodeButton.setEnabled(enabled);
        saveLinkButton.setEnabled(enabled);
        breakLinkButton.setEnabled(enabled);
    }

    private void refreshComboBox(JComboBox<String> comboBox) {
        Object previousSelection = comboBox.getSelectedItem();
        comboBox.removeAllItems();
        if (parsedGraph != null) {
            for (String node : parsedGraph.getNodes()) {
                comboBox.addItem(node);
            }
        }
        if (previousSelection != null) {
            comboBox.setSelectedItem(previousSelection);
        }
    }

    private void addNode() {
        String node = nodeField.getText().trim();
        String error = validateNodeName(node);
        if (error != null) {
            showError(error);
            return;
        }

        Graph graph = requireValidGraph();
        if (graph == null) {
            return;
        }
        if (graph.containsNode(node)) {
            showError("Node already exists: " + node);
            return;
        }

        Graph nextGraph = LSRTopologyService.addNode(graph, node);
        applyGraphChange(nextGraph, "Added node " + node + ".");
        nodeField.setText("");
    }

    private void removeNode() {
        Graph graph = requireValidGraph();
        if (graph == null) {
            return;
        }

        String node = selected(removeNodeComboBox);
        if (node == null) {
            showError("Please choose a node to remove.");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Remove node " + node + " and all its links?",
                "Confirm Remove Node",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        Graph nextGraph = LSRTopologyService.removeNode(graph, node);
        applyGraphChange(nextGraph, "Removed node " + node + ".");
    }

    private void addOrUpdateLink() {
        Graph graph = requireValidGraph();
        if (graph == null) {
            return;
        }

        String fromNode = selected(linkFromComboBox);
        String toNode = selected(linkToComboBox);
        if (fromNode == null || toNode == null) {
            showError("Please choose both link endpoints.");
            return;
        }
        if (fromNode.equals(toNode)) {
            showError("A link cannot connect a node to itself.");
            return;
        }

        Integer cost = parseCost(costField.getText().trim());
        if (cost == null) {
            return;
        }

        Graph nextGraph = LSRTopologyService.addOrUpdateLink(graph, fromNode, toNode, cost.intValue());
        applyGraphChange(nextGraph, "Saved link " + fromNode + "-" + toNode + ".");
    }

    private void breakLink() {
        Graph graph = requireValidGraph();
        if (graph == null) {
            return;
        }

        String fromNode = selected(breakFromComboBox);
        String toNode = selected(breakToComboBox);
        if (fromNode == null || toNode == null) {
            showError("Please choose both link endpoints.");
            return;
        }
        if (fromNode.equals(toNode)) {
            showError("Choose two different nodes.");
            return;
        }
        if (!graph.containsEdge(fromNode, toNode) && !graph.containsEdge(toNode, fromNode)) {
            showError("No link exists between " + fromNode + " and " + toNode + ".");
            return;
        }

        Graph nextGraph = LSRTopologyService.breakLink(graph, fromNode, toNode);
        applyGraphChange(nextGraph, "Broke link " + fromNode + "-" + toNode + ".");
    }

    private Graph requireValidGraph() {
        if (parsedGraph == null || !lastParseValid) {
            showError("Fix the LSA parse errors before using topology operations.");
            return null;
        }
        return parsedGraph;
    }

    private void applyGraphChange(Graph graph, String message) {
        try {
            LSRGraphValidator.validate(graph);
            List<String> lines = LSRFileWriter.toLines(graph);
            String text = String.join(System.lineSeparator(), lines);
            if (!text.isEmpty()) {
                text += System.lineSeparator();
            }
            replaceRawTextWithUndo(text);
            setStatus(message + "\nPreview updated. Click Save or Save As to write the LSA file.", false);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void replaceRawTextWithUndo(String newText) {
        String oldText = rawTextArea.getText();
        if (oldText.equals(newText)) {
            return;
        }

        replaceRawTextWithoutUndo(newText);
        dirty = true;
        parseCurrentText(false);
        undoManager.addEdit(new AbstractUndoableEdit() {
            public void undo() throws CannotUndoException {
                super.undo();
                replaceRawTextWithoutUndo(oldText);
                dirty = true;
                parseCurrentText(false);
            }

            public void redo() throws CannotRedoException {
                super.redo();
                replaceRawTextWithoutUndo(newText);
                dirty = true;
                parseCurrentText(false);
            }
        });
        updateUndoRedoButtons();
    }

    private void replaceRawTextWithoutUndo(String text) {
        try {
            replacingText = true;
            rawTextArea.setText(text);
            rawTextArea.setCaretPosition(0);
        } finally {
            replacingText = false;
        }
    }

    private Integer parseCost(String value) {
        if (value.isEmpty()) {
            showError("Please enter a link cost.");
            return null;
        }

        try {
            int cost = Integer.parseInt(value);
            if (cost < 0) {
                showError("Link cost cannot be negative.");
                return null;
            }
            return Integer.valueOf(cost);
        } catch (NumberFormatException e) {
            showError("Link cost must be an integer.");
            return null;
        }
    }

    private String validateNodeName(String node) {
        if (node.isEmpty()) {
            return "Node name cannot be empty.";
        }
        if (node.contains(":")) {
            return "Node name cannot contain ':'.";
        }
        for (int i = 0; i < node.length(); i++) {
            if (Character.isWhitespace(node.charAt(i))) {
                return "Node name cannot contain whitespace.";
            }
        }
        return null;
    }

    private String selected(JComboBox<String> comboBox) {
        Object selectedItem = comboBox.getSelectedItem();
        if (selectedItem == null) {
            return null;
        }
        return String.valueOf(selectedItem);
    }

    private void saveFile() {
        if (confirmSaveInvalidText() && writeFile(filePath)) {
            dirty = false;
            parseCurrentText(false);
            notifySaved("File saved.");
        }
    }

    private void saveFileAs() {
        if (!confirmSaveInvalidText()) {
            return;
        }

        JFileChooser chooser = new JFileChooser(getChooserDirectory());
        chooser.setDialogTitle("Save LSA File As");
        chooser.setFileFilter(new FileNameExtensionFilter("LSA files", "lsa"));
        chooser.setSelectedFile(filePath.getFileName().toFile());

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path targetPath = ensureLSAExtension(chooser.getSelectedFile().toPath());
        if (Files.isRegularFile(targetPath) && !confirmOverwrite(targetPath)) {
            return;
        }

        if (writeFile(targetPath)) {
            filePath = targetPath;
            dirty = false;
            updateFileDisplay();
            parseCurrentText(false);
            notifySaved("File saved as " + targetPath.toAbsolutePath() + ".");
        }
    }

    private boolean confirmSaveInvalidText() {
        if (lastParseValid) {
            return true;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "The current LSA text has parse errors. Save it anyway?",
                "Save Invalid LSA",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    private File getChooserDirectory() {
        Path parent = filePath.toAbsolutePath().getParent();
        if (parent == null) {
            return new File(".");
        }
        return parent.toFile();
    }

    private Path ensureLSAExtension(Path path) {
        if (path.getFileName().toString().toLowerCase().endsWith(".lsa")) {
            return path;
        }
        return path.resolveSibling(path.getFileName() + ".lsa");
    }

    private boolean confirmOverwrite(Path targetPath) {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Overwrite existing file?\n" + targetPath.toAbsolutePath(),
                "Confirm Save As",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    private boolean writeFile(Path targetPath) {
        try {
            Files.write(targetPath, rawTextArea.getText().getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            showError(e.getMessage());
            return false;
        }
    }

    private void notifySaved(String message) {
        if (saveCallback != null) {
            saveCallback.accept(filePath);
        }
        JOptionPane.showMessageDialog(this, message, "Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateFileDisplay() {
        setTitle("Edit LSA File - " + filePath.getFileName());
        pathLabel.setText("File: " + filePath.toAbsolutePath());
    }

    private void closeEditor() {
        if (dirty && !confirmDiscardChanges()) { return; }
        dispose();
    }

    private boolean confirmDiscardChanges() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Discard unsaved changes?",
                "Unsaved Changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    private void setStatus(String message, boolean error) {
        statusTextArea.setForeground(error ? Color.RED : statusDefaultColor);
        statusTextArea.setText(message);
        statusTextArea.setCaretPosition(0);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
