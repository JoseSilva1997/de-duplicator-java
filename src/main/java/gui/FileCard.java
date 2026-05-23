package gui;

import backend.DeduplicationService;

import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.List;

class FileCard extends JPanel {

    private static final int BIG_ICON_SIZE = 40;
    private static final Icon ICON_EMPTY   = FontIcon.of(BootstrapIcons.DOWNLOAD,     BIG_ICON_SIZE, Theme.TEXT_MUTED);
    private static final Icon ICON_HOVER   = FontIcon.of(BootstrapIcons.DOWNLOAD,     BIG_ICON_SIZE, Theme.PRIMARY);
    private static final Icon ICON_LOADED  = FontIcon.of(BootstrapIcons.CHECK_CIRCLE, BIG_ICON_SIZE, Theme.SUCCESS);

    private final JLabel iconLabel;
    private final JLabel mainLabel;     // filename when loaded, hint text when empty
    private final JLabel detailLabel;   // sheets/rows when loaded, "Click to change" hint when loaded
    private final Runnable onChange;
    private final SheetSelector sheetSelector;
    private final DeduplicationService service;

    private File selectedFile;
    private List<String> selectedSheets;
    private int recordCount = -1;   // -1 = unknown / not yet computed
    private String fullFileName = "";   // un-truncated name, for tooltip + re-truncation on resize
    private boolean dragHover = false;
    private boolean mouseHover = false;

