package gui;

import backend.DeduplicationService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

class FileCard extends JPanel {

    private final JLabel statusIcon;
    private final JLabel statusLabel;
    private final JLabel detailLabel;
    private final JButton chooseButton;
    private final Runnable onChange;
    private final SheetSelector sheetSelector;
    private final DeduplicationService service;

    private File selectedFile;
    private List<String> selectedSheets;
    private int recordCount = -1;   // -1 = unknown / not yet computed

    FileCard(String title, String description,
             SheetSelector sheetSelector, DeduplicationService service,
             Runnable onChange) {
        this.onChange = onChange;
        this.sheetSelector = sheetSelector;
        this.service = service;

        // Transparent so paintComponent fully controls the rounded background.
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 24, 24));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(Theme.FONT_REGULAR);
        descLabel.setForeground(Theme.TEXT_SECONDARY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 18, 0));

        // Status pill: icon + label horizontal row.
        statusIcon = new JLabel("○");
        statusIcon.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 14));
        statusIcon.setForeground(Theme.TEXT_MUTED);

        statusLabel = new JLabel("No file selected");
        statusLabel.setFont(Theme.FONT_MEDIUM);
        statusLabel.setForeground(Theme.TEXT_SECONDARY);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusRow.setOpaque(false);
        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusRow.add(statusIcon);
        statusRow.add(statusLabel);

        // Secondary detail line under the status — sheet count + row count after selection.
        detailLabel = new JLabel(" ");
        detailLabel.setFont(Theme.FONT_REGULAR);
        detailLabel.setForeground(Theme.TEXT_MUTED);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailLabel.setBorder(BorderFactory.createEmptyBorder(4, 26, 14, 0));

        chooseButton = new JButton("Choose file");
        chooseButton.setFont(Theme.FONT_BOLD);
        chooseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chooseButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        chooseButton.setBackground(Theme.PRIMARY);
        chooseButton.setForeground(Color.WHITE);
        chooseButton.setBorder(BorderFactory.createEmptyBorder(12, 26, 12, 26));
        chooseButton.addActionListener(e -> openFileChooser());

        add(titleLabel);
        add(descLabel);
        add(statusRow);
        add(detailLabel);
        add(Box.createVerticalGlue());
        add(chooseButton);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = Theme.CARD_ARC;

        g2.setColor(Theme.CARD);
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        g2.setColor(hasFile() ? Theme.SUCCESS : Theme.CARD_BORDER);
        g2.setStroke(new BasicStroke(hasFile() ? 1.4f : 1f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }

    private void openFileChooser() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        Frame parent = owner instanceof Frame ? (Frame) owner : null;
        FileDialog dialog = new FileDialog(parent, "Select spreadsheet", FileDialog.LOAD);
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv");
        });
        dialog.setFile("*.xlsx;*.xls;*.csv");
        dialog.setVisible(true);

        String filename = dialog.getFile();
        String directory = dialog.getDirectory();
        if (filename != null && directory != null) {
            File chosen = new File(directory, filename);
            List<String> sheets = sheetSelector.select(chosen);
            if (sheets == null) return;   // user cancelled the picker — keep current state

            selectedFile = chosen;
            selectedSheets = sheets;
            recordCount = -1;             // unknown until the background read finishes

            statusIcon.setText("✓");
            statusIcon.setForeground(Theme.SUCCESS);
            statusLabel.setText(chosen.getName());
            statusLabel.setForeground(Theme.TEXT_PRIMARY);
            detailLabel.setText(formatSheetSummary(sheets.size(), -1));
            chooseButton.setText("Change file");

            repaint();          // border colour reflects selection
            onChange.run();
            countRecordsAsync(chosen, sheets);
        }
    }

    /** Reads the selected sheets off the EDT to get the record count, then updates the UI. */
    private void countRecordsAsync(File file, List<String> sheets) {
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return service.countRecords(file.toPath(), sheets);
            }
            @Override
            protected void done() {
                // Guard against stale callbacks if the user picks a different file
                // before this one finishes counting.
                if (!file.equals(selectedFile)) return;
                try {
                    recordCount = get();
                    detailLabel.setText(formatSheetSummary(sheets.size(), recordCount));
                } catch (Exception ex) {
                    recordCount = -1;   // leave just the sheet count visible
                } finally {
                    onChange.run();     // tell Gui to refresh the global status line
                }
            }
        };
        worker.execute();
    }

    private static String formatSheetSummary(int sheetCount, int rowCount) {
        String sheetPart = sheetCount == 1 ? "1 sheet" : sheetCount + " sheets";
        if (rowCount < 0) return sheetPart + " selected";
        String rowPart = rowCount == 1 ? "1 row" : rowCount + " rows";
        return sheetPart + " · " + rowPart;
    }

    List<String> getSelectedSheets() {
        return selectedSheets;
    }

    /** Returns the record count if known, or -1 if not yet computed. */
    int getRecordCount() {
        return recordCount;
    }

    boolean hasFile() {
        return selectedFile != null;
    }

    File getFile() {
        return selectedFile;
    }
}
