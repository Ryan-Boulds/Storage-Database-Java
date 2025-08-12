// New file: view_software_list_tab/license_key_tracker/KeyRulesDialog.java
package view_software_list_tab.license_key_tracker;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class KeyRulesDialog extends JDialog {
    public KeyRulesDialog(JDialog parent) {
        super(parent, "Key Rules Settings", true);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Settings coming soon."), BorderLayout.CENTER);
        setContentPane(panel);
        pack();
    }
}