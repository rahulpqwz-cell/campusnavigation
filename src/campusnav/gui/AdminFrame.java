package campusnav.gui;

import campusnav.model.Location;
import campusnav.model.Report;
import campusnav.model.User;
import campusnav.dsa.CampusGraph;
import campusnav.dsa.SimpleQueue;
import campusnav.util.Sorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Admin control panel with four tabs: Reports, Paths, Places and Users.
 * Pending reports are handled through a queue (first come, first served).
 */
public class AdminFrame extends BaseFrame implements ActionListener, ListSelectionListener, MapPanel.MapClickListener {

    private CampusGraph graph = Navigation.getGraph();
    private JTabbedPane tabs;
    private JButton backBtn;

    // reports tab
    private DefaultListModel<String> reportModel = new DefaultListModel<String>();
    private JList<String> reportList;
    private JTextArea reportDetail;
    private ArrayList<Report> shownReports = new ArrayList<Report>();
    private JButton processBtn, resolveBtn, deleteReportBtn;

    // paths tab
    private DefaultListModel<String> pathModel = new DefaultListModel<String>();
    private JList<String> pathList;
    private ArrayList<int[]> pathPairs = new ArrayList<int[]>();
    private JButton toggleBtn, checkBtn, addPathBtn;
    private JComboBox<Location> pathA, pathB;
    private JTextField pathDist;
    private JTextArea connResult;

    // places tab
    private JTextField nameField, newDistField;
    private JComboBox<String> catBox;
    private JComboBox<Location> connectBox;
    private javax.swing.JLabel coordLabel;
    private MapPanel pickMap;
    private JButton addPlaceBtn;
    private double pickX = -1;
    private double pickY = -1;

    // users tab
    private DefaultListModel<String> userModel = new DefaultListModel<String>();
    private JList<String> userList;
    private ArrayList<User> shownUsers = new ArrayList<User>();
    private JButton deleteUserBtn;

    public AdminFrame(int startTab) {
        super("Admin Panel");

        tabs = new JTabbedPane();
        tabs.setFont(BOLD_FONT);
        tabs.addTab("Reports", buildReportsTab());
        tabs.addTab("Paths", buildPathsTab());
        tabs.addTab("Places", buildPlacesTab());
        tabs.addTab("Users", buildUsersTab());
        body.add(tabs, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.setOpaque(false);
        backBtn = plainButton("Back to Home");
        backBtn.addActionListener(this);
        south.add(backBtn);
        body.add(south, BorderLayout.SOUTH);

        refreshReports();
        refreshPaths();
        refreshUsers();
        tabs.setSelectedIndex(startTab);
        setStatus("Admin tools. Changes to paths and places are saved automatically.");
    }

    // =====================================================  REPORTS

    private JPanel buildReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        reportList = new JList<String>(reportModel);
        reportList.setFont(BODY_FONT);
        reportList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reportList.setFixedCellHeight(28);
        reportList.addListSelectionListener(this);
        reportDetail = readOnlyArea();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(reportList), new JScrollPane(reportDetail));
        split.setResizeWeight(0.55);
        panel.add(split, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        processBtn = button("Process Next (Queue)");
        resolveBtn = plainButton("Mark Selected Resolved");
        deleteReportBtn = dangerButton("Delete Selected");
        processBtn.addActionListener(this);
        resolveBtn.addActionListener(this);
        deleteReportBtn.addActionListener(this);
        buttons.add(processBtn);
        buttons.add(resolveBtn);
        buttons.add(deleteReportBtn);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshReports() {
        reportModel.clear();
        shownReports = new ArrayList<Report>(Navigation.getReports());
        for (Report r : shownReports) {
            reportModel.addElement(r.toString());
        }
        if (shownReports.isEmpty()) {
            reportDetail.setText("No reports yet.");
        }
        int pending = 0;
        for (Report r : shownReports) {
            if (r.getStatus().equals("Pending")) {
                pending++;
            }
        }
        setStatus(shownReports.size() + " report(s), " + pending + " pending.");
    }

    private void processNextReport() {
        SimpleQueue<Report> queue = new SimpleQueue<Report>(); // first come, first served
        for (Report r : Navigation.getReports()) {
            if (r.getStatus().equals("Pending")) {
                queue.enqueue(r);
            }
        }
        if (queue.isEmpty()) {
            info("There are no pending reports.");
            return;
        }
        Report r = queue.dequeue();
        String oldStatus = r.getStatus();
        r.setStatus("In Progress");
        if (!Navigation.saveReports()) {
            r.setStatus(oldStatus);
            error("The report status could not be saved. Check the data folder and try again.");
            return;
        }
        refreshReports();
        info("Report #" + r.getId() + " is now In Progress.\n"
                + queue.size() + " other pending report(s) remain in the queue.");
    }

    // =====================================================  PATHS

    private JPanel buildPathsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        pathList = new JList<String>(pathModel);
        pathList.setFont(BODY_FONT);
        pathList.setFixedCellHeight(26);
        pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(pathList), BorderLayout.CENTER);

