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
        JMenuItem deleteLocationItem = new JMenuItem("Delete Location");
        deleteLocationItem.addActionListener(new DeleteLocationAction(this));
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
        JButton addCableButton = UIComponentUtils.createFormattedButton("Add New Cable Type");
        addCableButton.addActionListener(new AddCableAction(this));
        buttonPanel.add(addCableButton);

        JButton addToStorageButton = UIComponentUtils.createFormattedButton("Add to Storage");
        addToStorageButton.addActionListener(new AddToStorageAction(this));
        buttonPanel.add(addToStorageButton);

        JButton removeFromStorageButton = UIComponentUtils.createFormattedButton("Remove from Storage");
        removeFromStorageButton.addActionListener(new RemoveFromStorageAction(this));
        buttonPanel.add(removeFromStorageButton);

        JButton deleteCableButton = UIComponentUtils.createFormattedButton("Delete Cable");
        deleteCableButton.addActionListener(new DeleteCableAction(this));
        buttonPanel.add(deleteCableButton);

        JButton moveCableButton = UIComponentUtils.createFormattedButton("Move Cable");
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

    public void refreshTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        try {
            List<String> topLevelLocations = CablesDAO.getSubLocations(null);
            System.out.println("Top-level locations: " + topLevelLocations); // Debug output
            for (String location : topLevelLocations) {
                DefaultMutableTreeNode locationNode = new DefaultMutableTreeNode(location);
                List<String> subLocations = CablesDAO.getSubLocations(location);
                if (subLocations.isEmpty()) {
                    // Treat as leaf node if no children
                    root.add(locationNode);
                } else {
                    List<CablesDAO.CableEntry> directCables = CablesDAO.getCablesByLocation(location);
                    boolean hasDirectCables = false;
                    for (CablesDAO.CableEntry cable : directCables) {
                        if (!cable.cableType.startsWith("Placeholder_")) {
                            hasDirectCables = true;
                            break;
                        }
                    }
                    if (hasDirectCables) {
                        locationNode.add(new DefaultMutableTreeNode(UNASSIGNED_IN_LOCATION));
                    }
                    addSubLocations(locationNode, location);
                    root.add(locationNode);
                }
            }
            List<CablesDAO.CableEntry> unassignedCables = CablesDAO.getCablesByLocation(getUnassignedLocation());
            boolean hasUnassignedCables = false;
            for (CablesDAO.CableEntry cable : unassignedCables) {
                if (!cable.cableType.startsWith("Placeholder_")) {
                    hasUnassignedCables = true;
                    break;
                }
            }
            if (hasUnassignedCables) {
                root.add(new DefaultMutableTreeNode(getUnassignedLocation()));
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

    private void addSubLocations(DefaultMutableTreeNode parentNode, String parentLocation) throws SQLException {
        List<String> subLocations = CablesDAO.getSubLocations(parentLocation);
        for (String subLocation : subLocations) {
            DefaultMutableTreeNode locationNode = new DefaultMutableTreeNode(subLocation);
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
                locationNode.add(new DefaultMutableTreeNode(UNASSIGNED_IN_LOCATION));
            }
            addSubLocations(locationNode, subLocation);
            parentNode.add(locationNode);
        }
    }

    public void refreshTable(String location, boolean isUnassignedView) {
        try {
            List<CablesDAO.CableEntry> cables;
            if (isUnassignedView) {
                cables = CablesDAO.getCablesByLocation(location);
            } else {
                cables = CablesDAO.getCablesByLocation(location);
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
                if (location.equals(getUnassignedLocation())) {
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
            refreshTable(getUnassignedLocation(), false);
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

    public static String getUnassignedLocation() {
        return UNASSIGNED_LOCATION;
    }

    public static String getPathSeparator() {
        return PATH_SEPARATOR;
    }

    private String buildPathFromNode(DefaultMutableTreeNode node) {
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
                fullPath.append(getPathSeparator());
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}