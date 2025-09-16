package log_adapters.actions;

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
import javax.swing.tree.TreePath;

import log_adapters.AdaptersDAO;
import log_adapters.LogAdaptersTab;
import utils.UIComponentUtils;

public class MoveAdapterAction implements ActionListener {
    private final LogAdaptersTab tab;

    public MoveAdapterAction(LogAdaptersTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (tab.isSummaryView()) {
            JOptionPane.showMessageDialog(tab, "Cannot perform this action in summary view", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int selectedRow = tab.getAdapterTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select an adapter to move");
            JOptionPane.showMessageDialog(null, "Please select an adapter to move", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String adapterType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        int currentCount = ((Number) tab.getTableModel().getValueAt(selectedRow, 1)).intValue();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tab.getLocationTree().getLastSelectedPathComponent();
        if (node == null) {
            tab.setStatus("Error: Select a location first");
            JOptionPane.showMessageDialog(null, "Please select a location first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String nodeValue = (String) node.getUserObject();
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
        String currentLocation;
        if (nodeValue.equals(LogAdaptersTab.getUnassignedLocation()) && parentNode instanceof DefaultMutableTreeNode && !parentNode.isRoot()) {
            currentLocation = tab.buildPathFromNode(parentNode).replace(LogAdaptersTab.DISPLAY_SEPARATOR, LogAdaptersTab.getPathSeparator());
        } else {
            currentLocation = tab.buildPathFromNode(node).replace(LogAdaptersTab.DISPLAY_SEPARATOR, LogAdaptersTab.getPathSeparator());
        }

        JDialog dialog = new JDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, tab), "Move Adapter", true);
        dialog.setSize(600, 400);
        dialog.setMinimumSize(new java.awt.Dimension(500, 300));
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        JLabel locationLabel = new JLabel("Select New Location:");
        treePanel.add(locationLabel, BorderLayout.NORTH);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        JTree locationTree;
        try {
            List<AdaptersDAO.AdapterEntry> unassignedAdapters = AdaptersDAO.getAdaptersByLocation(LogAdaptersTab.getUnassignedLocation());
            boolean hasUnassignedAdapters = false;
            for (AdaptersDAO.AdapterEntry adapter : unassignedAdapters) {
                if (!adapter.adapterType.startsWith("Placeholder_")) {
                    hasUnassignedAdapters = true;
                    break;
                }
            }
            if (hasUnassignedAdapters) {
                root.add(new DefaultMutableTreeNode(LogAdaptersTab.getUnassignedLocation()));
            }

            List<String> allLocations = AdaptersDAO.getAllLocations();
            for (String location : allLocations) {
                if (!location.equals(LogAdaptersTab.getUnassignedLocation())) {
                    addLocationToTree(root, location);
                }
            }
            locationTree = new JTree(new DefaultTreeModel(root));
            locationTree.setRootVisible(false);
            for (int i = 0; i < locationTree.getRowCount(); i++) {
                locationTree.expandRow(i);
            }
            JScrollPane treeScrollPane = UIComponentUtils.createScrollableContentPanel(locationTree);
            treePanel.add(treeScrollPane, BorderLayout.CENTER);
        } catch (SQLException ex) {
            tab.setStatus("Error loading locations for move: " + ex.getMessage());
            JOptionPane.showMessageDialog(dialog, "Error loading locations: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel quantityLabel = new JLabel("Quantity to Move (max " + currentCount + "):");
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
        JButton moveButton = UIComponentUtils.createFormattedButton("Move Adapter");
        moveButton.addActionListener(e1 -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
            if (selectedNode == null || selectedNode.isRoot()) {
                JOptionPane.showMessageDialog(dialog, "Please select a new location", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String newLocation = buildPathFromNode(selectedNode).replace(LogAdaptersTab.DISPLAY_SEPARATOR, LogAdaptersTab.getPathSeparator());
            if (newLocation == null || newLocation.equals(currentLocation)) {
                JOptionPane.showMessageDialog(dialog, "Please select a different location", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (newLocation.endsWith(LogAdaptersTab.getPathSeparator() + LogAdaptersTab.getUnassignedLocation())) {
                JOptionPane.showMessageDialog(dialog, "Cannot move to 'Unassigned' in a location. Please select a valid location.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String quantityText = quantityField.getText().trim();
            int quantity;
            try {
                quantity = Integer.parseInt(quantityText);
                if (quantity <= 0 || quantity > currentCount) {
                    JOptionPane.showMessageDialog(dialog, "Quantity must be between 1 and " + currentCount, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid quantity format", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                int adapterId = AdaptersDAO.getAdapterId(adapterType, currentLocation);
                if (adapterId == -1) {
                    tab.setStatus("Error: Adapter type '" + adapterType + "' not found at " + currentLocation);
                    JOptionPane.showMessageDialog(dialog, "Adapter not found", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                AdaptersDAO.moveAdapter(adapterId, newLocation, quantity);
                tab.setStatus("Successfully moved " + quantity + " " + adapterType + " to " + newLocation);
                tab.refreshTree();
                tab.refresh();
                dialog.dispose();
            } catch (SQLException ex) {
                tab.setStatus("Error moving adapter: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "Error moving adapter: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        inputPanel.add(moveButton, gbc);

        dialog.add(treePanel, BorderLayout.CENTER);
        dialog.add(inputPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(tab);
        dialog.setVisible(true);
    }

    private void addLocationToTree(DefaultMutableTreeNode parentNode, String fullPath) throws SQLException {
        if (fullPath == null || fullPath.isEmpty()) {
            return;
        }

        String[] segments = fullPath.split(LogAdaptersTab.getPathSeparator());
        DefaultMutableTreeNode currentNode = parentNode;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (i > 0) {
                currentPath.append(LogAdaptersTab.getPathSeparator());
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
        List<AdaptersDAO.AdapterEntry> directAdapters = AdaptersDAO.getAdaptersByLocation(fullPathStr);
        List<String> subLocations = AdaptersDAO.getSubLocations(fullPathStr);

        boolean hasDirectAdapters = false;
        for (AdaptersDAO.AdapterEntry adapter : directAdapters) {
            if (!adapter.adapterType.startsWith("Placeholder_")) {
                hasDirectAdapters = true;
                break;
            }
        }

        if (hasDirectAdapters && !subLocations.isEmpty() && !fullPathStr.equals(LogAdaptersTab.getUnassignedLocation())) {
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
        if (node.getUserObject().equals(LogAdaptersTab.getUnassignedLocation())) {
            return LogAdaptersTab.getUnassignedLocation();
        }
        TreePath path = new TreePath(node.getPath());
        Object[] nodes = path.getPath();
        StringBuilder fullPath = new StringBuilder();
        for (int i = 1; i < nodes.length; i++) {
            if (i > 1) {
                fullPath.append(LogAdaptersTab.DISPLAY_SEPARATOR);
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}