package gui;

import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Overlay side panel that slides in from the right edge of the window.
 * Hosts tabs for Settings and How it works. Lives on a JLayeredPane so it
 * floats above the main content without reflowing it.
 *
 * Comes paired with a scrim ({@link #getScrim()}) that the parent must add
 * to the same layered pane, below the panel itself. The scrim darkens the
 * main UI while the panel is open and closes the panel when clicked.
 */
public final class SidePanel extends JPanel {

    static final int WIDTH = 340;
    private static final int ANIMATION_MS = 180;
    private static final int FRAME_MS = 12;
    private static final float SCRIM_ALPHA = 0.40f;

    private final Scrim scrim = new Scrim();
    private final JCheckBox dropNoEmailCheckbox = buildSettingCheckbox(
        "Remove guests with no email address", true);
    private boolean open = false;
    private Timer animator;

    public SidePanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.CARD_BORDER));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);

        scrim.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { setOpen(false); }
        });
    }

    public JComponent getScrim() {
        return scrim;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 12));

        JLabel title = new JLabel("Settings");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JButton close = new JButton(FontIcon.of(BootstrapIcons.X, 18, Theme.TEXT_SECONDARY));
        close.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        close.setContentAreaFilled(false);
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setToolTipText("Close");
        close.addActionListener(e -> setOpen(false));
        header.add(close, BorderLayout.EAST);

        return header;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_MEDIUM);
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        tabs.addTab("Settings", buildSettingsTab());
        tabs.addTab("How it works", buildHowItWorksTab());
        return tabs;
    }

    private JComponent buildSettingsTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));

        panel.add(sectionLabel("Extra filtering"));
        panel.add(Box.createVerticalStrut(6));
        panel.add(dropNoEmailCheckbox);

        panel.add(Box.createVerticalGlue());
        return wrapScrollable(panel);
    }

    public boolean isDropRowsWithoutEmailEnabled() {
        return dropNoEmailCheckbox.isSelected();
    }

    private JComponent buildHowItWorksTab() {
        // JEditorPane in HTML mode wraps text only when its view width is bounded.
        // Overriding getScrollableTracksViewportWidth to true forces the editor
        // to match the viewport width instead of growing horizontally with the
        // longest line, which is what triggers a horizontal scrollbar.
        JEditorPane content = new JEditorPane("text/html", howItWorksHtml()) {
            @Override public boolean getScrollableTracksViewportWidth() { return true; }
        };
        content.setEditable(false);
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
        content.setFont(Theme.FONT_REGULAR);

        return wrapScrollable(content);
    }

    private static String howItWorksHtml() {
        String bodyStyle = "font-family:'" + Theme.FONT_FAMILY
            + "'; font-size:12pt; color:#0F172A;";
        String h = "color:#0F172A; font-size:13pt; margin:14px 0 4px 0;";
        String p = "color:#334155; margin:4px 0 10px 0; line-height:1.45;";
        String li = "color:#334155; margin:2px 0; line-height:1.4;";

        return "<html><body style=\"" + bodyStyle + "\">"
            + "<h3 style=\"" + h + "\">What it does</h3>"
            + "<p style=\"" + p + "\">Compares a guest list against a list of confirmed "
            + "attendees and produces a cleaned guest list with the duplicates removed. "
            + "Accepts <b>.xlsx</b>, <b>.xls</b>, and <b>.csv</b> files.</p>"

            + "<h3 style=\"" + h + "\">Matching pipeline</h3>"
            + "<p style=\"" + p + "\">Each guest is checked against the attendees using "
            + "five strategies in order. A guest is removed as soon as one strategy finds a match.</p>"
            + "<ol style=\"margin:0 0 10px 18px; padding:0;\">"
            + "<li style=\"" + li + "\"><b>Exact email</b> (confidence 100): same email address.</li>"
            + "<li style=\"" + li + "\"><b>Email username</b> (90): the part before the @ matches.</li>"
            + "<li style=\"" + li + "\"><b>Name and company</b> (90): first name, last name, and company all match exactly.</li>"
            + "<li style=\"" + li + "\"><b>Fuzzy key</b> (85): first name, last name, and company match approximately.</li>"
            + "<li style=\"" + li + "\"><b>Fuzzy name</b> (80+): names are similar enough to match, even without a company.</li>"
            + "</ol>"

            + "<h3 style=\"" + h + "\">Output</h3>"
            + "<p style=\"" + p + "\">Two Excel files are written to your desktop:</p>"
            + "<ul style=\"margin:0 0 10px 18px; padding:0;\">"
            + "<li style=\"" + li + "\"><b>Updated guests list</b>: the cleaned guest list.</li>"
            + "<li style=\"" + li + "\"><b>People removed</b>: every removed guest with the reason and confidence score for each match. Skipped entirely if no one was removed.</li>"
            + "</ul>"
            + "<p style=\"" + p + "\">If you selected multiple sheets from a workbook, each sheet is deduplicated independently against the full pool of attendees, and the output files preserve that structure: one sheet in the output per sheet in the input. Sheets with no removals are omitted from the <b>People removed</b> file.</p>"
            + "<p style=\"" + p + "\">CSV inputs are treated as a single sheet named after the file.</p>"
            + "</body></html>";
    }

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_BOLD);
        label.setForeground(Theme.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JCheckBox buildSettingCheckbox(String text, boolean selected) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setOpaque(false);
        box.setFont(Theme.FONT_REGULAR);
        box.setForeground(Theme.TEXT_PRIMARY);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        return box;
    }

    private static JScrollPane wrapScrollable(JComponent inner) {
        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * Animates the panel into or out of view. The scrim fades alongside it.
     * Width is fixed; the animator only adjusts x.
     */
    public void setOpen(boolean shouldOpen) {
        if (open == shouldOpen && animator == null) return;
        open = shouldOpen;
        if (open) scrim.setVisible(true);
        startAnimation();
    }

    private void startAnimation() {
        if (animator != null && animator.isRunning()) animator.stop();
        if (!isVisible()) setVisible(true);

        Container parent = getParent();
        if (parent == null) return;

        final int parentW = parent.getWidth();
        final int hiddenX = parentW;
        final int shownX  = parentW - WIDTH;
        final int fromX   = getX();
        final int toX     = open ? shownX : hiddenX;
        final float fromA = scrim.getAlpha();
        final float toA   = open ? SCRIM_ALPHA : 0f;

        long start = System.currentTimeMillis();
        animator = new Timer(FRAME_MS, null);
        animator.addActionListener(e -> {
            float t = Math.min(1f, (System.currentTimeMillis() - start) / (float) ANIMATION_MS);
            float eased = easeOutCubic(t);
            int x = Math.round(fromX + (toX - fromX) * eased);
            setLocation(x, getY());
            scrim.setAlpha(fromA + (toA - fromA) * eased);
            if (t >= 1f) {
                animator.stop();
                animator = null;
                if (!open) {
                    setVisible(false);
                    scrim.setVisible(false);
                }
            }
        });
        animator.start();
    }

    private static float easeOutCubic(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    /**
     * Called by the parent layered pane when it resizes. Keeps the panel pinned
     * to the right edge (open) or just off-screen to the right (closed), and
     * sizes the scrim to cover the full pane.
     */
    public void resyncBounds(int parentWidth, int parentHeight) {
        int x = open ? parentWidth - WIDTH : parentWidth;
        setBounds(x, 0, WIDTH, parentHeight);
        scrim.setBounds(0, 0, parentWidth, parentHeight);
    }

    /**
     * Translucent black overlay that darkens the main UI while the panel is open.
     * Click anywhere on it to dismiss the panel.
     */
    private static final class Scrim extends JComponent {
        private float alpha = 0f;

        Scrim() {
            setOpaque(false);
            setVisible(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        float getAlpha() { return alpha; }

        void setAlpha(float a) {
            alpha = Math.max(0f, Math.min(1f, a));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (alpha <= 0f) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
