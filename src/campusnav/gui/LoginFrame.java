package campusnav.gui;

import campusnav.model.User;
import campusnav.util.AuthException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Role-aware local login and Student/Staff registration. */
public class LoginFrame extends BaseFrame implements ActionListener {
    private JTextField userField;
    private JPasswordField passField;
    private JPasswordField confirmField;
    private JComboBox<String> roleBox;
    private JButton loginBtn;
    private JButton registerBtn;
    private JButton guestBtn;
    private JButton backBtn;

    public LoginFrame() {
        super("Sign In");
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(BG);
        JPanel card = card();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(430, 470));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 6, 5, 6);

        JLabel title = new JLabel("Sign in or create a reporting account");
        title.setFont(new Font("SansSerif", Font.BOLD, 19));
        title.setForeground(PRIMARY);
        card.add(title, c);
        c.gridy++;
        card.add(label("Username"), c);
        userField = new JTextField();
        userField.setFont(BODY_FONT);
        c.gridy++;
        card.add(userField, c);
        c.gridy++;
        card.add(label("Password"), c);
        passField = new JPasswordField();
        passField.setFont(BODY_FONT);
        passField.addActionListener(this);
        c.gridy++;
        card.add(passField, c);
        c.gridy++;
        card.add(label("Confirm password (registration only)"), c);
        confirmField = new JPasswordField();
        confirmField.setFont(BODY_FONT);
        c.gridy++;
        card.add(confirmField, c);
        c.gridy++;
        card.add(label("New account type"), c);
        roleBox = new JComboBox<String>(new String[] {"Student", "Staff"});
        roleBox.setFont(BODY_FONT);
        c.gridy++;
        card.add(roleBox, c);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        loginBtn = button("Sign In");
        registerBtn = plainButton("Register");
        loginBtn.addActionListener(this);
        registerBtn.addActionListener(this);
        buttons.add(loginBtn);
        buttons.add(registerBtn);
        c.gridy++;
        c.insets = new Insets(12, 6, 5, 6);
        card.add(buttons, c);

        JLabel hint = new JLabel("Demo Faculty: faculty / faculty123");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 12));
        hint.setForeground(java.awt.Color.GRAY);
        c.gridy++;
        c.insets = new Insets(5, 6, 5, 6);
        card.add(hint, c);

        JLabel access = new JLabel("Faculty accounts cannot be created from this screen.");
        access.setFont(new Font("SansSerif", Font.PLAIN, 12));
        access.setForeground(java.awt.Color.GRAY);
        c.gridy++;
        card.add(access, c);
        wrap.add(card);
        body.add(wrap, BorderLayout.CENTER);

        JPanel south = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        south.setOpaque(false);
        guestBtn = plainButton("Continue as Guest");
        backBtn = plainButton("Back");
        guestBtn.addActionListener(this);
        backBtn.addActionListener(this);
        south.add(guestBtn);
        south.add(backBtn);
        body.add(south, BorderLayout.SOUTH);
        setStatus(Navigation.hasPendingReport()
                ? "Sign in as Student or Staff to continue to the report form."
                : "Search, map and route features are available without an account.");
    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == loginBtn || source == passField) {
            try {
                User user = Navigation.getUserManager().login(userField.getText(), new String(passField.getPassword()));
                Navigation.loginUser(user);
                Navigation.afterLogin(this);
            } catch (AuthException ex) {
                error(ex.getMessage());
            }
        } else if (source == registerBtn) {
            try {
                String role = String.valueOf(roleBox.getSelectedItem()).toUpperCase(java.util.Locale.ROOT);
                User user = Navigation.getUserManager().register(userField.getText(),
                        new String(passField.getPassword()), new String(confirmField.getPassword()), role);
                Navigation.loginUser(user);
                Navigation.afterLogin(this);
            } catch (AuthException ex) {
                error(ex.getMessage());
            }
        } else if (source == guestBtn) {
            Navigation.setPendingReport(false);
            Navigation.continueAsGuest();
            Navigation.showHome(this);
        } else if (source == backBtn) {
            Navigation.setPendingReport(false);
            Navigation.showWelcome(this);
        }
    }
}
