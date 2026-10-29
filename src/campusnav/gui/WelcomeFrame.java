package campusnav.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Welcome screen. Visitors may explore without signing in. */
public class WelcomeFrame extends BaseFrame implements ActionListener {
    private JButton guestBtn;
    private JButton searchBtn;
    private JButton mapBtn;
    private JButton loginBtn;
    private JButton exitBtn;
    private JLabel bigTitle;

    public WelcomeFrame() {
        super("Welcome");
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(8, 8, 8, 8);

        bigTitle = new JLabel("CampusNav", SwingConstants.CENTER);
        bigTitle.setForeground(PRIMARY);
        center.add(bigTitle, c);

        JLabel subtitle = new JLabel("Explore campus places and follow simulated outdoor-to-indoor routes.",
                SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitle.setForeground(TEXT);
        c.gridy++;
        center.add(subtitle, c);

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        guestBtn = button("Explore as Guest");
        guestBtn.setBackground(ACCENT);
        guestBtn.setForeground(Color.BLACK);
        searchBtn = plainButton("Search Campus");
        mapBtn = plainButton("View Map");
        loginBtn = button("Sign In for Reporting Issues");
        exitBtn = plainButton("Exit");
        JButton[] buttons = {guestBtn, searchBtn, mapBtn, loginBtn, exitBtn};
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setPreferredSize(new Dimension(290, 42));
            buttons[i].addActionListener(this);
            GridBagConstraints row = new GridBagConstraints();
            row.gridx = 0;
            row.gridy = i;
            row.insets = new Insets(5, 8, 5, 8);
            actions.add(buttons[i], row);
        }
        c.gridy++;
        c.insets = new Insets(24, 8, 8, 8);
        center.add(actions, c);
        body.add(center, BorderLayout.CENTER);
        setStatus("Demo map and distances are illustrative. Simulated location only; GPS is not connected.");

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int size = Math.max(32, Math.min(64, getWidth() / 15));
                bigTitle.setFont(new Font("SansSerif", Font.BOLD, size));
            }
        });
        bigTitle.setFont(new Font("SansSerif", Font.BOLD, 48));
    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == guestBtn) {
            Navigation.continueAsGuest();
            Navigation.showHome(this);
        } else if (source == searchBtn) {
            Navigation.continueAsGuest();
            Navigation.showSearch(this);
        } else if (source == mapBtn) {
            Navigation.continueAsGuest();
            Navigation.setFocusId(-1);
            Navigation.showMap(this);
        } else if (source == loginBtn) {
            Navigation.showLogin(this);
        } else if (source == exitBtn) {
            System.exit(0);
        }
    }
}
