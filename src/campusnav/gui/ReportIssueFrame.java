package campusnav.gui;

import campusnav.model.Location;
import campusnav.model.Report;
import campusnav.util.Sorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

/** Lets a user report a problem; reports are saved to a file and handled by the admin. */
public class ReportIssueFrame extends BaseFrame implements ActionListener {

    private JComboBox<Location> locationBox;
    private JComboBox<String> typeBox;
    private JTextArea descArea;
    private JTextArea myReports;
    private JButton submitBtn, backBtn;

    public ReportIssueFrame() {
        super("Report an Issue");

        // form on the left
        JPanel form = card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 4, 5, 4);

        form.add(label("Where is the problem?"), c);
        locationBox = new JComboBox<Location>();
        locationBox.setFont(BODY_FONT);
        ArrayList<Location> sorted = Navigation.getGraph().getDestinations();
        Sorter.sortByName(sorted);
        for (Location l : sorted) {
            locationBox.addItem(l);
        }
        c.gridy++;
        form.add(locationBox, c);

        c.gridy++;
        form.add(label("Type of issue"), c);
        typeBox = new JComboBox<String>(new String[] {"Path blocked", "Construction work",
            "Poor lighting", "Wrong distance or information", "Water logging", "Other"});
        typeBox.setFont(BODY_FONT);
        c.gridy++;
        form.add(typeBox, c);

        c.gridy++;
        form.add(label("Describe the problem"), c);
        descArea = new JTextArea(6, 20);
        descArea.setFont(BODY_FONT);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        form.add(new JScrollPane(descArea), c);

        submitBtn = button("Submit Report");
        submitBtn.addActionListener(this);
        c.gridy++;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        form.add(submitBtn, c);

        // my previous reports on the right
        JPanel right = card();
        right.setLayout(new BorderLayout(0, 8));
        right.add(label("My reports"), BorderLayout.NORTH);
        myReports = readOnlyArea();
        right.add(new JScrollPane(myReports), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, form, right);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        body.add(split, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.setOpaque(false);
        backBtn = plainButton("Back");
        backBtn.addActionListener(this);
        south.add(backBtn);
        body.add(south, BorderLayout.SOUTH);

        showMyReports();
        setStatus("Reports are sent to the admin, who handles them in the order they arrive.");
    }

    private void showMyReports() {
        String me = Navigation.getCurrentUser().getUsername();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Report r : Navigation.getReports()) {
            if (r.getUsername().equalsIgnoreCase(me)) {
                count++;
                sb.append("#").append(r.getId()).append("  [").append(r.getStatus()).append("]\n");
                sb.append(r.getLocation()).append(" - ").append(r.getType()).append("\n");
                sb.append(r.getDate()).append("\n");
                sb.append(r.getDescription()).append("\n\n");
            }
        }
        if (count == 0) {
            sb.append("You have not reported anything yet.");
        }
        myReports.setText(sb.toString());
        myReports.setCaretPosition(0);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == submitBtn) {
            String desc = descArea.getText().trim();
            if (desc.length() < 5) {
                error("Please describe the problem (at least 5 characters).");
                return;
            }
            Location loc = (Location) locationBox.getSelectedItem();
            Report r = new Report(Navigation.nextReportId(), Navigation.getCurrentUser().getUsername(),
                    loc.getName(), (String) typeBox.getSelectedItem(), desc, "Pending", Navigation.now());
            Navigation.getReports().add(r);
            if (!Navigation.saveReports()) {
                Navigation.getReports().remove(r);
                error("The report could not be saved. Check that the data folder is writable, then try again.");
                return;
            }
            descArea.setText("");
            showMyReports();
            info("Thank you! Your report #" + r.getId() + " has been submitted.");
        } else if (e.getSource() == backBtn) {
            Navigation.showHome(this);
        }
    }
}
