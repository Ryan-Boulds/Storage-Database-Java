package log_cables;

import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import log_cables.actions.AddCableAction;
import log_cables.actions.AddToStorageAction;
import log_cables.actions.DeleteCableAction;
import log_cables.actions.DeleteLocationAction;
import log_cables.actions.MoveCableAction;
import log_cables.actions.NewLocationDialog;
import log_cables.actions.RemoveFromStorageAction;
import utils.UIComponentUtils;

public final class LogCablesTab extends JPanel {

    private final JTree locationTree;
    private final JTable cableTable;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;
    private static final Logger LOGGER = Logger.getLogger(LogCablesTab.class.getName());
    private static final String UNASSIGNED_LOCATION = "Unassigned";
    private static final String UNASSIGNED_IN_LOCATION = "Unassigned";
    private static final String PATH_SEPARATOR = "/";
    public static final String DISPLAY_SEPARATOR = "/";
    private boolean isSummaryView = false;
    private final JButton addToStorageButton;
    private final JButton removeFromStorageButton;
    private final JButton deleteCableButton;
    private final JButton moveCableButton;
    private final JButton addCableButton;

    public LogCablesTab() throws SQLException {
        setLayout(new BorderLayout(10, 10));

        statusLabel = UIComponentUtils.createAlignedLabel("");

        // Ensure schema and clean up unassigned sublocations
        CablesDAO.ensureSchema();
        CablesDAO.cleanUpUnassignedSublocations();

        // Split pane for tree and table
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.3);

        // Left: Location tree with new location button
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel topLeftPanel = new JPanel(new BorderLayout());
        JLabel locationLabel = new JLabel("Locations:");
        JButton newLocationButton = UIComponentUtils.createFormattedButton("New Location");
        newLocationButton.addActionListener(e -> {
            new NewLocationDialog(this, null).showDialog();
            refreshTree(); // Force refresh after adding location
        });
        topLeftPanel.add(locationLabel, BorderLayout.WEST);
        topLeftPanel.add(newLocationButton, BorderLayout.EAST);
        leftPanel.add(topLeftPanel, BorderLayout.NORTH);

