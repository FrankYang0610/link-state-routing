import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class LSRFileEditor extends JFrame {
    private Path filePath;
    private final Consumer<Path> saveCallback;
    private final JLabel pathLabel;
    private final JTextArea editorTextArea;
    private boolean dirty;
    private boolean loading;
    private final boolean ready;

    public LSRFileEditor(Path filePath, Consumer<Path> saveCallback) {
        super("Edit LSA File - " + filePath.getFileName());

        this.filePath = filePath;
        this.saveCallback = saveCallback;
        pathLabel = new JLabel();
        editorTextArea = new JTextArea();
        updateFileDisplay();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setLayout(new BorderLayout(8, 8));

        add(createToolbar(), BorderLayout.NORTH);
        add(createEditorPane(), BorderLayout.CENTER);

        setupEditor();
        setupCloseConfirmation();
        ready = loadFile();

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
    }

    public static void open(String fileName, Consumer<Path> saveCallback) {
        final Path path = Paths.get(fileName);
        SwingUtilities.invokeLater(() -> {
            LSRFileEditor editor = new LSRFileEditor(path, saveCallback);
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

        saveButton.addActionListener(event -> saveFile());
        saveAsButton.addActionListener(event -> saveFileAs());
        reloadButton.addActionListener(event -> reloadFile());
        closeButton.addActionListener(event -> closeEditor());

        buttons.add(saveButton);
        buttons.add(saveAsButton);
        buttons.add(reloadButton);
        buttons.add(closeButton);

        toolbar.add(pathLabel, BorderLayout.CENTER);
        toolbar.add(buttons, BorderLayout.EAST);
        return toolbar;
    }

    private JScrollPane createEditorPane() {
        JScrollPane scrollPane = new JScrollPane(editorTextArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("LSA File Content"));
        return scrollPane;
    }

    private void setupEditor() {
        editorTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editorTextArea.setLineWrap(false);
        editorTextArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { markDirty(); }
            public void removeUpdate(DocumentEvent event) { markDirty(); }
            public void changedUpdate(DocumentEvent event) { markDirty(); }
        });
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

    private boolean loadFile() {
        if (!Files.isRegularFile(filePath)) {
            showError(".lsa file does not exist: " + filePath);
            dispose();
            return false;
        }

        try {
            loading = true;
            editorTextArea.setText(Files.readString(filePath));
            editorTextArea.setCaretPosition(0);
            dirty = false;
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
        loadFile();
    }

    private void saveFile() {
        if (writeFile(filePath)) {
            dirty = false;
            notifySaved("File saved.");
        }
    }

    private void saveFileAs() {
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
            notifySaved("File saved as " + targetPath.toAbsolutePath() + ".");
        }
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
            Files.write(targetPath, editorTextArea.getText().getBytes(StandardCharsets.UTF_8));
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

    private void markDirty() {
        if (!loading) { dirty = true; }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