        JPanel side = new JPanel(new GridBagLayout());
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(310, 100));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 4, 4);

        toggleBtn = button("Close / Open Selected Path");
        toggleBtn.addActionListener(this);
        side.add(toggleBtn, c);

        checkBtn = plainButton("Check Connectivity (DFS)");
        checkBtn.addActionListener(this);
        c.gridy++;
        side.add(checkBtn, c);

        connResult = readOnlyArea();
        connResult.setRows(5);
        connResult.setText("Press the button above to check that every place can be reached from the Main Gate.");
        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        side.add(new JScrollPane(connResult), c);

        JPanel add = card();
        add.setLayout(new GridLayout(0, 1, 0, 6));
        add.add(label("Add or update a path (metres)"));
        pathA = new JComboBox<Location>();
        pathB = new JComboBox<Location>();
        ArrayList<Location> sorted = graph.getLocations();
        Sorter.sortByName(sorted);
        for (Location l : sorted) {
            pathA.addItem(l);
            pathB.addItem(l);
        }
        pathA.setFont(BODY_FONT);
        pathB.setFont(BODY_FONT);
        pathDist = new JTextField();
        pathDist.setFont(BODY_FONT);
        pathDist.setToolTipText("Distance in metres");
        addPathBtn = button("Save Path");
        addPathBtn.addActionListener(this);
        add.add(pathA);
        add.add(pathB);
        JPanel distRow = new JPanel(new BorderLayout(6, 0));
        distRow.setOpaque(false);
        distRow.add(pathDist, BorderLayout.CENTER);
        distRow.add(addPathBtn, BorderLayout.EAST);
        add.add(distRow);
        c.gridy++;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        side.add(add, c);

        panel.add(side, BorderLayout.EAST);
        return panel;
    }

    private void refreshPaths() {
        int keep = pathList.getSelectedIndex();
        pathModel.clear();
        pathPairs.clear();
        for (int a = 0; a < graph.size(); a++) {
            for (int b = a + 1; b < graph.size(); b++) {
                if (graph.hasPath(a, b)) {
                    pathPairs.add(new int[] {a, b});
                    String state = graph.isClosed(a, b) ? "   [CLOSED]" : "";
                    pathModel.addElement(graph.get(a).getName() + "  <->  " + graph.get(b).getName()
                            + "   " + (int) graph.getDistance(a, b) + " m" + state);
                }
            }
        }
        if (keep >= 0 && keep < pathModel.size()) {
            pathList.setSelectedIndex(keep);
        }
    }

    private void toggleSelectedPath() {
        int idx = pathList.getSelectedIndex();
        if (idx < 0) {
            error("Select a path from the list first.");
            return;
        }
        int[] p = pathPairs.get(idx);
        graph.setClosed(p[0], p[1], !graph.isClosed(p[0], p[1]));
        Navigation.saveCampus();
        refreshPaths();
        checkConnectivity();
    }

    /** DFS from the Main Gate (place 0): every place that is not visited is cut off. */
    private void checkConnectivity() {
        boolean[] seen = graph.reachableFrom(0);
        StringBuilder sb = new StringBuilder();
        int cut = 0;
        for (int i = 0; i < seen.length; i++) {
            if (!seen[i]) {
                cut++;
                sb.append(" - ").append(graph.get(i).getName()).append("\n");
            }
        }
        if (cut == 0) {
            connResult.setText("All " + seen.length + " places can be reached from the Main Gate.");
        } else {
            connResult.setText("WARNING: " + cut + " place(s) cannot be reached from the Main Gate:\n" + sb);
        }
        connResult.setCaretPosition(0);
    }

    private void savePath() {
        Location a = (Location) pathA.getSelectedItem();
        Location b = (Location) pathB.getSelectedItem();
        if (a.getId() == b.getId()) {
            error("Choose two different places.");
            return;
        }
        double d;
        try {
            d = Double.parseDouble(pathDist.getText().trim());
        } catch (NumberFormatException ex) {
            error("Enter the distance as a number, for example 85.");
            return;
        }
        if (d <= 0) {
            error("The distance must be more than zero.");
            return;
        }
        graph.addPath(a.getId(), b.getId(), d);
        Navigation.saveCampus();
        pathDist.setText("");
        refreshPaths();
        checkConnectivity();
    }

    // =====================================================  PLACES

    private JPanel buildPlacesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = card();
        form.setLayout(new GridBagLayout());
        form.setPreferredSize(new Dimension(300, 100));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 4, 4);

        form.add(label("Name of the new place"), c);
        nameField = new JTextField();
        nameField.setFont(BODY_FONT);
        c.gridy++;
        form.add(nameField, c);

        c.gridy++;
        form.add(label("Category (pick or type a new one)"), c);
        catBox = new JComboBox<String>();
        catBox.setEditable(true);
        catBox.setFont(BODY_FONT);
        for (String cat : graph.getCategories()) {
            catBox.addItem(cat);
        }
        c.gridy++;
        form.add(catBox, c);

        c.gridy++;
        form.add(label("Connect it to"), c);
        connectBox = new JComboBox<Location>();
        connectBox.setFont(BODY_FONT);
        ArrayList<Location> sorted = graph.getLocations();
        Sorter.sortByName(sorted);
        for (Location l : sorted) {
            connectBox.addItem(l);
        }
        c.gridy++;
        form.add(connectBox, c);

        c.gridy++;
        form.add(label("Distance to it (metres)"), c);
        newDistField = new JTextField();
        newDistField.setFont(BODY_FONT);
        c.gridy++;
        form.add(newDistField, c);

        coordLabel = label("Click the map to choose the position  ->");
        c.gridy++;
        form.add(coordLabel, c);

        addPlaceBtn = button("Add Place");
        addPlaceBtn.addActionListener(this);
        c.gridy++;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTH;
        form.add(addPlaceBtn, c);

        pickMap = new MapPanel(graph);
        pickMap.setListener(this);
        pickMap.setPreferredSize(new Dimension(420, 340));

        panel.add(form, BorderLayout.WEST);
        panel.add(pickMap, BorderLayout.CENTER);
        return panel;
    }

    private void addPlace() {
        String name = nameField.getText().trim().replace("|", "/");
        Object catObj = catBox.getSelectedItem();
        String cat = catObj == null ? "" : catObj.toString().trim().replace("|", "/");
        if (name.length() == 0 || cat.length() == 0) {
            error("Enter a name and a category.");
            return;
        }
        if (pickX < 0) {
            error("Click on the map to choose where the new place is.");
            return;
        }
        double d;
        try {
            d = Double.parseDouble(newDistField.getText().trim());
        } catch (NumberFormatException ex) {
            error("Enter the distance as a number, for example 90.");
            return;
        }
        if (d <= 0) {
            error("The distance must be more than zero.");
            return;
        }
        Location to = (Location) connectBox.getSelectedItem();
        try {
            int id = graph.addLocation(name, cat, pickX, pickY);
            graph.addPath(id, to.getId(), d);
        } catch (IllegalStateException ex) {
            error(ex.getMessage());
            return;
        }
        Navigation.saveCampus();
        info("'" + name + "' was added and connected to " + to.getName() + ".");
        Navigation.showAdmin(this, 2); // reopen so every list shows the new place
    }

    // MapPanel.MapClickListener (used by the position picker)
    public void onLocationClicked(int id) {
        coordLabel.setText("That spot is taken by " + graph.get(id).getName() + ".");
    }

    public void onEmptyClicked(double x, double y) {
        pickX = x;
        pickY = y;
        pickMap.setMarker(x, y);
        coordLabel.setText("Position chosen (" + (int) (x * 100) + "%, " + (int) (y * 100) + "%)");
    }

    // =====================================================  USERS

    private JPanel buildUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        userList = new JList<String>(userModel);
        userList.setFont(BODY_FONT);
        userList.setFixedCellHeight(28);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(userList), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        deleteUserBtn = dangerButton("Delete Selected User");
        deleteUserBtn.addActionListener(this);
        buttons.add(deleteUserBtn);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshUsers() {
        userModel.clear();
        shownUsers = Navigation.getUserManager().getAllUsers();
        for (User u : shownUsers) {
            userModel.addElement(u.getUsername() + "   (" + u.getRole() + ")");
        }
    }

    // =====================================================  EVENTS

    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() || e.getSource() != reportList) {
            return;
        }
        int idx = reportList.getSelectedIndex();
        if (idx < 0 || idx >= shownReports.size()) {
            return;
        }
        Report r = shownReports.get(idx);
        reportDetail.setText("Report #" + r.getId() + "\nStatus: " + r.getStatus() + "\nFrom: " + r.getUsername()
                + "\nDate: " + r.getDate() + "\nPlace: " + r.getLocation() + "\nType: " + r.getType()
                + "\n\n" + r.getDescription());
        reportDetail.setCaretPosition(0);
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == backBtn) {
            Navigation.showHome(this);
        } else if (src == processBtn) {
            processNextReport();
        } else if (src == resolveBtn || src == deleteReportBtn) {
            int idx = reportList.getSelectedIndex();
            if (idx < 0 || idx >= shownReports.size()) {
                error("Select a report first.");
                return;
            }
            Report r = shownReports.get(idx);
            if (src == resolveBtn) {
                String oldStatus = r.getStatus();
                r.setStatus("Resolved");
                if (!Navigation.saveReports()) {
                    r.setStatus(oldStatus);
                    error("The report status could not be saved. Check the data folder and try again.");
                    return;
                }
            } else if (confirm("Delete report #" + r.getId() + "?")) {
                Navigation.getReports().remove(r);
                if (!Navigation.saveReports()) {
                    Navigation.getReports().add(r);
                    error("The report could not be deleted from storage. Check the data folder and try again.");
                    return;
                }
            } else {
                return;
            }
            refreshReports();
        } else if (src == toggleBtn) {
            toggleSelectedPath();
        } else if (src == checkBtn) {
            checkConnectivity();
        } else if (src == addPathBtn) {
            savePath();
        } else if (src == addPlaceBtn) {
            addPlace();
        } else if (src == deleteUserBtn) {
            int idx = userList.getSelectedIndex();
            if (idx < 0) {
                error("Select a user first.");
                return;
            }
            User u = shownUsers.get(idx);
            if (u.isAdmin()) {
                error("The admin account cannot be deleted.");
                return;
            }
            if (confirm("Delete user '" + u.getUsername() + "'?")) {
                Navigation.getUserManager().deleteUser(u.getUsername());
                refreshUsers();
            }
        }
    }
}
