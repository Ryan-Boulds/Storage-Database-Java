package log_cables.actions;

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

import log_cables.CablesDAO;
import log_cables.LogCablesTab;

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

            List<String> topLevelLocations = CablesDAO.getSubLocations(null);
            for (String location : topLevelLocations) {
                if (location.equals(LogCablesTab.getUnassignedLocation())) continue;
                addLocationToTree(root, location);
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