        locationTree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("Root")));
        locationTree.setRootVisible(false);
        JScrollPane treeScrollPane = new JScrollPane(locationTree);
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);
        JPopupMenu locationPopup = new JPopupMenu();
        JMenuItem addSublocationItem = new JMenuItem("Add Sublocation");
        addSublocationItem.addActionListener(e -> handleAddSublocation());
        JMenuItem deleteLocationItem = new JMenuItem("Delete Location");
        deleteLocationItem.addActionListener(new DeleteLocationAction(this));
        locationPopup.add(addSublocationItem);
        locationPopup.add(deleteLocationItem);
        locationTree.setComponentPopupMenu(locationPopup);

        // Right: Cable table with buttons
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableModel = new DefaultTableModel(new Object[]{"Cable Type", "Count"}, 0);
        cableTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(cableTable);
        rightPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        addCableButton = UIComponentUtils.createFormattedButton("Add Cable");
        addCableButton.addActionListener(new AddCableAction(this));
        buttonPanel.add(addCableButton);

        addToStorageButton = UIComponentUtils.createFormattedButton("Add to Storage");
        addToStorageButton.addActionListener(new AddToStorageAction(this));
        buttonPanel.add(addToStorageButton);

        removeFromStorageButton = UIComponentUtils.createFormattedButton("Remove from Storage");
        removeFromStorageButton.addActionListener(new RemoveFromStorageAction(this));
        buttonPanel.add(removeFromStorageButton);

        deleteCableButton = UIComponentUtils.createFormattedButton("Delete Cable");
        deleteCableButton.addActionListener(new DeleteCableAction(this));
        buttonPanel.add(deleteCableButton);

        moveCableButton = UIComponentUtils.createFormattedButton("Move Cable");
        moveCableButton.addActionListener(new MoveCableAction(this));
        buttonPanel.add(moveCableButton);

        JPopupMenu cablePopup = new JPopupMenu();
        JMenuItem moveCablePopupItem = new JMenuItem("Move Cable");
        moveCablePopupItem.addActionListener(new MoveCableAction(this));
        cablePopup.add(moveCablePopupItem);
        cableTable.setComponentPopupMenu(cablePopup);

        rightPanel.add(buttonPanel, BorderLayout.SOUTH);
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        refreshTree();
        locationTree.addTreeSelectionListener(e -> refresh());
        refresh();
    }

    private void handleAddSublocation() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
        if (selectedNode == null || selectedNode.isRoot() || selectedNode.getUserObject().equals(UNASSIGNED_IN_LOCATION)) {
            JOptionPane.showMessageDialog(this, "Please select a valid location", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String parentLocation = buildPathFromNode(selectedNode).replace(DISPLAY_SEPARATOR, PATH_SEPARATOR);
        String sublocationInput = JOptionPane.showInputDialog(this, "Enter sublocation path (use '/' for hierarchy, e.g., Office/Scotts_Desk):");
        if (sublocationInput == null || sublocationInput.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Sublocation path cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        sublocationInput = sublocationInput.trim();

        // Split input by forward slash to handle nested sublocations
        String[] segments = sublocationInput.split("/");
        if (segments.length == 0) {
            JOptionPane.showMessageDialog(this, "Invalid sublocation path", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check for "Unassigned" in the input
        if (java.util.Arrays.stream(segments).anyMatch(s -> s.equalsIgnoreCase(UNASSIGNED_LOCATION))) {
            JOptionPane.showMessageDialog(this, "Sublocation name cannot contain 'Unassigned'", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String currentPath = parentLocation.equals(UNASSIGNED_LOCATION) ? "" : parentLocation;
            List<String> existingLocations = CablesDAO.getAllLocations();

            // Process each segment to create nested locations
            for (String segment : segments) {
                if (segment.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Sublocation name cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (segment.contains(PATH_SEPARATOR)) {
                    JOptionPane.showMessageDialog(this, "Sublocation name cannot contain '" + PATH_SEPARATOR + "'", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!currentPath.isEmpty()) {
                    currentPath += PATH_SEPARATOR;
                }
                currentPath += segment;

                // Create location if it doesn't exist
                if (!existingLocations.contains(currentPath)) {
                    CablesDAO.createLocation(currentPath, currentPath.substring(0, currentPath.lastIndexOf(PATH_SEPARATOR)));
                }
            }
            setStatus("Added sublocation: " + currentPath.replace(PATH_SEPARATOR, DISPLAY_SEPARATOR));
            refreshTree();
            refreshTable(currentPath, false);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding sublocation: {0}", e.getMessage());
            setStatus("Error adding sublocation: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error adding sublocation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        try {
            // Add top-level locations, excluding "Unassigned" initially
            List<String> topLevelLocations = CablesDAO.getSubLocations(null);
            for (String location : topLevelLocations) {
                if (!location.equals(UNASSIGNED_LOCATION)) {
                    addLocationToTree(root, location);
                }
            }

            // Add top-level "Unassigned" node only if it has non-placeholder cables
            List<CablesDAO.CableEntry> unassignedCables = CablesDAO.getCablesByLocation(UNASSIGNED_LOCATION);
            boolean hasUnassignedCables = false;
            for (CablesDAO.CableEntry cable : unassignedCables) {
                if (!cable.cableType.startsWith("Placeholder_")) {
                    hasUnassignedCables = true;
                    break;
                }
            }
            if (hasUnassignedCables) {
                DefaultMutableTreeNode unassignedNode = new DefaultMutableTreeNode(UNASSIGNED_LOCATION);
                root.add(unassignedNode);
            }

            locationTree.setModel(new DefaultTreeModel(root));
            for (int i = 0; i < locationTree.getRowCount(); i++) {
                locationTree.expandRow(i);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing tree: {0}", e.getMessage());
            setStatus("Error refreshing tree: " + e.getMessage());
        }
    }

    private void addLocationToTree(DefaultMutableTreeNode parentNode, String location) throws SQLException {
        // Skip if location is empty or the top-level "Unassigned" (handled in refreshTree)
        if (location == null || location.isEmpty() || location.equals(UNASSIGNED_LOCATION)) {
            return;
        }

        String[] segments = location.split(PATH_SEPARATOR);
        DefaultMutableTreeNode currentNode = parentNode;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (i > 0) {
                currentPath.append(PATH_SEPARATOR);
            }
            currentPath.append(segment);

            // Skip adding "Unassigned" as a segment unless it's the full path
            if (segment.equals(UNASSIGNED_LOCATION) && !currentPath.toString().equals(UNASSIGNED_LOCATION)) {
                continue;
            }

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

        String fullPath = currentPath.toString();
        List<CablesDAO.CableEntry> directCables = CablesDAO.getCablesByLocation(fullPath);
        boolean hasDirectCables = false;
        for (CablesDAO.CableEntry cable : directCables) {
            if (!cable.cableType.startsWith("Placeholder_")) {
                hasDirectCables = true;
                break;
            }
        }
        List<String> subLocations = CablesDAO.getSubLocations(fullPath);

        // Check if "Unassigned" node already exists to avoid duplicates
        boolean hasUnassignedNode = false;
        for (int i = 0; i < currentNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) currentNode.getChildAt(i);
            if (child.getUserObject().equals(UNASSIGNED_IN_LOCATION)) {
                hasUnassignedNode = true;
                break;
            }
        }

        // Add "Unassigned" node only if it doesn’t exist, there are direct cables, and the location is not top-level Unassigned
        if (!hasUnassignedNode && hasDirectCables && !fullPath.equals(UNASSIGNED_LOCATION)) {
            currentNode.add(new DefaultMutableTreeNode(UNASSIGNED_IN_LOCATION));
        }

        // Recursively add sublocations, skipping top-level "Unassigned"
        for (String subLocation : subLocations) {
            if (!subLocation.equals(UNASSIGNED_LOCATION)) {
                addLocationToTree(parentNode, subLocation);
            }
        }
    }

    public void refreshTable(String location, boolean isSummary) {
        try {
            this.isSummaryView = isSummary;
            updateButtonStates();

            List<CablesDAO.CableEntry> cables;
            if (isSummary) {
                cables = CablesDAO.getCablesByParentLocation(location);
                statusLabel.setText("Summary for " + location.replace(PATH_SEPARATOR, DISPLAY_SEPARATOR) + " and sublocations");
            } else if (location.endsWith(PATH_SEPARATOR + UNASSIGNED_IN_LOCATION)) {
                // Handle "Unassigned" node by getting cables directly at the parent location
                String parentPath = location.substring(0, location.lastIndexOf(PATH_SEPARATOR + UNASSIGNED_IN_LOCATION));
                parentPath = parentPath.replace(DISPLAY_SEPARATOR, PATH_SEPARATOR);
                cables = CablesDAO.getCablesByLocation(parentPath);
                statusLabel.setText("Unassigned cables in " + parentPath.replace(PATH_SEPARATOR, DISPLAY_SEPARATOR));
            } else {
                cables = CablesDAO.getCablesByLocation(location);
                statusLabel.setText("Cables in " + location.replace(PATH_SEPARATOR, DISPLAY_SEPARATOR));
            }
            cableTable.setModel(tableModel);

            tableModel.setRowCount(0);
            // Sort cables: non-zero counts first (alphabetically), then zero counts (alphabetically)
            cables.sort(java.util.Comparator.comparingInt((CablesDAO.CableEntry c) -> c.count == 0 ? 1 : 0)
                    .thenComparing(c -> c.cableType));
            for (CablesDAO.CableEntry cable : cables) {
                if (cable.cableType.startsWith("Placeholder_")) {
                    continue; // Skip placeholder entries
                }
                tableModel.addRow(new Object[]{cable.cableType, cable.count});
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing table: {0}", e.getMessage());
            setStatus("Error refreshing table: " + e.getMessage());
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.log(Level.SEVERE, "Initialization error: {0}", e.getMessage());
            setStatus("Error: Invalid location path - " + e.getMessage());
        }
    }

    private void updateButtonStates() {
        boolean enabled = !isSummaryView;
        addCableButton.setEnabled(enabled);
        addToStorageButton.setEnabled(enabled);
        removeFromStorageButton.setEnabled(enabled);
        deleteCableButton.setEnabled(enabled);
        moveCableButton.setEnabled(enabled);
        cableTable.setRowSelectionAllowed(enabled);
    }

    public void refresh() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
        if (node == null) {
            refreshTable(getUnassignedLocation(), false);
            return;
        }
        String nodeValue = (String) node.getUserObject();
        if (nodeValue.equals(UNASSIGNED_IN_LOCATION)) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            String parentPath = buildPathFromNode(parentNode).replace(DISPLAY_SEPARATOR, PATH_SEPARATOR);
            refreshTable(parentPath + PATH_SEPARATOR + UNASSIGNED_IN_LOCATION, false);
        } else {
            String fullPath = buildPathFromNode(node).replace(DISPLAY_SEPARATOR, PATH_SEPARATOR);
            boolean isSummary = node.getChildCount() > 0;
            refreshTable(fullPath, isSummary);
        }
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    public JTable getCableTable() {
        return cableTable;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTree getLocationTree() {
        return locationTree;
    }

    public static String getUnassignedLocation() {
        return UNASSIGNED_LOCATION;
    }

    public static String getPathSeparator() {
        return PATH_SEPARATOR;
    }

    public String buildPathFromNode(DefaultMutableTreeNode node) {
        if (node == null || node.isRoot()) {
            return null;
        }
        if (node.getUserObject().equals(UNASSIGNED_LOCATION)) {
            return getUnassignedLocation();
        }
        TreePath path = new TreePath(node.getPath());
        Object[] nodes = path.getPath();
        StringBuilder fullPath = new StringBuilder();
        for (int i = 1; i < nodes.length; i++) { // Skip root
            if (i > 1) {
                fullPath.append(DISPLAY_SEPARATOR);
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }

    public boolean isSummaryView() {
        return isSummaryView;
    }
}