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

        // Checkbox list, default all selected.
        List<JCheckBox> checkBoxes = new ArrayList<>(sheets.size());
        JPanel checkPanel = new JPanel();
        checkPanel.setOpaque(false);
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        checkPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (String sheet : sheets) {
            JCheckBox cb = new JCheckBox(sheet, true);
            cb.setOpaque(false);
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cb.setForeground(Theme.TEXT_PRIMARY);
            checkBoxes.add(cb);
            checkPanel.add(cb);
        }

        JScrollPane scroll = new JScrollPane(checkPanel);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER));
        scroll.setPreferredSize(new Dimension(380, 260));
        scroll.getViewport().setBackground(Theme.CARD);

        // Action buttons.
        JButton selectAll  = new JButton("Select All");
        JButton selectNone = new JButton("Select None");
        JButton ok         = new JButton("OK");
        JButton cancel     = new JButton("Cancel");

        selectAll.addActionListener(e -> checkBoxes.forEach(cb -> cb.setSelected(true)));
        selectNone.addActionListener(e -> checkBoxes.forEach(cb -> cb.setSelected(false)));

        // Mutable holder to capture OK vs Cancel from the lambdas.
        boolean[] confirmed = {false};
        ok.addActionListener(e -> { confirmed[0] = true; dialog.dispose(); });
        cancel.addActionListener(e -> dialog.dispose());

        // Layout.
        JPanel selectionButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selectionButtons.setOpaque(false);
        selectionButtons.add(selectAll);
        selectionButtons.add(selectNone);

        JPanel okCancelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        okCancelButtons.setOpaque(false);
        okCancelButtons.add(cancel);
        okCancelButtons.add(ok);

        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        root.add(scroll);
        root.add(Box.createVerticalStrut(10));
        root.add(selectionButtons);
        root.add(Box.createVerticalStrut(6));
        root.add(okCancelButtons);

        dialog.add(root);
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
}
