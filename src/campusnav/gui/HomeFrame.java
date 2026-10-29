package campusnav.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import campusnav.model.User;

/** Main menu: a grid of big buttons. The text size follows the window size. */
public class HomeFrame extends BaseFrame implements ActionListener {

    private JButton searchBtn, routeBtn, mapBtn, facilitiesBtn, historyBtn;
    private JButton reportBtn, helpBtn, adminBtn, logoutBtn;
    private ArrayList<JButton> tiles = new ArrayList<JButton>();
    private ArrayList<String[]> tileText = new ArrayList<String[]>();

    public HomeFrame() {
        super("Home");

        User user = Navigation.getCurrentUser();
        JLabel hello = new JLabel(user == null ? "Explore CampusNav as a guest"
                : "Hello, " + user.getUsername() + "! Where would you like to go today?");
        hello.setFont(new Font("SansSerif", Font.BOLD, 18));
        hello.setForeground(TEXT);
        body.add(hello, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 3, 14, 14));
        grid.setBackground(BG);

        searchBtn = tile(grid, "Search Places", "Find any building or facility", PRIMARY);
        routeBtn = tile(grid, "Find Route", "Shortest walk between two places", PRIMARY);
        mapBtn = tile(grid, "Campus Map", "See the whole campus", PRIMARY);
        facilitiesBtn = tile(grid, "Nearby Facilities", "Closest library, canteen, ATM...", PRIMARY);
        historyBtn = tile(grid, "Route History", "Your recent routes", PRIMARY);
        reportBtn = tile(grid, "Report Issue", "Blocked path or wrong info", PRIMARY);
        helpBtn = tile(grid, "Help", "How to use the app", PRIMARY);
        if (user != null && user.isFaculty()) {
            adminBtn = tile(grid, "Faculty Management", "Review and resolve issue reports", new Color(142, 68, 173));
        }
        logoutBtn = tile(grid, user == null ? "Sign In" : "Logout",
                user == null ? "Student, Staff or Faculty" : "Sign out of your account",
                user == null ? new Color(46, 125, 50) : new Color(192, 57, 43));

        body.add(grid, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateTileFonts();
            }
        });
        updateTileFonts();
        setStatus("Choose what you want to do.");
    }

    private JButton tile(JPanel grid, String title, String sub, Color color) {
        JButton b = button(title);
        b.setBackground(color);
        b.addActionListener(this);
        grid.add(b);
        tiles.add(b);
        tileText.add(new String[] {title, sub});
        return b;
    }

    /** Re-creates the button texts with a font size that matches the current window width. */
    private void updateTileFonts() {
        int big = Math.max(14, Math.min(24, getWidth() / 46));
        int small = Math.max(11, big - 5);
        for (int i = 0; i < tiles.size(); i++) {
            String[] t = tileText.get(i);
            tiles.get(i).setText("<html><div style='text-align:center'><span style='font-size:" + big
                    + "px'><b>" + t[0] + "</b></span><br><span style='font-size:" + small + "px'>"
                    + t[1] + "</span></div></html>");
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == searchBtn) {
            Navigation.showSearch(this);
        } else if (src == routeBtn) {
            Navigation.showDestination(this);
        } else if (src == mapBtn) {
            Navigation.setFocusId(-1);
            Navigation.showMap(this);
        } else if (src == facilitiesBtn) {
            Navigation.showFacilities(this);
        } else if (src == historyBtn) {
            Navigation.showHistory(this);
        } else if (src == reportBtn) {
            Navigation.showReport(this);
        } else if (src == helpBtn) {
            Navigation.showHelp(this);
        } else if (src == adminBtn) {
            Navigation.showAdmin(this, 0);
        } else if (src == logoutBtn) {
            if (Navigation.getCurrentUser() == null) {
                Navigation.showLogin(this);
            } else {
                Navigation.logout(this);
            }
        }
    }
}
