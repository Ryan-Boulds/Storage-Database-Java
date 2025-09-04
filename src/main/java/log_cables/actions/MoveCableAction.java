package log_cables.actions;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

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
        if (tab.isSummaryView()) {
            JOptionPane.showMessageDialog(tab, "Cannot perform this action in summary view", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int selectedRow = tab.getCableTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a cable to move");
            JOptionPane.showMessageDialog(null, "Please select a cable to move", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String cableType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tab.getLocationTree().getLastSelectedPathComponent();
        if (node == null) {
            tab.setStatus("Error: Select a location first");
            JOptionPane.showMessageDialog(null, "Please select a location first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String currentLocation = node.getUserObject().equals("Unassigned in this location")
                ? buildPathFromNode((DefaultMutableTreeNode) node.getParent())
                : buildPathFromNode(node);

        JDialog dialog = new JDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, tab), "Move Cable", true);
        dialog.setSize(700, 500); // Increased size for better usability
        dialog.setMinimumSize(new Dimension(600, 450)); // Prevent resizing too small
        dialog.setLayout(new GridBagLayout());
        dialog.setLocationRelativeTo(tab);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Location Tree
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel locationLabel = new JLabel("Select New Location:");
        inputPanel.add(locationLabel, gbc);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.5; // Give more vertical space to the tree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        try {
            // Check if "Unassigned" should be added
            List<CablesDAO.CableEntry> unassignedCables = CablesDAO.getCablesByLocation("Unassigned");
            boolean hasUnassignedCables = false;
            for (CablesDAO.CableEntry cable : unassignedCables) {
                if (!cable.cableType.startsWith("Placeholder_")) {
                    hasUnassignedCables = true;
                    break;
                }
            }
            if (hasUnassignedCables) {
                root.add(new DefaultMutableTreeNode("Unassigned"));
            }

            List<String> topLevelLocations = CablesDAO.getSubLocations(null);
            for (String location : topLevelLocations) {
                DefaultMutableTreeNode locationNode = new DefaultMutableTreeNode(getLastSegment(location));
                // Check if "Unassigned in this location" should be added
                List<CablesDAO.CableEntry> directCables = CablesDAO.getCablesByLocation(location);
                boolean hasDirectCables = false;
                for (CablesDAO.CableEntry cable : directCables) {
                    if (!cable.cableType.startsWith("Placeholder_")) {
                        hasDirectCables = true;
                        break;
                    }
                }
                List<String> subLocations = CablesDAO.getSubLocations(location);
                if (!subLocations.isEmpty() && hasDirectCables) {
                    locationNode.add(new DefaultMutableTreeNode("Unassigned in this location"));
                }
                addSubLocations(locationNode, location);
                root.add(locationNode);
            }
            if (topLevelLocations.isEmpty() && !hasUnassignedCables && !CablesDAO.locationExists("Unassigned")) {
                tab.setStatus("Warning: No locations available. Please create a location first.");
                JOptionPane.showMessageDialog(dialog, "No locations available. Please create a location first.", "Warning", JOptionPane.WARNING_MESSAGE);
                dialog.dispose();
                return;
            }
        } catch (SQLException ex) {
            tab.setStatus("Error retrieving locations: " + ex.getMessage());
            JOptionPane.showMessageDialog(dialog, "Error retrieving locations: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            dialog.dispose();
            return;
        }

        JTree locationTree = new JTree(new DefaultTreeModel(root));
        locationTree.setRootVisible(false);
        for (int i = 0; i < locationTree.getRowCount(); i++) {
            locationTree.expandRow(i);
        }
        JScrollPane treeScrollPane = new JScrollPane(locationTree);
        treeScrollPane.setPreferredSize(new Dimension(500, 300)); // Increased size for better visibility
        inputPanel.add(treeScrollPane, gbc);

        // Quantity
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel quantityLabel = new JLabel("Quantity to Move:");
        inputPanel.add(quantityLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        JTextField quantityField = UIComponentUtils.createFormattedTextField();
        inputPanel.add(quantityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        JButton moveButton = UIComponentUtils.createFormattedButton("Move Cable");
        moveButton.addActionListener(e1 -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
            if (selectedNode == null || selectedNode.isRoot()) {
                JOptionPane.showMessageDialog(dialog, "Please select a new location", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String selectedValue = (String) selectedNode.getUserObject();
            if (selectedValue.equals("Unassigned in this location")) {
                JOptionPane.showMessageDialog(dialog, "Cannot move to 'Unassigned in this location'. Please select a valid location.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String newLocation = buildPathFromNode(selectedNode);
            if (newLocation == null || newLocation.equals(currentLocation)) {
                JOptionPane.showMessageDialog(dialog, "Please select a different location", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String quantityText = quantityField.getText().trim();
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
        inputPanel.add(moveButton, gbc);

        dialog.add(inputPanel);
        dialog.setVisible(true);
    }

    private void addSubLocations(DefaultMutableTreeNode parentNode, String parentLocation) throws SQLException {
        List<String> subLocations = CablesDAO.getSubLocations(parentLocation);
        for (String subLocation : subLocations) {
            DefaultMutableTreeNode locationNode = new DefaultMutableTreeNode(getLastSegment(subLocation));
            List<String> subSubLocations = CablesDAO.getSubLocations(subLocation);
            List<CablesDAO.CableEntry> directCables = CablesDAO.getCablesByLocation(subLocation);
            boolean hasDirectCables = false;
            for (CablesDAO.CableEntry cable : directCables) {
                if (!cable.cableType.startsWith("Placeholder_")) {
                    hasDirectCables = true;
                    break;
                }
            }
            if (!subSubLocations.isEmpty() && hasDirectCables) {
                locationNode.add(new DefaultMutableTreeNode("Unassigned in this location"));
            }
            addSubLocations(locationNode, subLocation);
            parentNode.add(locationNode);
        }
    }

    private String getLastSegment(String location) {
        if (location.contains(LogCablesTab.getPathSeparator())) {
            return location.substring(location.lastIndexOf(LogCablesTab.getPathSeparator()) + LogCablesTab.getPathSeparator().length());
        } else {
            return location;
        }
    }

    private String buildPathFromNode(DefaultMutableTreeNode node) {
        if (node == null || node.isRoot()) {
            return null;
        }
        if (node.getUserObject().equals("Unassigned")) {
            return "Unassigned";
        }
        javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(node.getPath());
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