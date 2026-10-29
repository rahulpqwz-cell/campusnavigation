package campusnav.gui;

import campusnav.model.Location;
import campusnav.model.Route;
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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/** Choose the start, the destination and the search method, then find the route. */
public class DestinationFrame extends BaseFrame implements ActionListener {

    private CampusGraph graph = Navigation.getGraph();
    private JComboBox<Location> fromBox, toBox;
    private JRadioButton shortestRb, fewestRb;
    private JButton findBtn, swapBtn, backBtn;

    public DestinationFrame() {
        super("Find Route");

        ArrayList<Location> sorted = graph.getDestinations();
        Sorter.sortByName(sorted);
        fromBox = new JComboBox<Location>();
        toBox = new JComboBox<Location>();
        for (Location l : sorted) {
            fromBox.addItem(l);
            toBox.addItem(l);
        }
        fromBox.setFont(BODY_FONT);
        toBox.setFont(BODY_FONT);
        select(fromBox, Navigation.getSourceId());
        select(toBox, Navigation.getDestinationId());

        shortestRb = new JRadioButton("Shortest distance (Dijkstra's algorithm)", true);
        fewestRb = new JRadioButton("Fewest stops (Breadth First Search)");
        shortestRb.setFont(BODY_FONT);
        fewestRb.setFont(BODY_FONT);
        shortestRb.setOpaque(false);
        fewestRb.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        group.add(shortestRb);
        group.add(fewestRb);

        swapBtn = plainButton("Swap");
        findBtn = button("Find Route");
        swapBtn.addActionListener(this);
        findBtn.addActionListener(this);

        // the card stretches with the window width
        JPanel card = card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        card.add(label("From"), c);
        c.gridx = 1;
        c.weightx = 1;
        card.add(fromBox, c);
        c.gridx = 2;
        c.weightx = 0;
        card.add(swapBtn, c);

        c.gridx = 0;
        c.gridy = 1;
        card.add(label("To"), c);
        c.gridx = 1;
        c.weightx = 1;
        card.add(toBox, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        card.add(label("Method"), c);
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        JPanel modes = new JPanel(new GridBagLayout());
        modes.setOpaque(false);
        GridBagConstraints m = new GridBagConstraints();
        m.gridx = 0;
        m.weightx = 1;
        m.anchor = GridBagConstraints.WEST;
        m.gridy = 0;
        modes.add(shortestRb, m);
        m.gridy = 1;
        modes.add(fewestRb, m);
        card.add(modes, c);

        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        card.add(findBtn, c);

        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        GridBagConstraints w = new GridBagConstraints();
        w.fill = GridBagConstraints.HORIZONTAL;
        w.weightx = 1;
        w.weighty = 1;
        w.anchor = GridBagConstraints.NORTH;
        wrap.add(card, w);
        body.add(wrap, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.setOpaque(false);
        backBtn = plainButton("Back");
        backBtn.addActionListener(this);
        south.add(backBtn);
        body.add(south, BorderLayout.SOUTH);

        setStatus("Pick a start and a destination. Closed paths are avoided automatically.");
    }

    private void select(JComboBox<Location> box, int id) {
        if (id < 0) {
            return;
        }
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getItemAt(i).getId() == id) {
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == swapBtn) {
            int a = fromBox.getSelectedIndex();
            fromBox.setSelectedIndex(toBox.getSelectedIndex());
            toBox.setSelectedIndex(a);
        } else if (src == findBtn) {
            Location a = (Location) fromBox.getSelectedItem();
            Location b = (Location) toBox.getSelectedItem();
            if (a.getId() == b.getId()) {
                error("The start and the destination are the same place.");
                return;
            }
            Route r = Navigation.findRoute(a.getId(), b.getId(), fewestRb.isSelected());
            if (r == null) {
                error("No open path connects these two places. Some paths may be closed by the admin.");
                return;
            }
            Navigation.showRoute(this);
        } else if (src == backBtn) {
            Navigation.showHome(this);
        }
    }
}