    FileCard(String title, String description,
             SheetSelector sheetSelector, DeduplicationService service,
             Runnable onChange) {
        this.onChange = onChange;
        this.sheetSelector = sheetSelector;
        this.service = service;

        // Transparent so paintComponent fully controls the rounded background + dashed border.
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 24, 24));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(Theme.FONT_REGULAR);
        descLabel.setForeground(Theme.TEXT_SECONDARY);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        descLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 18, 0));

        iconLabel = new JLabel(ICON_EMPTY);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        mainLabel = new JLabel("Choose a file or drag it here");
        mainLabel.setFont(Theme.FONT_MEDIUM);
        mainLabel.setForeground(Theme.TEXT_SECONDARY);
        mainLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        detailLabel = new JLabel(" ");
        detailLabel.setFont(Theme.FONT_REGULAR);
        detailLabel.setForeground(Theme.TEXT_MUTED);
        detailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        add(titleLabel);
        add(descLabel);
        add(Box.createVerticalGlue());
        add(iconLabel);
        add(mainLabel);
        add(detailLabel);
        add(Box.createVerticalGlue());

        // Re-truncate the filename whenever the card is resized.
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                updateFilenameDisplay();
            }
        });

        installClickHandler();
        installDropTarget();
    }

    /**
     * Attaches click + hover handlers to the card and every child. Swing doesn't
     * bubble mouse events to parents, so each child needs its own listener. On
     * mouseExited we re-test whether the pointer is still inside the card's
     * bounds, otherwise crossing onto a child would briefly drop the hover state.
     */
    private void installClickHandler() {
        MouseListener handler = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openFileChooser(); }
            @Override public void mouseEntered(MouseEvent e) { setMouseHover(true); }
            @Override public void mouseExited(MouseEvent e) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(p, FileCard.this);
                if (!FileCard.this.contains(p)) setMouseHover(false);
            }
        };
        attachRecursively(this, handler);
    }

    private void setMouseHover(boolean on) {
        if (mouseHover == on) return;
        mouseHover = on;
        repaint();
    }

    private static void attachRecursively(Component c, MouseListener listener) {
        c.addMouseListener(listener);
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                attachRecursively(child, listener);
            }
        }
    }

    /**
     * Accepts files dropped from the OS. We only ever take the first file in the
     * drop list, even if the user drags multiple. dragEnter/dragExit toggle a
     * hover flag that paintComponent uses to highlight the card border.
     */
    private void installDropTarget() {
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetListener() {
            @Override public void dragEnter(DropTargetDragEvent e) {
                if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    e.acceptDrag(DnDConstants.ACTION_COPY);
                    setDragHover(true);
                } else {
                    e.rejectDrag();
                }
            }
            @Override public void dragOver(DropTargetDragEvent e) {}
            @Override public void dropActionChanged(DropTargetDragEvent e) {}
            @Override public void dragExit(DropTargetEvent e) {
                setDragHover(false);
            }
            @Override
            public void drop(DropTargetDropEvent e) {
                setDragHover(false);
                if (!e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    e.rejectDrop();
                    return;
                }
                e.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    Object data = e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (data instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof File f) {
                        if (isAcceptedFile(f)) {
                            acceptFile(f);
                        } else {
                            showUnsupportedFileError(f);
                        }
                    }
                    e.dropComplete(true);
                } catch (Exception ex) {
                    e.dropComplete(false);
                }
            }
        }, true);
    }

    private void setDragHover(boolean on) {
        if (dragHover == on) return;
        dragHover = on;
        if (!hasFile()) {
            iconLabel.setIcon(on ? ICON_HOVER : ICON_EMPTY);
            mainLabel.setForeground(on ? Theme.PRIMARY : Theme.TEXT_SECONDARY);
        }
        repaint();
    }

    private static boolean isAcceptedFile(File f) {
        String n = f.getName().toLowerCase();
        return f.isFile() && (n.endsWith(".xlsx") || n.endsWith(".xls") || n.endsWith(".csv"));
    }

    private void showUnsupportedFileError(File f) {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        String typeDescription = (dot > 0 && dot < name.length() - 1)
            ? "is a " + name.substring(dot) + " file"
            : "is not a supported file type";
        JOptionPane.showMessageDialog(
            this,
            "\"" + name + "\" " + typeDescription + ".\n"
                + "Please drop an Excel or CSV file (.xlsx, .xls, or .csv).",
            "Unsupported file",
            JOptionPane.ERROR_MESSAGE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = Theme.CARD_ARC;

        // Background: tint to NEUTRAL_BG whenever the card is "active" — either
        // mouse-hover (clickable affordance) or drag-hover (drop target affordance).
        Color background = (mouseHover || dragHover) ? Theme.NEUTRAL_BG : Theme.CARD;
        g2.setColor(background);
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        Color borderColor;
        float borderWidth;
        BasicStroke stroke;
        if (dragHover) {
            borderColor = Theme.PRIMARY;
            borderWidth = 1.8f;
            stroke = new BasicStroke(borderWidth);
        } else if (hasFile()) {
            borderColor = Theme.SUCCESS;
            borderWidth = mouseHover ? 1.8f : 1.4f;
            stroke = new BasicStroke(borderWidth);
        } else {
            // Empty state: dashed border signals "drop zone". On hover, darken
            // it so the click affordance reads alongside the background tint.
            borderColor = mouseHover ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED;
            borderWidth = 1.4f;
            stroke = new BasicStroke(
                borderWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                1f, new float[] { 6f, 5f }, 0f);
        }
        g2.setColor(borderColor);
        g2.setStroke(stroke);
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
            acceptFile(new File(directory, filename));
        }
    }

    /** Shared entry point for file selection from either the chooser or a drop. */
    private void acceptFile(File chosen) {
        List<String> sheets = sheetSelector.select(chosen);
        if (sheets == null) return;   // user cancelled the picker — keep current state

        selectedFile = chosen;
        selectedSheets = sheets;
        recordCount = -1;             // unknown until the background read finishes
        fullFileName = chosen.getName();

        iconLabel.setIcon(ICON_LOADED);
        mainLabel.setForeground(Theme.TEXT_PRIMARY);
        mainLabel.setToolTipText(fullFileName);
        updateFilenameDisplay();
        detailLabel.setText(formatSheetSummary(sheets.size(), -1));

        repaint();          // border style reflects selection
        onChange.run();
        countRecordsAsync(chosen, sheets);
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

    /**
     * Truncates the displayed filename to fit the card width. Full name lives in
     * fullFileName + tooltip. Clamps preferred/maximum size so layout can't
     * expand the label beyond the truncated width.
     */
    private void updateFilenameDisplay() {
        if (fullFileName.isEmpty()) {
            // Empty state: mainLabel shows the hint, no clamping needed.
            mainLabel.setPreferredSize(null);
            mainLabel.setMaximumSize(null);
            return;
        }
        int available = computeAvailableTextWidth();
        if (available <= 0) return;   // card not yet sized; ComponentListener will re-call

        FontMetrics fm = mainLabel.getFontMetrics(mainLabel.getFont());
        String displayed = fullFileName;
        if (fm.stringWidth(fullFileName) > available) {
            String ellipsis = "…";
            int target = available - fm.stringWidth(ellipsis);
            int width = 0;
            int len = 0;
            for (int i = 0; i < fullFileName.length(); i++) {
                int cw = fm.charWidth(fullFileName.charAt(i));
                if (width + cw > target) break;
                width += cw;
                len = i + 1;
            }
            displayed = fullFileName.substring(0, len) + ellipsis;
        }
        mainLabel.setText(displayed);

        int actualWidth = fm.stringWidth(displayed);
        Dimension size = new Dimension(actualWidth, fm.getHeight());
        mainLabel.setPreferredSize(size);
        mainLabel.setMaximumSize(size);
        revalidate();
    }

    /** Pixels available for the filename text. The label is centred, so the
     *  whole content width minus padding is fair game. */
    private int computeAvailableTextWidth() {
        Insets insets = getInsets();
        int safety = 8;   // breathing room for font metric quirks
        return getWidth() - insets.left - insets.right - safety;
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
