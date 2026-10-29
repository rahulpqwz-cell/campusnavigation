package campusnav.gui;

import campusnav.dsa.CampusGraph;
import campusnav.model.Location;
import campusnav.model.MapLayer;
import campusnav.model.Route;
import campusnav.util.Sorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/** Main route-planning and step-by-step simulated navigation screen. */
public class MapFrame extends BaseFrame implements ActionListener, ListSelectionListener, MapPanel.MapClickListener {
    private final CampusGraph graph = Navigation.getGraph();
    private final MapPanel map;
    private final DefaultListModel<Location> resultModel = new DefaultListModel<Location>();
    private final JList<Location> resultList = new JList<Location>(resultModel);
    private final ArrayList<Location> allDestinations;
    private final JComboBox<Location> fromBox = new JComboBox<Location>();
    private final JComboBox<Location> toBox = new JComboBox<Location>();
    private final JComboBox<String> viewBox = new JComboBox<String>(new String[] {"Campus view", "Indoor floor"});
    private final JTextField searchField = new JTextField();
    private final JTextArea detail = readOnlyArea();
    private final JTextArea currentInstruction = readOnlyArea();
    private final javax.swing.JLabel progress = label("No route calculated yet.");
    private final JButton useStartBtn = plainButton("Use Selected as Start");
    private final JButton useDestinationBtn = plainButton("Set Selected Destination");
    private final JButton calculateBtn = button("Calculate Route");
    private final JButton startBtn = button("Start Demo Navigation");
    private final JButton previousBtn = plainButton("Previous Step");
    private final JButton nextBtn = button("Next Step");
    private final JButton stopBtn = plainButton("Stop / Reset");
    private final JButton backBtn = plainButton("Back to Home");
    private boolean adjustingView;

