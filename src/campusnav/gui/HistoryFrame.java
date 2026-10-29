package campusnav.gui;

import campusnav.model.HistoryEntry;
import campusnav.dsa.CampusGraph;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * NEW FRAME 3 - route history.
 * The history is a stack: the newest route is on top, "Undo Last" pops it.
 */
public class HistoryFrame extends BaseFrame implements ActionListener {

    private CampusGraph graph = Navigation.getGraph();
    private DefaultListModel<String> model;
    private JList<String> list;
    private ArrayList<HistoryEntry> shown = new ArrayList<HistoryEntry>();
    private JButton againBtn, undoBtn, clearBtn, backBtn;

    public HistoryFrame() {
        super("Route History");

        model = new DefaultListModel<String>();
        list = new JList<String>(model);
        list.setFont(BODY_FONT);
        list.setFixedCellHeight(32);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        body.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        south.setOpaque(false);
        againBtn = button("Navigate Again");
        undoBtn = plainButton("Undo Last (Pop)");
        clearBtn = dangerButton("Clear All");
        backBtn = plainButton("Back");
        againBtn.addActionListener(this);
        undoBtn.addActionListener(this);
        clearBtn.addActionListener(this);
        backBtn.addActionListener(this);
        south.add(againBtn);
        south.add(undoBtn);
        south.add(clearBtn);
        south.add(backBtn);
        body.add(south, BorderLayout.SOUTH);

        refresh();
    }

    private void refresh() {
        model.clear();
        shown = Navigation.getHistory().toList(); // top of the stack (newest) first
        for (HistoryEntry h : shown) {
            String method = h.isFewestStops() ? "Fewest stops" : "Shortest";
            model.addElement(h.getTime() + "   |   " + graph.get(h.getSourceId()).getName() + "  ->  "
                    + graph.get(h.getDestinationId()).getName() + "   |   " + (int) h.getDistance()
                    + " m   |   " + method);
        }
        if (shown.isEmpty()) {
            model.addElement("No routes yet. Find a route and it will appear here.");
            setStatus("History is empty.");
        } else {
            list.setSelectedIndex(0);
            setStatus(shown.size() + " route(s). Newest first (stack: last in, first out).");
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == againBtn) {
            int idx = list.getSelectedIndex();
            if (shown.isEmpty() || idx < 0) {
                error("Select a route from the list first.");
                return;
            }
            HistoryEntry h = shown.get(idx);
            if (Navigation.findRoute(h.getSourceId(), h.getDestinationId(), h.isFewestStops()) == null) {
                error("That route is not available now because a path on it is closed.");
                return;
            }
            Navigation.showRoute(this);
        } else if (src == undoBtn) {
            if (Navigation.getHistory().isEmpty()) {
                error("There is nothing to undo.");
                return;
            }
            Navigation.getHistory().pop();
            Navigation.saveHistory();
            refresh();
        } else if (src == clearBtn) {
            if (!Navigation.getHistory().isEmpty() && confirm("Delete your whole route history?")) {
                Navigation.getHistory().clear();
                Navigation.saveHistory();
                refresh();
            }
        } else if (src == backBtn) {
            Navigation.showHome(this);
        }
    }
}
