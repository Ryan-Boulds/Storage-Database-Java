package log_chargers.actions;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import log_chargers.ChargersDAO;
import log_chargers.LogChargersTab;

public class SelectLocationDialog {
    private final JDialog dialog;
    private final JTree locationTree;
    private String selectedLocation = null;

    public SelectLocationDialog(JFrame parent) {
        dialog = new JDialog(parent, "Select Location", true);
        dialog.setSize(400, 400);
        dialog.setLayout(new BorderLayout(10, 10));

        // Build location tree to mirror main tab
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        try {
            // Add top-level "Unassigned"
            List<ChargersDAO.ChargerEntry> unassignedChargers = ChargersDAO.getChargersByLocation(LogChargersTab.getUnassignedLocation());
            boolean hasUnassignedChargers = false;
            for (ChargersDAO.ChargerEntry charger : unassignedChargers) {
                if (!charger.chargerType.startsWith("Placeholder_")) {
                    hasUnassignedChargers = true;
                    break;
                }
            }
            if (hasUnassignedChargers) {
                root.add(new DefaultMutableTreeNode(LogChargersTab.getUnassignedLocation()));
            }

            // Add all locations
            List<String> allLocations = ChargersDAO.getAllLocations();
            for (String location : allLocations) {
                if (!location.equals(LogChargersTab.getUnassignedLocation())) {
                    addLocationToTree(root, location);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(dialog, "Error loading locations: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        locationTree = new JTree(new DefaultTreeModel(root));
        locationTree.setRootVisible(false);
        for (int i = 0; i < locationTree.getRowCount(); i++) {
            locationTree.expandRow(i);
        }
        JScrollPane treeScrollPane = new JScrollPane(locationTree);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton selectButton = new JButton("Select");
        selectButton.addActionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
            if (selectedNode == null || selectedNode.isRoot()) {
                JOptionPane.showMessageDialog(dialog, "Please select a location", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            selectedLocation = buildPathFromNode(selectedNode);
            dialog.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);

        dialog.add(treeScrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(parent);
    }

    public String showDialog() {
        dialog.setVisible(true);
        return selectedLocation;
    }

    private void addLocationToTree(DefaultMutableTreeNode parentNode, String fullPath) throws SQLException {
        if (fullPath == null || fullPath.isEmpty()) {
            return;
        }

        String[] segments = fullPath.split(LogChargersTab.getPathSeparator());
        DefaultMutableTreeNode currentNode = parentNode;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (i > 0) {
                currentPath.append(LogChargersTab.getPathSeparator());
            }
            currentPath.append(segment);

            boolean nodeExists = false;
            for (int j = 0; j < currentNode.getChildCount(); j++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) currentNode.getChildAt(j);
                if (child.getUserObject().toString().equals(segment)) {
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

        String fullPathStr = currentPath.toString();
        List<ChargersDAO.ChargerEntry> directChargers = ChargersDAO.getChargersByLocation(fullPathStr);
        List<String> subLocations = ChargersDAO.getSubLocations(fullPathStr);

        boolean hasDirectChargers = false;
        for (ChargersDAO.ChargerEntry charger : directChargers) {
            if (!charger.chargerType.startsWith("Placeholder_")) {
                hasDirectChargers = true;
                break;
            }
        }

        // Add "Unassigned" node only if there are direct chargers and sublocations exist
        if (hasDirectChargers && !subLocations.isEmpty() && !fullPathStr.equals(LogChargersTab.getUnassignedLocation())) {
            boolean hasUnassignedNode = false;
            for (int i = 0; i < currentNode.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) currentNode.getChildAt(i);
                if (child.getUserObject().equals("Unassigned")) {
                    hasUnassignedNode = true;
                    break;
                }
            }
            if (!hasUnassignedNode) {
                currentNode.add(new DefaultMutableTreeNode("Unassigned"));
            }
        }
    }

    private String buildPathFromNode(DefaultMutableTreeNode node) {
        if (node == null || node.isRoot()) {
            return null;
        }
        Object userObject = node.getUserObject();
        if (userObject.equals("Unassigned")) {
            // For "Unassigned" node, return the parent path
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            if (parentNode == null || parentNode.isRoot()) {
                return LogChargersTab.getUnassignedLocation();
            }
            javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(parentNode.getPath());
            Object[] nodes = path.getPath();
            StringBuilder fullPath = new StringBuilder();
            for (int i = 1; i < nodes.length; i++) { // Skip root
                if (i > 1) {
                    fullPath.append(LogChargersTab.getPathSeparator());
                }
                fullPath.append(nodes[i].toString());
            }
            return fullPath.toString();
        }
        // For non-"Unassigned" nodes, build the full path
        javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(node.getPath());
        Object[] nodes = path.getPath();
        StringBuilder fullPath = new StringBuilder();
        for (int i = 1; i < nodes.length; i++) { // Skip root
            if (i > 1) {
                fullPath.append(LogChargersTab.getPathSeparator());
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}