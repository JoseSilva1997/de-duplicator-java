package gui;

import javax.swing.*;
import javax.swing.border.Border;

import backend.DeduplicationService;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Gui extends JFrame {

    private final FileCard guestCard;
    private final FileCard attendeeCard;
    private final JButton processButton;
    private final JLabel statusLabel;
    private final JProgressBar progressBar;

    private final DeduplicationService service = new DeduplicationService();

    public Gui() {
        super("Excel Sheet Comparator");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 640);
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BACKGROUND);

        SheetSelector picker = file -> {
            try {
                List<String> sheets = service.listSheets(file.toPath());
                if (sheets.isEmpty()) {
                    showError("No usable sheets", new IllegalStateException("File contains no visible sheets."));
                    return null;
                }
                if (sheets.size() == 1) {
                    return sheets;   // single-sheet file: auto-select, no dialog
                }
                return SheetPickerDialog.show(this, "Select sheets in " + file.getName(), sheets);
            } catch (IOException ex) {
                showError("Could not read sheet list", ex);
                return null;
            }
        };

        guestCard = new FileCard(
            "Guest list",
            "Upload your complete guest list",
            picker,
            service,
            this::updateProcessButton);
        attendeeCard = new FileCard(
            "Attendee list",
            "Upload your confirmed attendees list",
            picker,
            service,
            this::updateProcessButton);

        processButton = buildProcessButton();
        statusLabel = buildStatusLabel("Upload both files to begin");
        progressBar = buildProgressBar();

        layoutComponents();
        updateProcessButton();
        getRootPane().setDefaultButton(processButton);   // Enter key triggers it once enabled
    }

    private JProgressBar buildProgressBar() {
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new Dimension(280, 6));
        bar.setMaximumSize(new Dimension(280, 6));
        bar.setBorderPainted(false);
        bar.setVisible(false);   // only shown while processing
        return bar;
    }

    private void layoutComponents() {
        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(36, 48, 32, 48));

        root.add(buildHeader());
        root.add(Box.createVerticalStrut(28));
        root.add(buildCardsRow());
        root.add(Box.createVerticalStrut(32));
        root.add(centered(processButton));
        root.add(Box.createVerticalStrut(14));
        root.add(centered(progressBar));
        root.add(Box.createVerticalStrut(10));
        root.add(centered(statusLabel));
        root.add(Box.createVerticalGlue());

        add(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Excel Sheet Comparator");
        title.setFont(Theme.FONT_DISPLAY);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Remove duplicate attendees from your guest list");
        subtitle.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 14));
        subtitle.setForeground(Theme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(6, 0, 14, 0));

        JPanel accent = new JPanel();
        accent.setBackground(Theme.PRIMARY);
        accent.setMaximumSize(new Dimension(56, 3));
        accent.setPreferredSize(new Dimension(56, 3));
        accent.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(subtitle);
        header.add(accent);
        return header;
    }

    private JPanel buildCardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 22, 0));
        row.setOpaque(false);
        row.add(guestCard);
        row.add(attendeeCard);
        return row;
    }

    private JButton buildProcessButton() {
        JButton button = new JButton("Remove duplicates");
        button.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setBackground(Theme.PRIMARY);
        button.setForeground(Color.WHITE);
        Border padding = BorderFactory.createEmptyBorder(12, 36, 12, 36);
        button.setBorder(padding);
        button.addActionListener(e -> onRemoveDuplicates());
        return button;
    }

    private JLabel buildStatusLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 13));
        label.setForeground(Theme.TEXT_SECONDARY);
        return label;
    }

    private void updateProcessButton() {
        boolean ready = guestCard.hasFile() && attendeeCard.hasFile();
        processButton.setEnabled(ready);
        processButton.setBackground(ready ? Theme.PRIMARY : Theme.PRIMARY_DISABLED);

        if (!ready) {
            statusLabel.setText("Upload both files to begin");
            statusLabel.setForeground(Theme.TEXT_SECONDARY);
            return;
        }

        int guests = guestCard.getRecordCount();
        int attendees = attendeeCard.getRecordCount();
        if (guests < 0 || attendees < 0) {
            statusLabel.setText("Ready to process · counting rows…");
        } else {
            statusLabel.setText("Ready to process · "
                + guests + (guests == 1 ? " guest" : " guests")
                + " and "
                + attendees + (attendees == 1 ? " attendee" : " attendees"));
        }
        statusLabel.setForeground(Theme.SUCCESS);
    }

    private void onRemoveDuplicates() {
        Path primaryPath   = guestCard.getFile().toPath();
        Path secondaryPath = attendeeCard.getFile().toPath();
        List<String> primarySheets   = guestCard.getSelectedSheets();
        List<String> secondarySheets = attendeeCard.getSelectedSheets();

        processButton.setEnabled(false);
        processButton.setBackground(Theme.PRIMARY_DISABLED);
        statusLabel.setText("Processing…");
        statusLabel.setForeground(Theme.TEXT_SECONDARY);
        progressBar.setVisible(true);

        SwingWorker<DeduplicationService.Summary, Void> worker = new SwingWorker<>() {
            @Override
            protected DeduplicationService.Summary doInBackground() throws Exception {
                return service.run(primaryPath, primarySheets, secondaryPath, secondarySheets);
            }
            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    showSummary(get());
                } catch (Exception ex) {
                    showError("Processing failed", ex.getCause() != null ? ex.getCause() : ex);
                } finally {
                    updateProcessButton();
                }
            }
        };
        worker.execute();
    }

    private void showSummary(DeduplicationService.Summary s) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        panel.add(summaryLine("Processed " + s.sheetsProcessed() + " sheet(s)"));
        panel.add(summaryLine("Kept:    " + s.totalKept()));
        panel.add(summaryLine("Removed: " + s.totalRemoved()));
        panel.add(Box.createVerticalStrut(12));
        panel.add(summaryLine("Output saved to:"));

        // JTextArea wraps long paths (no spaces) at character boundaries; JLabel can't.
        JTextArea pathArea = new JTextArea(s.outputDirectory().toString());
        pathArea.setLineWrap(true);
        pathArea.setWrapStyleWord(false);
        pathArea.setEditable(false);
        pathArea.setOpaque(false);
        pathArea.setBorder(null);
        pathArea.setFont(UIManager.getFont("Label.font"));
        pathArea.setColumns(30);   // bounds the dialog width; text wraps inside it
        pathArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(pathArea);

        JOptionPane.showMessageDialog(this, panel, "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    private static JLabel summaryLine(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void showError(String title, Throwable t) {
        JOptionPane.showMessageDialog(this,
            title + ":\n" + t.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel centered(JComponent component) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(component);
        return wrapper;
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> new Gui().setVisible(true));
    }
}
