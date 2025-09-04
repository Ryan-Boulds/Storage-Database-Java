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
import javax.swing.tree.TreePath;

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

        // Build location tree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        try {
            // Add Unassigned location
            root.add(new DefaultMutableTreeNode(LogCablesTab.getUnassignedLocation()));

            // Add top-level locations and their sub-locations
            List<String> topLevelLocations = CablesDAO.getSubLocations(null);
            for (String location : topLevelLocations) {
                DefaultMutableTreeNode locationNode = new DefaultMutableTreeNode(location);
                addSubLocations(locationNode, location);
                root.add(locationNode);
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

    private void addSubLocations(DefaultMutableTreeNode parentNode, String parentLocation) throws SQLException {
        List<String> subLocations = CablesDAO.getSubLocations(parentLocation);
        for (String subLocation : subLocations) {
            DefaultMutableTreeNode locationNode = new DefaultMutableTreeNode(subLocation);
            addSubLocations(locationNode, subLocation);
            parentNode.add(locationNode);
        }
    }

    private String buildPathFromNode(DefaultMutableTreeNode node) {
        if (node == null || node.isRoot()) {
            return null;
        }
        String nodeValue = (String) node.getUserObject();
        if (nodeValue.equals(LogCablesTab.getUnassignedLocation())) {
            return LogCablesTab.getUnassignedLocation();
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