    public MapFrame() {
        super("Campus Map & Directions");
        map = new MapPanel(graph);
        map.setListener(this);
        allDestinations = graph.getDestinations();
        Sorter.sortByName(allDestinations);
        for (Location location : allDestinations) {
            fromBox.addItem(location);
            toBox.addItem(location);
        }
        fromBox.setFont(BODY_FONT);
        toBox.setFont(BODY_FONT);
        fromBox.setSelectedIndex(findIndex(0));
        if (Navigation.getDestinationId() >= 0) {
            toBox.setSelectedIndex(findIndex(Navigation.getDestinationId()));
        } else {
            toBox.setSelectedIndex(findRoomIndex("D1"));
        }
        resultList.setFont(BODY_FONT);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.addListSelectionListener(this);
        searchField.setFont(BODY_FONT);
        searchField.setToolTipText("Search a building, facility or room label");
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) { refreshResults(); }
        });
        viewBox.setFont(BODY_FONT);
        viewBox.addActionListener(this);
        JButton[] buttons = {useStartBtn, useDestinationBtn, calculateBtn, startBtn,
                previousBtn, nextBtn, stopBtn, backBtn};
        for (JButton button : buttons) button.addActionListener(this);

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.add(label("Search campus or room"), BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(viewBox, BorderLayout.EAST);
        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setOpaque(false);
        left.add(searchRow, BorderLayout.NORTH);
        left.add(map, BorderLayout.CENTER);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        legend.setOpaque(false);
        legend.add(label("Demo Navigation — Simulated Location — GPS Not Connected"));
        left.add(legend, BorderLayout.SOUTH);

        JPanel routeControls = card();
        routeControls.setLayout(new GridLayout(0, 1, 0, 5));
        routeControls.add(label("Start"));
        routeControls.add(fromBox);
        routeControls.add(label("Destination"));
        routeControls.add(toBox);
        routeControls.add(calculateBtn);
        routeControls.add(progress);
        currentInstruction.setRows(3);
        routeControls.add(new JScrollPane(currentInstruction));
        routeControls.add(startBtn);
        JPanel stepRow = new JPanel(new GridLayout(1, 2, 6, 0));
        stepRow.setOpaque(false);
        stepRow.add(previousBtn);
        stepRow.add(nextBtn);
        routeControls.add(stepRow);
        routeControls.add(stopBtn);

        JPanel selectedPanel = card();
        selectedPanel.setLayout(new BorderLayout(0, 6));
        selectedPanel.add(label("Destination details"), BorderLayout.NORTH);
        selectedPanel.add(new JScrollPane(detail), BorderLayout.CENTER);
        JPanel selectButtons = new JPanel(new GridLayout(1, 2, 6, 0));
        selectButtons.setOpaque(false);
        selectButtons.add(useStartBtn);
        selectButtons.add(useDestinationBtn);
        selectedPanel.add(selectButtons, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(330, 200));
        JPanel results = card();
        results.setLayout(new BorderLayout(0, 6));
        results.add(label("Destinations"), BorderLayout.NORTH);
        results.add(new JScrollPane(resultList), BorderLayout.CENTER);
        JSplitPane sideSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, routeControls,
                new JSplitPane(JSplitPane.VERTICAL_SPLIT, results, selectedPanel));
        sideSplit.setResizeWeight(.52);
        sideSplit.setBorder(null);
        right.add(sideSplit, BorderLayout.CENTER);
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        mainSplit.setResizeWeight(.68);
        mainSplit.setBorder(null);
        body.add(mainSplit, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        bottom.add(backBtn);
        body.add(bottom, BorderLayout.SOUTH);

        refreshResults();
        map.setLayer(Navigation.getCurrentMapLayer());
        if (Navigation.getCurrentRoute() != null) {
            refreshRouteView();
        } else {
            map.setCurrentPosition(findIdByName("Main Gate"));
        }
        setStatus("Configured demo distances are illustrative. Choose a route and advance the steps.");
    }

    private int findIndex(int id) {
        for (int i = 0; i < toBox.getItemCount(); i++) {
            if (toBox.getItemAt(i).getId() == id) return i;
        }
        return 0;
    }

    private int findRoomIndex(String room) {
        for (int i = 0; i < toBox.getItemCount(); i++) {
            if (room.equalsIgnoreCase(toBox.getItemAt(i).getRoom())) return i;
        }
        return 0;
    }

    private int findIdByName(String name) {
        for (Location location : graph.getLocations()) {
            if (name.equalsIgnoreCase(location.getName())) return location.getId();
        }
        return -1;
    }

    private void refreshResults() {
        String query = searchField.getText().trim();
        Location old = resultList.getSelectedValue();
        resultModel.clear();
        for (Location location : allDestinations) {
            if (location.matches(query)) resultModel.addElement(location);
        }
        if (resultModel.size() > 0) {
            int selected = 0;
            if (old != null) {
                for (int i = 0; i < resultModel.size(); i++) {
                    if (resultModel.get(i).getId() == old.getId()) selected = i;
                }
            }
            resultList.setSelectedIndex(selected);
            setStatus(resultModel.size() + " destination(s) match. Locations and route estimates are demonstration data.");
        } else {
            detail.setText("No campus place or room matches that search.");
            setStatus("No destination matches your search.");
        }
    }

    private Location selected() { return resultList.getSelectedValue(); }

    private void showSelectedDetail(Location location) {
        if (location == null) return;
        map.setSelected(location.getId());
        map.setLayer(location.getLayer());
        adjustingView = true;
        viewBox.setSelectedIndex(location.isIndoor() ? 1 : 0);
        adjustingView = false;
        detail.setText(location.getDisplayName() + "\n"
                + "Category: " + location.getCategory() + "\n"
                + (location.getBuilding().length() == 0 ? "" : "Building: " + location.getBuilding() + "\n")
                + (location.getFloor().length() == 0 ? "" : "Level: " + location.getFloor() + "\n")
                + (location.getRoom().length() == 0 ? "" : "Room: " + location.getRoom() + "\n")
                + "Map layer: " + (location.isIndoor() ? "Indoor demonstration floor" : "Outdoor campus") + "\n\n"
                + location.getDescription());
        detail.setCaretPosition(0);
    }

    private void calculateRoute() {
        Location from = (Location) fromBox.getSelectedItem();
        Location to = (Location) toBox.getSelectedItem();
        if (from == null || to == null) {
            error("Choose both a start location and a destination.");
            return;
        }
        Route route = Navigation.findRoute(from.getId(), to.getId(), false);
        if (route == null) {
            error("No configured path connects those places. Choose another destination or check the campus data.");
            return;
        }
        Navigation.stopDemoNavigation();
        Navigation.setRouteStep(0);
        refreshRouteView();
    }

    private void refreshRouteView() {
        Route route = Navigation.getCurrentRoute();
        if (route == null || route.getPath().isEmpty()) {
            progress.setText("No route calculated yet.");
            currentInstruction.setText("");
            map.setHighlightPath(null);
            return;
        }
        ArrayList<Integer> path = route.getPath();
        int step = Math.min(Navigation.getRouteStep(), path.size() - 1);
        int currentId = path.get(step);
        map.setHighlightPath(path);
        map.setDestination(path.get(path.size() - 1));
        map.setCurrentPosition(currentId);
        map.setLayer(graph.get(currentId).getLayer());
        adjustingView = true;
        viewBox.setSelectedIndex(graph.get(currentId).isIndoor() ? 1 : 0);
        adjustingView = false;
        progress.setText("Step " + (step + 1) + " of " + path.size() + "  •  "
                + (int) route.getDistance() + " m demo distance  •  about " + route.getWalkMinutes() + " min");
        currentInstruction.setText(instruction(path, step));
        currentInstruction.setCaretPosition(0);
        setStatus("Demo Navigation — Simulated Location — GPS Not Connected");
    }

    private String instruction(ArrayList<Integer> path, int step) {
        Location current = graph.get(path.get(step));
        if (step == 0) return "Start at " + current.getName() + ".";
        Location previous = graph.get(path.get(step - 1));
        String description = graph.getEdgeDescription(previous.getId(), current.getId());
        StringBuilder text = new StringBuilder();
        text.append(step + 1).append(". ").append(description);
        if (current.isIndoor() && !previous.isIndoor()) {
            text.append("\nIndoor navigation begins at YOU HERE.");
        }
        text.append("\nNext waypoint: ").append(current.getDisplayName());
        text.append(" (illustrative ").append((int) graph.getDistance(previous.getId(), current.getId())).append(" m).");
        if (step == path.size() - 1) text.append("\nYou have arrived at the selected destination.");
        return text.toString();
    }

    public void onLocationClicked(int id) {
        Location location = graph.get(id);
        if (!location.isDestination()) {
            setStatus(location.getName() + " is a route junction, not a selectable destination.");
            return;
        }
        for (int i = 0; i < resultModel.size(); i++) {
            if (resultModel.get(i).getId() == id) {
                resultList.setSelectedIndex(i);
                resultList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    public void onEmptyClicked(double x, double y) { }

    public void valueChanged(ListSelectionEvent event) {
        if (!event.getValueIsAdjusting()) showSelectedDetail(selected());
    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == viewBox && !adjustingView) {
            map.setLayer(viewBox.getSelectedIndex() == 1 ? MapLayer.INDOOR : MapLayer.OUTDOOR);
        } else if (source == useStartBtn || source == useDestinationBtn) {
            Location location = selected();
            if (location == null) {
                error("Select a destination first.");
                return;
            }
            (source == useStartBtn ? fromBox : toBox).setSelectedItem(location);
        } else if (source == calculateBtn) {
            calculateRoute();
        } else if (source == startBtn) {
            if (Navigation.getCurrentRoute() == null) calculateRoute();
            if (Navigation.getCurrentRoute() != null) {
                Navigation.startDemoNavigation();
                refreshRouteView();
            }
        } else if (source == nextBtn) {
            Navigation.advanceRouteStep();
            refreshRouteView();
        } else if (source == previousBtn) {
            Navigation.previousRouteStep();
            refreshRouteView();
        } else if (source == stopBtn) {
            Navigation.stopDemoNavigation();
            refreshRouteView();
            if (Navigation.getCurrentRoute() == null) map.setCurrentPosition(findIdByName("Main Gate"));
        } else if (source == backBtn) {
            Navigation.showHome(this);
        }
    }
}
