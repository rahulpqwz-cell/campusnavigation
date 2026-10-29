package campusnav.gui;

import campusnav.model.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

/**
 * Parent class of every screen. It gives all frames the same header, body, status bar,
 * colours and helper methods, so each frame only has to add its own components.
 * Frames are resizable and use layout managers (no fixed positions), so they stay responsive.
 */
public abstract class BaseFrame extends JFrame {

    public static final Color PRIMARY = new Color(21, 71, 140);
    public static final Color ACCENT = new Color(255, 152, 0);
    public static final Color BG = new Color(241, 245, 251);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(30, 40, 55);
    public static final Color DANGER = new Color(192, 57, 43);
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 22);
    public static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font BOLD_FONT = new Font("SansSerif", Font.BOLD, 14);

    /** Put your components in this panel (it uses BorderLayout). */
    protected JPanel body;
    private JLabel statusBar;

    protected BaseFrame(String title) {
        super("Campus Navigation - " + title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setMinimumSize(new Dimension(780, 600));
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);
        User u = Navigation.getCurrentUser();
        if (u != null) {
            JLabel who = new JLabel(u.getUsername() + " (" + u.getRole() + ")");
            who.setFont(BODY_FONT);
            who.setForeground(new Color(255, 224, 160));
            header.add(who, BorderLayout.EAST);
        }
        getContentPane().add(header, BorderLayout.NORTH);

        body = new JPanel(new BorderLayout(12, 12));
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(16, 20, 16, 20));
        getContentPane().add(body, BorderLayout.CENTER);

        statusBar = new JLabel(" Campus Navigation");
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusBar.setForeground(Color.DARK_GRAY);
        statusBar.setBorder(new EmptyBorder(5, 20, 5, 20));
        statusBar.setOpaque(true);
        statusBar.setBackground(new Color(222, 229, 240));
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    // ---------- helper methods shared by all frames ----------

    protected JButton button(String text) {
        JButton b = new JButton(text);
        b.setFont(BOLD_FONT);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    protected JButton plainButton(String text) {
        JButton b = button(text);
        b.setBackground(new Color(218, 227, 241));
        b.setForeground(PRIMARY);
        return b;
    }

    protected JButton dangerButton(String text) {
        JButton b = button(text);
        b.setBackground(DANGER);
        return b;
    }

    protected JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BODY_FONT);
        l.setForeground(TEXT);
        return l;
    }

    protected JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 215, 230)),
                new EmptyBorder(14, 16, 14, 16)));
        return p;
    }

    protected JTextArea readOnlyArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(BODY_FONT);
        a.setMargin(new java.awt.Insets(8, 10, 8, 10));
        return a;
    }

    protected void setStatus(String text) {
        statusBar.setText(" " + text);
    }

    protected void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    protected void info(String message) {
        JOptionPane.showMessageDialog(this, message, "Campus Navigation", JOptionPane.INFORMATION_MESSAGE);
    }

    protected boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
