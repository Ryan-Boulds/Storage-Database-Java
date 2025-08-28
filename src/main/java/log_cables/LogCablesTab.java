package log_cables;

import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
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
    private DefaultTableModel tableModel;
    private final JLabel statusLabel;
    private static final Logger LOGGER = Logger.getLogger(LogCablesTab.class.getName());
    private static final String UNASSIGNED_LOCATION = "Unassigned";
    private static final String UNASSIGNED_IN_LOCATION = "Unassigned in this location";
    private static final String PATH_SEPARATOR = "--";

    public LogCablesTab() throws SQLException {
        setLayout(new BorderLayout(10, 10));

        statusLabel = UIComponentUtils.createAlignedLabel("");

        // Ensure schema
        CablesDAO.ensureSchema();

        // Split pane for tree and table
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.3);

        // Left: Location tree with new location button
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel topLeftPanel = new JPanel(new BorderLayout());
        JLabel locationLabel = new JLabel("Locations:");
        JButton newLocationButton = UIComponentUtils.createFormattedButton("New Location");
        newLocationButton.addActionListener(e -> new NewLocationDialog(this, null).showDialog());
        topLeftPanel.add(locationLabel, BorderLayout.WEST);
        topLeftPanel.add(newLocationButton, BorderLayout.EAST);
        leftPanel.add(topLeftPanel, BorderLayout.NORTH);
        locationTree = new JTree();
        locationTree.setRootVisible(false);
        JScrollPane treeScrollPane = new JScrollPane(locationTree);
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);

        // Right: Cable table and buttons
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel cablesLabel = new JLabel("Cables:");
        rightPanel.add(cablesLabel, BorderLayout.NORTH);
        tableModel = new DefaultTableModel(new String[]{"Cable Type", "Count"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cableTable = new JTable(tableModel);
        cableTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = new JScrollPane(cableTable);
        rightPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton addCableButton = UIComponentUtils.createFormattedButton("Add New Cable Type");
        JButton addToStorageButton = UIComponentUtils.createFormattedButton("Add to Storage");
        JButton removeFromStorageButton = UIComponentUtils.createFormattedButton("Remove from Storage");

        addCableButton.addActionListener(new AddCableAction(this));
        addToStorageButton.addActionListener(new AddToStorageAction(this));
        removeFromStorageButton.addActionListener(new RemoveFromStorageAction(this));

        buttonPanel.add(addCableButton);
        buttonPanel.add(addToStorageButton);
        buttonPanel.add(removeFromStorageButton);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Tree selection listener
        locationTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
            if (node != null) {
                String nodeValue = (String) node.getUserObject();
                if (nodeValue.equals(UNASSIGNED_IN_LOCATION)) {
                    // Get the parent node's full path
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                    String parentPath = buildPathFromNode(parentNode);
                    refreshTable(parentPath, true);
                } else {
                    String fullPath = buildPathFromNode(node);
                    refreshTable(fullPath, false);
                }
            }
        });

        // Right-click menu for table
        JPopupMenu tablePopup = new JPopupMenu();
        JMenuItem moveCableItem = new JMenuItem("Move Cable");
        JMenuItem deleteCableItem = new JMenuItem("Delete Cable");
        tablePopup.add(moveCableItem);
        tablePopup.add(deleteCableItem);

        cableTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = cableTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        cableTable.setRowSelectionInterval(row, row);
                        tablePopup.show(cableTable, e.getX(), e.getY());
                    }
                }
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = cableTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        cableTable.setRowSelectionInterval(row, row);
                        tablePopup.show(cableTable, e.getX(), e.getY());
                    }
                }
            }
        });

        // Right-click menu for location tree
        JPopupMenu treePopup = new JPopupMenu();
        JMenuItem deleteLocationItem = new JMenuItem("Delete Location");
        JMenuItem addSubCategoryItem = new JMenuItem("Add Sub-Category");
        treePopup.add(deleteLocationItem);
        treePopup.add(addSubCategoryItem);

        locationTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    TreePath path = locationTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        locationTree.setSelectionPath(path);
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node != null && !node.getUserObject().equals(UNASSIGNED_IN_LOCATION)) {
                            treePopup.show(locationTree, e.getX(), e.getY());
                        }
                    }
                }
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    TreePath path = locationTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        locationTree.setSelectionPath(path);
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node != null && !node.getUserObject().equals(UNASSIGNED_IN_LOCATION)) {
                            treePopup.show(locationTree, e.getX(), e.getY());
                        }
                    }
                }
            }
        });

        deleteLocationItem.addActionListener(new DeleteLocationAction(this));
        addSubCategoryItem.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
            if (node != null) {
                String parentPath = buildPathFromNode(node);
                parentPath = normalizePath(parentPath); // Normalize the parent path
                new NewLocationDialog(this, parentPath).showDialog();
            }
        });

        moveCableItem.addActionListener(new MoveCableAction(this));
        deleteCableItem.addActionListener(new DeleteCableAction(this));

        refreshTree();
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
                fullPath.append(PATH_SEPARATOR);
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }

    public void refreshTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        try {
            // Get top-level locations
            List<String> topLevelLocations = CablesDAO.getSubLocations(null);
            for (String location : topLevelLocations) {
                String normalizedLocation = normalizePath(location);
                DefaultMutableTreeNode locationNode = new DefaultMutableTreeNode(getLastSegment(normalizedLocation));
                // Add "Unassigned in this location" if there are direct cables and sub-locations
                List<String> subLocations = CablesDAO.getSubLocations(normalizedLocation);
                if (!subLocations.isEmpty()) {
                    List<CablesDAO.CableEntry> directCables = CablesDAO.getCablesByLocation(normalizedLocation);
                    if (!directCables.isEmpty()) {
                        locationNode.add(new DefaultMutableTreeNode(UNASSIGNED_IN_LOCATION));
                    }
                }
                // Build sub-tree
                buildLocationTree(locationNode, normalizedLocation);
                root.add(locationNode);
            }
            // Add Unassigned location
            DefaultMutableTreeNode unassignedNode = new DefaultMutableTreeNode(UNASSIGNED_LOCATION);
            root.add(unassignedNode);
            locationTree.setModel(new DefaultTreeModel(root));
            for (int i = 0; i < locationTree.getRowCount(); i++) {
                locationTree.expandRow(i);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing tree: {0}", e.getMessage());
            setStatus("Error refreshing tree: " + e.getMessage());
        }
    }

    private void buildLocationTree(DefaultMutableTreeNode parentNode, String parentPath) throws SQLException {
        List<String> subLocations = CablesDAO.getSubLocations(parentPath);
        for (String subLocation : subLocations) {
            String normalizedSubLocation = normalizePath(subLocation);
            String lastSegment = getLastSegment(normalizedSubLocation);
            DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(lastSegment);
            // Add "Unassigned in this location" if there are direct cables and sub-locations
            List<String> subSubLocations = CablesDAO.getSubLocations(normalizedSubLocation);
            if (!subSubLocations.isEmpty()) {
                List<CablesDAO.CableEntry> directCables = CablesDAO.getCablesByLocation(normalizedSubLocation);
                if (!directCables.isEmpty()) {
                    subNode.add(new DefaultMutableTreeNode(UNASSIGNED_IN_LOCATION));
                }
            }
            // Recursively build sub-tree
            buildLocationTree(subNode, normalizedSubLocation);
            parentNode.add(subNode);
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String[] segments = path.split(PATH_SEPARATOR);
        LinkedHashSet<String> uniqueSegments = new LinkedHashSet<>();
        for (String segment : segments) {
            if (!segment.isEmpty()) {
                uniqueSegments.add(segment);
            }
        }
        return String.join(PATH_SEPARATOR, uniqueSegments);
    }

    private String getLastSegment(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String[] segments = path.split(PATH_SEPARATOR);
        return segments[segments.length - 1];
    }

    public void refreshTable(String location, boolean isUnassignedView) {
        try {
            List<CablesDAO.CableEntry> cables;
            if (isUnassignedView || !CablesDAO.getSubLocations(location).isEmpty()) {
                // Parent location or Unassigned in this location: show aggregated counts
                tableModel = new DefaultTableModel(new String[]{"Cable Type", "Total Count"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
                cables = CablesDAO.getCablesByParentLocation(location);
            } else {
                // Leaf location or Unassigned: show direct counts
                tableModel = new DefaultTableModel(
                        location.equals(UNASSIGNED_LOCATION) ? new String[]{"Cable Type", "Count", "Previous Location"} : new String[]{"Cable Type", "Count"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
                cables = CablesDAO.getCablesByLocation(location);
            }
            cableTable.setModel(tableModel);

            tableModel.setRowCount(0);
            // Sort cables: non-zero counts first (alphabetically), then zero counts (alphabetically)
            cables.sort(java.util.Comparator.comparingInt((CablesDAO.CableEntry c) -> c.count == 0 ? 1 : 0)
                    .thenComparing(c -> c.cableType));
            for (CablesDAO.CableEntry cable : cables) {
                if (cable.cableType.startsWith("Placeholder_")) {
                    continue;
                }
                if (location.equals(UNASSIGNED_LOCATION)) {
                    tableModel.addRow(new Object[]{cable.cableType, cable.count, cable.previousLocation});
                } else {
                    tableModel.addRow(new Object[]{cable.cableType, cable.count});
                }
            }
            statusLabel.setText("Table refreshed for: " + (isUnassignedView ? location + " (Unassigned)" : location));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing table: {0}", e.getMessage());
            setStatus("Error refreshing table: " + e.getMessage());
        }
    }

    public void refresh() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
        if (node == null) {
            refreshTable(UNASSIGNED_LOCATION, false);
        } else {
            String nodeValue = (String) node.getUserObject();
            if (nodeValue.equals(UNASSIGNED_IN_LOCATION)) {
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                String parentPath = buildPathFromNode(parentNode);
                refreshTable(parentPath, true);
            } else {
                String fullPath = buildPathFromNode(node);
                refreshTable(fullPath, false);
            }
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

    public String getUnassignedLocation() {
        return UNASSIGNED_LOCATION;
    }

    public static String getPathSeparator() {
        return PATH_SEPARATOR;
    }
}