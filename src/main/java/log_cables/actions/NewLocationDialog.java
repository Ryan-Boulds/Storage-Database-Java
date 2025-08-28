package log_cables.actions;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import log_cables.CablesDAO;
import log_cables.LogCablesTab;
import utils.UIComponentUtils;

public class NewLocationDialog {
    @SuppressWarnings("unused")
    private final LogCablesTab tab;
    private final JDialog dialog;

    public NewLocationDialog(LogCablesTab tab) {
        this.tab = tab;
        dialog = new JDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, tab), "New Location", true);
        dialog.setSize(300, 150);
        dialog.setLayout(new java.awt.BorderLayout());
        dialog.setLocationRelativeTo(tab);

        JPanel inputPanel = new JPanel(new java.awt.BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel label = new JLabel("Enter new location name:");
        JTextField textField = UIComponentUtils.createFormattedTextField();
        // Prevent spaces in text field
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    e.consume();
                }
            }
        });
        inputPanel.add(label, java.awt.BorderLayout.NORTH);
        inputPanel.add(textField, java.awt.BorderLayout.CENTER);

        JButton addButton = UIComponentUtils.createFormattedButton("Add");
        addButton.addActionListener(e1 -> {
            String newLocation = textField.getText().trim();
            if (newLocation.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Location name cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!newLocation.matches("[a-zA-Z0-9-_]+")) {
                JOptionPane.showMessageDialog(dialog, "Invalid characters. Use letters, numbers, -, or _ only", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                if (CablesDAO.locationExists(newLocation)) {
                    JOptionPane.showMessageDialog(dialog, "Location already exists", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                CablesDAO.createLocation(newLocation);
                tab.refreshTree();
                tab.setStatus("Successfully created location: " + newLocation);
                dialog.dispose();
            } catch (SQLException ex) {
                tab.setStatus("Error creating location: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "Error creating location: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(inputPanel, java.awt.BorderLayout.CENTER);
        dialog.add(addButton, java.awt.BorderLayout.SOUTH);
    }

    public void showDialog() {
        dialog.setVisible(true);
    }
}