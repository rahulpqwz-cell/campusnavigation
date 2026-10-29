package campusnav.gui;

import campusnav.model.Location;
import campusnav.dsa.CampusGraph;
import campusnav.util.Sorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.ListSelectionModel;

/**
 * NEW FRAME 2 - nearby facilities.
 * Pick where you are and what you need (library, canteen, medical ...) and the places of that
 * type are listed nearest first (Dijkstra distances + insertion sort).
 */
public class FacilitiesFrame extends BaseFrame implements ActionListener {

    private CampusGraph graph = Navigation.getGraph();
    private JComboBox<Location> fromBox;
    private JComboBox<String> typeBox;
    private DefaultListModel<String> model;
    private JList<String> list;
    private ArrayList<Integer> resultIds = new ArrayList<Integer>();
    private JButton goBtn, backBtn;

    public FacilitiesFrame() {
        super("Nearby Facilities");

        JPanel top = card();
        top.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.weightx = 0;
        top.add(label("I am at"), c);
        fromBox = new JComboBox<Location>();
        fromBox.setFont(BODY_FONT);
        ArrayList<Location> sorted = graph.getDestinations();
        Sorter.sortByName(sorted);
        for (Location l : sorted) {
            fromBox.addItem(l);
        }
        c.gridx = 1;
        c.weightx = 1;
        top.add(fromBox, c);

        c.gridx = 2;
        c.weightx = 0;
        top.add(label("I need"), c);
        typeBox = new JComboBox<String>();
        typeBox.setFont(BODY_FONT);
        for (String cat : graph.getCategories()) {
            typeBox.addItem(cat);
        }
        c.gridx = 3;
        c.weightx = 1;
        top.add(typeBox, c);
        body.add(top, BorderLayout.NORTH);

        model = new DefaultListModel<String>();
        list = new JList<String>(model);
        list.setFont(BODY_FONT);
        list.setFixedCellHeight(32);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        body.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        south.setOpaque(false);
        goBtn = button("Get Route to Selected");
        backBtn = plainButton("Back");
        goBtn.addActionListener(this);
        backBtn.addActionListener(this);
        south.add(goBtn);
        south.add(backBtn);
        body.add(south, BorderLayout.SOUTH);

        int start = Navigation.getSourceId();
        if (start >= 0) {
            for (int i = 0; i < fromBox.getItemCount(); i++) {
                if (fromBox.getItemAt(i).getId() == start) {
                    fromBox.setSelectedIndex(i);
                }
            }
        }
        search();
        fromBox.addActionListener(this); // listeners added last so the first search is not run twice
        typeBox.addActionListener(this);
    }

    private void search() {
        Location from = (Location) fromBox.getSelectedItem();
        String type = (String) typeBox.getSelectedItem();
        if (from == null || type == null) {
            return;
        }
        double[] dist = graph.distancesFrom(from.getId());

        resultIds.clear();
        for (int i = 0; i < graph.size(); i++) {
            if (i != from.getId() && graph.get(i).getCategory().equals(type)) {
                resultIds.add(i);
            }
        }
        Sorter.sortByDistance(resultIds, dist);

        model.clear();
        for (int k = 0; k < resultIds.size(); k++) {
            int id = resultIds.get(k);
            String text = (k + 1) + ".  " + graph.get(id).getName() + "  -  ";
            if (dist[id] >= CampusGraph.INF) {
                text += "not reachable (paths closed)";
            } else {
                int metres = (int) dist[id];
                int minutes = Math.max(1, (int) Math.ceil(dist[id] / 80.0));
                text += metres + " m, about " + minutes + " min";
                if (k == 0) {
                    text += "   <- nearest";
                }
            }
            model.addElement(text);
        }
        if (resultIds.isEmpty()) {
            setStatus("No other " + type + " place found.");
        } else {
            list.setSelectedIndex(0);
            setStatus(resultIds.size() + " " + type + " place(s), nearest first.");
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == fromBox || src == typeBox) {
            search();
        } else if (src == goBtn) {
            int idx = list.getSelectedIndex();
            if (idx < 0) {
                error("Select a facility from the list first.");
                return;
            }
            Location from = (Location) fromBox.getSelectedItem();
            int target = resultIds.get(idx);
            if (Navigation.findRoute(from.getId(), target, false) == null) {
                error("No open path leads to that place right now.");
                return;
            }
            Navigation.showRoute(this);
        } else if (src == backBtn) {
            Navigation.showHome(this);
        }
    }
}
