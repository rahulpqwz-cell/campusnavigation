package campusnav.gui;

import campusnav.model.Location;
import campusnav.dsa.CampusGraph;
import campusnav.util.Sorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.DefaultListCellRenderer;
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

/** Search places by name and category. Results are sorted with insertion sort. */
public class SearchFrame extends BaseFrame implements ActionListener, ListSelectionListener {

    private CampusGraph graph = Navigation.getGraph();
    private JTextField searchField;
    private JComboBox<String> categoryBox;
    private DefaultListModel<Location> model;
    private JList<Location> list;
    private JTextArea details;
    private JButton navBtn, mapBtn, backBtn;

    public SearchFrame() {
        super("Search Places");

        // top: search text and category filter
        JPanel top = card();
        top.setLayout(new BorderLayout(10, 0));
        top.add(label("Search:"), BorderLayout.WEST);
        searchField = new JTextField();
        searchField.setFont(BODY_FONT);
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                refresh();
            }
        });
        top.add(searchField, BorderLayout.CENTER);
        categoryBox = new JComboBox<String>();
        categoryBox.setFont(BODY_FONT);
        categoryBox.addItem("All");
        for (String c : graph.getCategories()) {
            categoryBox.addItem(c);
        }
        categoryBox.addActionListener(this);
        top.add(categoryBox, BorderLayout.EAST);
        body.add(top, BorderLayout.NORTH);

        // centre: results (left) and details (right) in a split pane that resizes with the window
        model = new DefaultListModel<Location>();
        list = new JList<Location>(model);
        list.setFont(BODY_FONT);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(30);
        list.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(l, value, index, selected, focus);
                Location loc = (Location) value;
                setText("  " + loc.getName() + "   [" + loc.getCategory() + "]");
                return this;
            }
        });
        list.addListSelectionListener(this);
        details = readOnlyArea();
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), new JScrollPane(details));
        split.setResizeWeight(0.5);
        split.setBorder(null);
        body.add(split, BorderLayout.CENTER);

        // bottom buttons
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        south.setOpaque(false);
        navBtn = button("Navigate Here");
        mapBtn = plainButton("Show on Map");
        backBtn = plainButton("Back");
        navBtn.addActionListener(this);
        mapBtn.addActionListener(this);
        backBtn.addActionListener(this);
        south.add(navBtn);
        south.add(mapBtn);
        south.add(backBtn);
        body.add(south, BorderLayout.SOUTH);

        refresh();
    }

    private void refresh() {
        String text = searchField.getText().trim();
        String q = text.toLowerCase();
        String cat = (String) categoryBox.getSelectedItem();

        ArrayList<Location> all = graph.getDestinations();
        Sorter.sortByName(all);

        model.clear();
        for (Location l : all) {
            boolean catOk = cat.equals("All") || l.getCategory().equals(cat);
            boolean textOk = q.length() == 0 || l.matches(text);
            if (catOk && textOk) {
                model.addElement(l);
            }
        }

        String msg = model.size() + " place(s) found.";
        int exact = Sorter.binarySearchByName(all, text);
        if (text.length() > 0 && exact >= 0) {
            msg = msg + "  Exact match: " + all.get(exact).getName();
        }
        setStatus(msg);

        if (model.size() > 0) {
            list.setSelectedIndex(0);
        } else {
            details.setText("No place matches your search.");
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        Location l = list.getSelectedValue();
        if (l == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(l.getName()).append("\n");
        sb.append("Category: ").append(l.getCategory()).append("\n\n");
        sb.append("Directly connected to:\n");
        for (int n : graph.neighbours(l.getId())) {
            sb.append("  - ").append(graph.get(n).getName()).append("  (")
                    .append((int) graph.getDistance(l.getId(), n)).append(" m, ")
                    .append(graph.direction(l.getId(), n));
            if (graph.isClosed(l.getId(), n)) {
                sb.append(", CLOSED");
            }
            sb.append(")\n");
        }
        details.setText(sb.toString());
        details.setCaretPosition(0);
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == categoryBox) {
            refresh();
        } else if (src == navBtn || src == mapBtn) {
            Location l = list.getSelectedValue();
            if (l == null) {
                error("Select a place from the list first.");
                return;
            }
            if (src == navBtn) {
                Navigation.setDestinationId(l.getId());
                Navigation.showDestination(this);
            } else {
                Navigation.setFocusId(l.getId());
                Navigation.showMap(this);
            }
        } else if (src == backBtn) {
            Navigation.showHome(this);
        }
    }
}
