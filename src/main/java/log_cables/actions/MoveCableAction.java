package log_cables.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import log_cables.CablesDAO;
import log_cables.LogCablesTab;
import utils.UIComponentUtils;

public class MoveCableAction implements ActionListener {
    private final LogCablesTab tab;

    public MoveCableAction(LogCablesTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedRow = tab.getCableTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a cable to move");
            return;
        }

        String cableType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tab.getLocationTree().getLastSelectedPathComponent();
        if (node == null) {
            tab.setStatus("Error: Select a location first");
            return;
        }
        String currentLocation = node.getUserObject().equals("Unassigned in this location")
                ? buildPathFromNode((DefaultMutableTreeNode) node.getParent())
                : buildPathFromNode(node);

        JDialog dialog = new JDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, tab), "Move Cable", true);
        dialog.setSize(300, 200);
        dialog.setLayout(new java.awt.BorderLayout());
        dialog.setLocationRelativeTo(tab);

        JPanel inputPanel = new JPanel(new java.awt.BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel locationLabel = new JLabel("Select new location:");
        JComboBox<String> locationComboBox = new JComboBox<>();
        try {
            List<String> locations = CablesDAO.getAllLocations();
            for (String location : locations) {
                if (!location.equals(currentLocation)) {
                    locationComboBox.addItem(location);
                }
            }
        } catch (SQLException ex) {
            tab.setStatus("Error retrieving locations: " + ex.getMessage());
            JOptionPane.showMessageDialog(dialog, "Error retrieving locations: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JLabel quantityLabel = new JLabel("Enter quantity to move:");
        JTextField quantityField = UIComponentUtils.createFormattedTextField();
        inputPanel.add(locationLabel, java.awt.BorderLayout.NORTH);
        inputPanel.add(locationComboBox, java.awt.BorderLayout.CENTER);
        inputPanel.add(quantityLabel, java.awt.BorderLayout.SOUTH);
        inputPanel.add(quantityField, java.awt.BorderLayout.SOUTH);

        JButton moveButton = UIComponentUtils.createFormattedButton("Move");
        moveButton.addActionListener(e1 -> {
            String newLocation = (String) locationComboBox.getSelectedItem();
            String quantityText = quantityField.getText().trim();
            if (newLocation == null || newLocation.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select a new location", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int quantity;
            try {
                quantity = Integer.parseInt(quantityText);
                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Quantity must be positive", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid quantity format", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                int cableId = CablesDAO.getCableId(cableType, currentLocation);
                if (cableId == -1) {
                    tab.setStatus("Error: Cable type '" + cableType + "' not found at " + currentLocation);
                    JOptionPane.showMessageDialog(dialog, "Cable not found", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                CablesDAO.moveCable(cableId, newLocation, quantity);
                tab.setStatus("Successfully moved " + quantity + " " + cableType + " to " + newLocation);
                tab.refresh();
                dialog.dispose();
            } catch (SQLException ex) {
                tab.setStatus("Error moving cable: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "Error moving cable: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(inputPanel, java.awt.BorderLayout.CENTER);
        dialog.add(moveButton, java.awt.BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private String buildPathFromNode(DefaultMutableTreeNode node) {
        if (node == null || node.isRoot()) {
            return null;
        }
        TreePath path = new TreePath(node.getPath());
        Object[] nodes = path.getPath();
        StringBuilder fullPath = new StringBuilder();
        for (int i = 1; i < nodes.length; i++) { // Skip root
            if (i > 1) {
                fullPath.append(LogCablesTab.getPathSeparator());
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}