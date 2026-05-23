package gui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class SheetPickerDialog {

    private SheetPickerDialog() {}

    public static List<String> show(Window owner, String title, List<String> sheets) {
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().setBackground(Theme.BACKGROUND);

        JLabel heading = new JLabel("Choose which sheets to include");
        heading.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 15));
        heading.setForeground(Theme.TEXT_PRIMARY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("All sheets are selected by default.");
        hint.setFont(Theme.FONT_REGULAR);
        hint.setForeground(Theme.TEXT_SECONDARY);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setBorder(BorderFactory.createEmptyBorder(2, 0, 12, 0));

        List<JCheckBox> checkBoxes = new ArrayList<>(sheets.size());
        JPanel checkPanel = new JPanel();
        checkPanel.setBackground(Theme.CARD);
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        checkPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        for (String sheet : sheets) {
            JCheckBox cb = new JCheckBox(sheet, true);
            cb.setOpaque(false);
            cb.setFont(Theme.FONT_REGULAR);
            cb.setForeground(Theme.TEXT_PRIMARY);
            cb.setFocusPainted(false);
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            cb.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            checkBoxes.add(cb);
            checkPanel.add(cb);
        }

        JScrollPane scroll = new JScrollPane(checkPanel);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER, 1, true));
        scroll.setPreferredSize(new Dimension(420, 280));
        scroll.getViewport().setBackground(Theme.CARD);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton selectAll  = ghostButton("Select all");
        JButton selectNone = ghostButton("Select none");
        JButton ok         = primaryButton("OK");
        JButton cancel     = ghostButton("Cancel");

        selectAll.addActionListener(e -> checkBoxes.forEach(cb -> cb.setSelected(true)));
        selectNone.addActionListener(e -> checkBoxes.forEach(cb -> cb.setSelected(false)));

        // Mutable holder to capture OK vs Cancel from the lambdas.
        boolean[] confirmed = {false};
        ok.addActionListener(e -> { confirmed[0] = true; dialog.dispose(); });
        cancel.addActionListener(e -> dialog.dispose());

        JPanel selectionButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        selectionButtons.setOpaque(false);
        selectionButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectionButtons.add(selectAll);
        selectionButtons.add(selectNone);

        JPanel okCancelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        okCancelButtons.setOpaque(false);
        okCancelButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        okCancelButtons.add(cancel);
        okCancelButtons.add(ok);

        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        buttonRow.add(selectionButtons, BorderLayout.WEST);
        buttonRow.add(okCancelButtons, BorderLayout.EAST);

        JPanel root = new JPanel();
        root.setBackground(Theme.BACKGROUND);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));
        root.add(heading);
        root.add(hint);
        root.add(scroll);
        root.add(buttonRow);

        dialog.add(root);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);   // blocks until dispose()

        if (!confirmed[0]) return null;

        List<String> selected = new ArrayList<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) selected.add(sheets.get(i));
        }
        return selected.isEmpty() ? null : selected;
    }

    private static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.FONT_BOLD);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // FlatLaf.style: setBorder otherwise forces a plain-painting fallback
        // that ignores JButton.arc. Same trick used on the main process button.
        b.putClientProperty("FlatLaf.style",
            "arc: 16; margin: 8,22,8,22; foreground: #FFFFFF;");
        b.setBackground(Theme.PRIMARY);
        b.setForeground(Color.WHITE);
        return b;
    }

    private static JButton ghostButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.FONT_MEDIUM);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("FlatLaf.style", "arc: 16; margin: 8,16,8,16;");
        b.setBackground(Theme.NEUTRAL_BG);
        b.setForeground(Theme.TEXT_PRIMARY);
        return b;
    }
}
