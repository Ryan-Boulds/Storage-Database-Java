package log_chargers.actions;

import java.sql.SQLException;
import java.util.LinkedHashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import log_chargers.ChargersDAO;
import log_chargers.LogChargersTab;
import utils.UIComponentUtils;

public class NewLocationDialog {
    private final LogChargersTab tab;
    private final String parentLocation;

    public NewLocationDialog(LogChargersTab tab, String parentLocation) {
        this.tab = tab;
        this.parentLocation = normalizePath(parentLocation);
    }

    public void showDialog() {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, tab), "New Location", true);
        dialog.setSize(300, 150);
        dialog.setLayout(new java.awt.BorderLayout());
        dialog.setLocationRelativeTo(tab);

        JPanel inputPanel = new JPanel(new java.awt.BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel label = new JLabel("Enter new location name:");
        JTextField locationField = UIComponentUtils.createFormattedTextField();
        inputPanel.add(label, java.awt.BorderLayout.NORTH);
        inputPanel.add(locationField, java.awt.BorderLayout.CENTER);

        JButton createButton = UIComponentUtils.createFormattedButton("Create");
        createButton.addActionListener(e -> {
            String locationName = locationField.getText().trim();
            if (locationName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Location name cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (locationName.contains(LogChargersTab.getPathSeparator())) {
                JOptionPane.showMessageDialog(dialog, "Location name cannot contain '" + LogChargersTab.getPathSeparator() + "'", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String fullPath = parentLocation == null ? locationName : parentLocation + LogChargersTab.getPathSeparator() + locationName;
            try {
                if (ChargersDAO.locationExists(fullPath)) {
                    JOptionPane.showMessageDialog(dialog, "Location already exists", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ChargersDAO.createLocation(fullPath, parentLocation);
                tab.refreshTree();
                tab.setStatus("Location created: " + fullPath);
                dialog.dispose();
            } catch (SQLException ex) {
                tab.setStatus("Error creating location: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "Error creating location: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(inputPanel, java.awt.BorderLayout.CENTER);
        dialog.add(createButton, java.awt.BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String[] segments = path.split(LogChargersTab.getPathSeparator());
        LinkedHashSet<String> uniqueSegments = new LinkedHashSet<>();
        for (String segment : segments) {
            if (!segment.isEmpty()) {
                uniqueSegments.add(segment);
            }
        }
        return String.join(LogChargersTab.getPathSeparator(), uniqueSegments);
    }
}