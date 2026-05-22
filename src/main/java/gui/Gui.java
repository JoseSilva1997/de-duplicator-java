package gui;

import javax.swing.*;

import backend.DeduplicationService;

import java.awt.*;
import java.util.List;
import java.io.IOException;
import java.nio.file.Path;

public class Gui extends JFrame {

    private final FileCard guestCard;
    private final FileCard attendeeCard;
    private final JButton processButton;
    private final JLabel statusLabel;

    private final DeduplicationService service = new DeduplicationService();

    public Gui() {
        super("Excel Sheet Comparator");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 700);
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
            "Guest List",
            "Upload your complete guest list",
            picker,
            this::updateProcessButton);
        attendeeCard = new FileCard(
            "Attendee List",
            "Upload your confirmed attendees list",
            picker,
            this::updateProcessButton);


        processButton = buildProcessButton();
        statusLabel = buildStatusLabel("Upload both files to begin processing");

        layoutComponents();
        updateProcessButton();
    }

    private void layoutComponents() {
        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        root.add(buildHeader());
        root.add(Box.createVerticalStrut(25));
        root.add(buildCardsRow());
        root.add(Box.createVerticalStrut(35));
        root.add(centered(processButton));
        root.add(Box.createVerticalStrut(15));
        root.add(centered(statusLabel));

        add(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Excel Sheet Comparator");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Remove duplicate attendees from your guest list");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(Theme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        header.add(title);
        header.add(subtitle);
        return header;
    }

    private JPanel buildCardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 20, 0));
        row.setOpaque(false);
        row.add(guestCard);
        row.add(attendeeCard);
        return row;
    }

    private JButton buildProcessButton() {
        JButton button = new JButton("Remove Duplicates");
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(Theme.PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(14, 50, 14, 50));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> onRemoveDuplicates());
        return button;
    }

    private JLabel buildStatusLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(Theme.TEXT_SECONDARY);
        return label;
    }

    private void updateProcessButton() {
        boolean ready = guestCard.hasFile() && attendeeCard.hasFile();
        processButton.setEnabled(ready);
        processButton.setBackground(ready ? Theme.PRIMARY : Theme.PRIMARY_DISABLED);
        statusLabel.setText(ready
                ? "Ready to process"
                : "Upload both files to begin processing");
    }

    private void onRemoveDuplicates() {
        Path primaryPath   = guestCard.getFile().toPath();
        Path secondaryPath = attendeeCard.getFile().toPath();
        List<String> primarySheets   = guestCard.getSelectedSheets();
        List<String> secondarySheets = attendeeCard.getSelectedSheets();

        processButton.setEnabled(false);
        statusLabel.setText("Processing…");

        SwingWorker<DeduplicationService.Summary, Void> worker = new SwingWorker<>() {
            @Override
            protected DeduplicationService.Summary doInBackground() throws Exception {
                return service.run(primaryPath, primarySheets, secondaryPath, secondarySheets);
            }
            @Override
            protected void done() {
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
        JOptionPane.showMessageDialog(this,
            "Processed " + s.sheetsProcessed() + " sheet(s)\n" +
            "Kept:    " + s.totalKept() + "\n" +
            "Removed: " + s.totalRemoved() + "\n\n" +
            "Output saved to:\n" + s.outputDirectory(),
            "Done",
            JOptionPane.INFORMATION_MESSAGE);
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

