package log_cables.actions;

import java.awt.BorderLayout;
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
        String nodeValue = (String) node.getUserObject();
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
        String currentLocation;
        if (nodeValue.equals("Unassigned") && parentNode instanceof DefaultMutableTreeNode && !((DefaultMutableTreeNode) parentNode).isRoot()) {
            currentLocation = buildPathFromNode(parentNode).replace("/", LogCablesTab.getPathSeparator());
        } else {
            currentLocation = buildPathFromNode(node).replace("/", LogCablesTab.getPathSeparator());
        }

        JDialog dialog = new JDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, tab), "Move Cable", true);
        dialog.setSize(600, 400);
        dialog.setMinimumSize(new java.awt.Dimension(500, 300));
        dialog.setLayout(new BorderLayout(10, 10));

        // Location Tree Panel
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        JLabel locationLabel = new JLabel("Select New Location:");
        treePanel.add(locationLabel, BorderLayout.NORTH);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        JTree locationTree;
        try {
            // Add top-level "Unassigned"
            List<CablesDAO.CableEntry> unassignedCables = CablesDAO.getCablesByLocation(LogCablesTab.getUnassignedLocation());
            boolean hasUnassignedCables = false;
            for (CablesDAO.CableEntry cable : unassignedCables) {
                if (!cable.cableType.startsWith("Placeholder_")) {
                    hasUnassignedCables = true;
                    break;
                }
            }
            if (hasUnassignedCables) {
                root.add(new DefaultMutableTreeNode(LogCablesTab.getUnassignedLocation()));
            }

            // Get all top-level locations
            List<String> topLevelLocations = CablesDAO.getSubLocations(null);
            for (String location : topLevelLocations) {
                if (location.equals(LogCablesTab.getUnassignedLocation())) continue;
                addLocationToTree(root, location);
            }
            locationTree = new JTree(new DefaultTreeModel(root));
            locationTree.setRootVisible(false);
            for (int i = 0; i < locationTree.getRowCount(); i++) {
                locationTree.expandRow(i);
            }
            JScrollPane treeScrollPane = new JScrollPane(locationTree);
            treePanel.add(treeScrollPane, BorderLayout.CENTER);
        } catch (SQLException ex) {
            tab.setStatus("Error loading locations for move: " + ex.getMessage());
            JOptionPane.showMessageDialog(dialog, "Error loading locations: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Input and Button Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel quantityLabel = new JLabel("Quantity to Move:");
        inputPanel.add(quantityLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField quantityField = UIComponentUtils.createFormattedTextField();
        inputPanel.add(quantityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton moveButton = UIComponentUtils.createFormattedButton("Move Cable");
        moveButton.addActionListener(e1 -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
            if (selectedNode == null || selectedNode.isRoot()) {
                JOptionPane.showMessageDialog(dialog, "Please select a new location", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String newLocation = buildPathFromNode(selectedNode).replace("/", LogCablesTab.getPathSeparator());
            if (newLocation == null || newLocation.equals(currentLocation)) {
                JOptionPane.showMessageDialog(dialog, "Please select a different location", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (newLocation.endsWith(LogCablesTab.getPathSeparator() + "Unassigned")) {
                JOptionPane.showMessageDialog(dialog, "Cannot move to 'Unassigned' in a location. Please select a valid location.", "Error", JOptionPane.ERROR_MESSAGE);
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

        dialog.add(treePanel, BorderLayout.CENTER);
        dialog.add(inputPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(tab);
        dialog.setVisible(true);
    }

    private void addLocationToTree(DefaultMutableTreeNode parentNode, String location) throws SQLException {
        String[] segments = location.split(LogCablesTab.getPathSeparator());
        DefaultMutableTreeNode currentNode = parentNode;
        StringBuilder currentPath = new StringBuilder();
        StringBuilder displayPath = new StringBuilder();

        // Build the hierarchical tree structure with display separator
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (i > 0) {
                currentPath.append(LogCablesTab.getPathSeparator());
                displayPath.append("/");
            }
            currentPath.append(segment);
            displayPath.append(segment);

            // Check if the node already exists
            boolean nodeExists = false;
            for (int j = 0; j < currentNode.getChildCount(); j++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) currentNode.getChildAt(j);
                if (child.getUserObject().equals(segment)) {
                    currentNode = child;
                    nodeExists = true;
                    break;
                }
            }

            if (!nodeExists) {
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(segment);
                currentNode.add(newNode);
                currentNode = newNode;
            }
        }

        // Add "Unassigned" if there are direct cables and sublocations
        String fullPath = currentPath.toString();
        List<CablesDAO.CableEntry> directCables = CablesDAO.getCablesByLocation(fullPath);
        boolean hasDirectCables = !directCables.isEmpty();
        List<String> subLocations = CablesDAO.getSubLocations(fullPath);
        if (!subLocations.isEmpty() && hasDirectCables) {
            currentNode.add(new DefaultMutableTreeNode("Unassigned"));
        }

        // Recursively add sublocations
        for (String subLocation : subLocations) {
            addLocationToTree(parentNode, subLocation);
        }
    }

    private String buildPathFromNode(DefaultMutableTreeNode node) {
        if (node == null || node.isRoot()) {
            return null;
        }
        Object userObject = node.getUserObject();
        if (userObject.equals(LogCablesTab.getUnassignedLocation()) && node.getParent() instanceof DefaultMutableTreeNode && ((DefaultMutableTreeNode) node.getParent()).isRoot()) {
            return LogCablesTab.getUnassignedLocation();
        }
        if (userObject.equals("Unassigned")) {
            javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(node.getPath());
            Object[] nodes = path.getPath();
            StringBuilder fullPath = new StringBuilder();
            for (int i = 1; i < nodes.length - 1; i++) { // Exclude the "Unassigned" node
                if (i > 1) {
                    fullPath.append("/");
                }
                fullPath.append(nodes[i].toString());
            }
            return fullPath.toString();
        }
        javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(node.getPath());
        Object[] nodes = path.getPath();
        StringBuilder fullPath = new StringBuilder();
        for (int i = 1; i < nodes.length; i++) { // Skip root
            if (i > 1) {
                fullPath.append("/");
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}