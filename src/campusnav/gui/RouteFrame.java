package campusnav.gui;

import campusnav.dsa.CampusGraph;
import campusnav.model.Location;
import campusnav.model.Route;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

/** Calculated route, generated instructions and controls for continuous simulated movement. */
public class RouteFrame extends BaseFrame implements ActionListener {
    private final CampusGraph graph = Navigation.getGraph();
    private final JLabel summary = label("");
    private final JLabel progress = label("");
    private final JTextArea instructions = readOnlyArea();
    private final MapPanel map;
    private final JButton startBtn = button("Start Demo Navigation");
    private final JButton previousBtn = plainButton("Previous Step");
    private final JButton nextBtn = button("Next Step");
    private final JButton stopBtn = plainButton("Stop / Reset");
    private final JButton mapBtn = plainButton("Open Map");
    private final JButton anotherBtn = plainButton("Plan Another Route");
    private final JButton reportBtn = plainButton("Report Issue");
    private final JButton homeBtn = plainButton("Home");

    public RouteFrame() {
        super("Your Route");
        Route route = Navigation.getCurrentRoute();
        if (route == null || route.getPath().isEmpty()) {
            instructions.setText("No route is available. Return to the map to choose a destination.");
            map = new MapPanel(graph);
        } else {
            ArrayList<Integer> path = route.getPath();
            Location from = graph.get(path.get(0));
            Location to = graph.get(path.get(path.size() - 1));
            summary.setText("<html><b>" + escape(from.getDisplayName()) + "</b> to <b>"
                    + escape(to.getDisplayName()) + "</b><br>Illustrative configured distance: <b>"
                    + (int) route.getDistance() + " m</b><br>Estimated demo walking time: about <b>"
                    + route.getWalkMinutes() + " min</b><br>Route waypoints: " + path.size()
                    + "<br>Method: " + escape(route.getMode()) + "</html>");
            map = new MapPanel(graph);
            map.setHighlightPath(path);
            instructions.setText(buildSteps(path));
        }
        summary.setFont(BODY_FONT);
        summary.setForeground(TEXT);
        map.setLayer(Navigation.getCurrentMapLayer());
        map.setCurrentPosition(Navigation.getCurrentPositionId());
        if (route != null) map.setDestination(Navigation.getDestinationId());

        JPanel left = card();
        left.setLayout(new BorderLayout(0, 8));
        left.add(summary, BorderLayout.NORTH);
        JPanel stepPanel = new JPanel(new BorderLayout(0, 6));
        stepPanel.setOpaque(false);
        progress.setFont(BOLD_FONT);
        stepPanel.add(progress, BorderLayout.NORTH);
        stepPanel.add(new JScrollPane(instructions), BorderLayout.CENTER);
        left.add(stepPanel, BorderLayout.CENTER);

        JPanel controls = new JPanel(new java.awt.GridLayout(0, 2, 8, 6));
        controls.setOpaque(false);
        JButton[] actionButtons = {startBtn, previousBtn, nextBtn, stopBtn, mapBtn,
                anotherBtn, reportBtn, homeBtn};
        for (JButton button : actionButtons) {
            button.addActionListener(this);
            controls.add(button);
        }
        left.add(controls, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, map);
        split.setResizeWeight(.4);
        split.setBorder(null);
        body.add(split, BorderLayout.CENTER);
        refreshStep();
        setStatus("Demo Navigation — Simulated Location — GPS Not Connected");
    }

    private String buildSteps(ArrayList<Integer> path) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            Location current = graph.get(path.get(i));
            if (i == 0) {
                text.append("1. Start at ").append(current.getName()).append(".\n\n");
            } else {
                Location previous = graph.get(path.get(i - 1));
                text.append(i + 1).append(". ").append(graph.getEdgeDescription(previous.getId(), current.getId()))
                        .append("\n   Next: ").append(current.getDisplayName()).append(" — illustrative ")
                        .append((int) graph.getDistance(previous.getId(), current.getId())).append(" m.\n\n");
            }
        }
        text.append("All distances and walking times are configured demonstration values.");
        return text.toString();
    }

    private void refreshStep() {
        Route route = Navigation.getCurrentRoute();
        if (route == null || route.getPath().isEmpty()) {
            progress.setText("No route selected.");
            return;
        }
        ArrayList<Integer> path = route.getPath();
        int step = Math.min(Navigation.getRouteStep(), path.size() - 1);
        int currentId = path.get(step);
        Location current = graph.get(currentId);
        String text;
        if (step == 0) {
            text = "Start at " + current.getName() + ".";
        } else {
            int previousId = path.get(step - 1);
            text = graph.getEdgeDescription(previousId, currentId) + "\nNext waypoint: " + current.getDisplayName()
                    + " (illustrative " + (int) graph.getDistance(previousId, currentId) + " m).";
            if (!graph.get(previousId).isIndoor() && current.isIndoor()) {
                text += "\nYou reached the building entrance; follow the indoor corridor route.";
            }
        }
        if (step == path.size() - 1) text += "\nYou have arrived at the destination.";
        instructions.setText(text + "\n\nFull route:\n" + buildSteps(path));
        instructions.setCaretPosition(0);
        progress.setText("Step " + (step + 1) + " of " + path.size()
                + (Navigation.isDemoRunning() ? "  •  navigation running" : "  •  ready"));
        map.setHighlightPath(path);
        map.setDestination(path.get(path.size() - 1));
        map.setCurrentPosition(currentId);
        map.setLayer(current.getLayer());
        setStatus("Demo Navigation — Simulated Location — GPS Not Connected");
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == startBtn) {
            Navigation.startDemoNavigation();
            refreshStep();
        } else if (source == previousBtn) {
            Navigation.previousRouteStep();
            refreshStep();
        } else if (source == nextBtn) {
            Navigation.advanceRouteStep();
            refreshStep();
        } else if (source == stopBtn) {
            Navigation.stopDemoNavigation();
            refreshStep();
        } else if (source == mapBtn) {
            Navigation.showMap(this);
        } else if (source == anotherBtn) {
            Navigation.showDestination(this);
        } else if (source == reportBtn) {
            Navigation.showReport(this);
        } else if (source == homeBtn) {
            Navigation.showHome(this);
        }
    }
}
