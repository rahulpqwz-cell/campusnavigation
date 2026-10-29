package campusnav;

import campusnav.gui.Navigation;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Entry point of the Campus Navigation system. */
public class Main {
    public static void main(String[] args) {
        try {
            // Cross-platform look so button colours look the same on every computer
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // keep the default look and feel
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Navigation.init();
                Navigation.showWelcome(null);
            }
        });
    }
}
