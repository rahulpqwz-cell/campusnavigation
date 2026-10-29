package campusnav.gui;

import campusnav.dsa.CampusGraph;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

/** NEW FRAME 4 - help, map legend and about page. */
public class HelpFrame extends BaseFrame implements ActionListener {

    private JButton backBtn;

    public HelpFrame() {
        super("Help");

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(BOLD_FONT);
        tabs.addTab("Getting Started", textTab(
                "Welcome to CampusNav. Guests can search, view the map and plan routes without an account.\n\n"
                + "1. Search by a building, facility or room label such as D1.\n"
                + "2. Choose a start and destination on the Map or Find Route screen.\n"
                + "3. Start Demo Navigation and use Next Step / Previous Step to follow the calculated route.\n\n"
                + "A route to a D-number room enters the Computer Science Dept demonstration floor and continues along its configured corridors."));
        tabs.addTab("Routes", textTab(
                "Two ways to find a route:\n\n"
                + "Shortest distance  -  uses Dijkstra's algorithm and gives the path with the fewest metres to walk.\n\n"
                + "Fewest stops  -  uses Breadth First Search and gives the path that passes through the fewest places.\n\n"
                + "Closed paths (marked with red dashed lines) are never used. If every path to a place is closed, "
                + "the app tells you that no route exists.\n\n"
                + "Distance and walking time are illustrative values from the configurable campus dataset, not measured directions. The blue marker is simulated; GPS is not connected."));
        tabs.addTab("Features", textTab(
                "Campus Map  -  click any place to see its details, or use Reachable Places to check which "
                + "locations can still be reached.\n\n"
                + "Nearby Facilities  -  choose where you are and what you need (library, canteen, medical ...). "
                + "The closest ones are listed first.\n\n"
                + "Route History  -  your recent routes. Undo Last removes the newest one.\n\n"
                + "Report Issue  -  Student and Staff accounts can submit a facility report. Guests are sent to Sign In first.\n\n"
                + "Faculty Management  -  Faculty accounts review saved reports and update their status.\n\n"
                + "The indoor plan is a schematic fallback. Room labels do not identify room functions."));
        tabs.addTab("Map Legend", legendTab());
        tabs.addTab("About", textTab(
                "Campus Navigation\n"
                + "An offline Java Swing prototype.\n\n"
                + "Data structures used: custom stack, queue, adjacency-list graph, "
                + "insertion sort, binary search, hashing (HashMap).\n\n"
                + "Algorithms: Dijkstra, Breadth First Search and Depth First Search.\n\n"
                + "Accounts, reports, routes and graph edits are stored as local text files in the data folder. No GPS, online map or server is used."));

        body.add(tabs, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.setOpaque(false);
        backBtn = plainButton("Back");
        backBtn.addActionListener(this);
        south.add(backBtn);
        body.add(south, BorderLayout.SOUTH);

        setStatus("Need more help? Ask the admin through Report Issue.");
    }

    private JScrollPane textTab(String text) {
        JTextArea area = readOnlyArea();
        area.setFont(BODY_FONT.deriveFont(15f));
        area.setText(text);
        area.setCaretPosition(0);
        return new JScrollPane(area);
    }

    private JPanel legendTab() {
        CampusGraph g = Navigation.getGraph();
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(CARD);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.add(label("Place colours on the map:"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 10));
        grid.setBackground(CARD);
        for (String cat : g.getCategories()) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            row.setBackground(CARD);
            JPanel dot = new JPanel();
            dot.setBackground(MapPanel.categoryColor(cat));
            dot.setPreferredSize(new Dimension(18, 18));
            row.add(dot);
            row.add(label(cat));
            grid.add(row);
        }
        panel.add(grid, BorderLayout.CENTER);

        JLabel lines = new JLabel("<html>Grey line = open path &nbsp;&nbsp; Red dashed line = closed path"
                + " &nbsp;&nbsp; Orange line = your route</html>");
        lines.setFont(BODY_FONT);
        lines.setForeground(new Color(60, 60, 60));
        panel.add(lines, BorderLayout.SOUTH);
        return panel;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == backBtn) {
            Navigation.showHome(this);
        }
    }
}
