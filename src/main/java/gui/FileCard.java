package gui;

import javax.swing.*;
import java.awt.*;
import java.io.File;

class FileCard extends JPanel {

    private final JLabel statusLabel;
    private final Runnable onChange;
    private File selectedFile;

    FileCard(String title, String description, Runnable onChange) {
        this.onChange = onChange;

        setBackground(Theme.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(20, 22, 22, 22)));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Theme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setForeground(Theme.TEXT_SECONDARY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 12, 0));

        statusLabel = new JLabel("No file selected");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(Theme.DANGER);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JButton chooseButton = new JButton("Choose File");
        chooseButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        chooseButton.setBackground(Theme.PRIMARY);
        chooseButton.setForeground(Color.WHITE);
        chooseButton.setFocusPainted(false);
        chooseButton.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        chooseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chooseButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        chooseButton.addActionListener(e -> openFileChooser());

        add(titleLabel);
        add(descLabel);
        add(statusLabel);
        add(chooseButton);
    }

    private void openFileChooser() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        Frame parent = owner instanceof Frame ? (Frame) owner : null;
        FileDialog dialog = new FileDialog(parent, "Select Excel file", FileDialog.LOAD);
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".xlsx") || lower.endsWith(".xls");
        });
        dialog.setFile("*.xlsx;*.xls");
        dialog.setVisible(true);

        String filename = dialog.getFile();
        String directory = dialog.getDirectory();
        if (filename != null && directory != null) {
            selectedFile = new File(directory, filename);
            statusLabel.setText(selectedFile.getName());
            statusLabel.setForeground(Theme.SUCCESS);
            onChange.run();
        }
    }

    boolean hasFile() {
        return selectedFile != null;
    }

    File getFile() {
        return selectedFile;
    }
}

