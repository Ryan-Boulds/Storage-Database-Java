package accessories_count;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import accessories_count.actions.AddAccessoryAction;
import accessories_count.actions.AddToStorageAction;
import accessories_count.actions.DeleteAccessoryAction;
import accessories_count.actions.DeleteLocationAction;
import accessories_count.actions.MoveAccessoryAction;
import accessories_count.actions.NewLocationDialog;
import accessories_count.actions.RemoveFromStorageAction;
import utils.UIComponentUtils;

public final class LogAccessoriesTab extends JPanel {

    private final JTree locationTree;
    private final JTable accessoryTable;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;
    private static final Logger LOGGER = Logger.getLogger(LogAccessoriesTab.class.getName());
    private static final String UNASSIGNED_LOCATION = "Unassigned";
    private static final String UNASSIGNED_IN_LOCATION = "Unassigned";
    private static final String PATH_SEPARATOR = "/";
    public static final String DISPLAY_SEPARATOR = "/";
    private boolean isSummaryView = false;
    private final JButton addToStorageButton;
    private final JButton removeFromStorageButton;
    private final JButton deleteAccessoryButton;
    private final JButton moveAccessoryButton;
    private final JButton addAccessoryButton;
    private final JTextField searchField;
    private final JLabel titleLabel;
    private List<AccessoriesDAO.AccessoryEntry> currentAccessories; // To store the current Accessory list for filtering

    public LogAccessoriesTab() throws SQLException {
        setLayout(new BorderLayout(10, 10));

        statusLabel = UIComponentUtils.createAlignedLabel("");

        AccessoriesDAO.ensureSchema();
        AccessoriesDAO.cleanUpUnassignedSublocations();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.3);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel topLeftPanel = new JPanel(new BorderLayout());
        JLabel locationLabel = new JLabel("Locations:");
        JButton newLocationButton = UIComponentUtils.createFormattedButton("New Location");
        newLocationButton.addActionListener(e -> {
            new NewLocationDialog(this, null).showDialog();
            refreshTree();
        });
        topLeftPanel.add(locationLabel, BorderLayout.WEST);
        topLeftPanel.add(newLocationButton, BorderLayout.EAST);
        leftPanel.add(topLeftPanel, BorderLayout.NORTH);

        locationTree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("Root")));
        locationTree.setRootVisible(false);
        locationTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        locationTree.addTreeSelectionListener(e -> refresh());
        JScrollPane treeScrollPane = UIComponentUtils.createScrollableContentPanel(locationTree);
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);

        // Tree popup menu for right-click
        JPopupMenu treePopup = new JPopupMenu();
        JMenuItem addSubLocationItem = new JMenuItem("Add Sublocation");
        addSubLocationItem.addActionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
            if (selectedNode != null) {
                String parentPath = buildPathFromNode(selectedNode);
                new NewLocationDialog(this, parentPath).showDialog();
                refreshTree();
            }
        });
        treePopup.add(addSubLocationItem);
        JMenuItem deleteLocationItem = new JMenuItem("Delete Location");
        deleteLocationItem.addActionListener(new DeleteLocationAction(this));
        treePopup.add(deleteLocationItem);

        locationTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    int row = locationTree.getRowForLocation(evt.getX(), evt.getY());
                    if (row != -1) {
                        locationTree.setSelectionRow(row);
                        DefaultMutableTreeNode selected = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
                        String val = (String) selected.getUserObject();
                        if (!val.equals(UNASSIGNED_IN_LOCATION)) {
                            treePopup.show(evt.getComponent(), evt.getX(), evt.getY());
                        }
                    }
                }
            }
        });

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel with title, search bar, and buttons
        JPanel topRightPanel = new JPanel(new BorderLayout());
        titleLabel = UIComponentUtils.createAlignedLabel("Items in " + UNASSIGNED_LOCATION);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16)); // Bold and larger font
        topRightPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel searchAndButtonPanel = new JPanel(new BorderLayout());
        searchField = new JTextField(20);
        searchField.setBorder(BorderFactory.createTitledBorder("Search Accessory Type"));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterTable();
            }
        });
        searchAndButtonPanel.add(searchField, BorderLayout.CENTER);

        JPanel topButtonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        addAccessoryButton = UIComponentUtils.createFormattedButton("Add Accessory");
        addAccessoryButton.addActionListener(new AddAccessoryAction(this));
        moveAccessoryButton = UIComponentUtils.createFormattedButton("Move Accessories");
        moveAccessoryButton.addActionListener(new MoveAccessoryAction(this));
        deleteAccessoryButton = UIComponentUtils.createFormattedButton("Delete Accessory Type");
        deleteAccessoryButton.addActionListener(new DeleteAccessoryAction(this));
        topButtonPanel.add(addAccessoryButton);
        topButtonPanel.add(moveAccessoryButton);
        topButtonPanel.add(deleteAccessoryButton);
        searchAndButtonPanel.add(topButtonPanel, BorderLayout.EAST);
        topRightPanel.add(searchAndButtonPanel, BorderLayout.CENTER);

        // Table
        tableModel = new DefaultTableModel(new String[]{"Accessory Type", "Count"}, 0);
        accessoryTable = new JTable(tableModel);
        accessoryTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = UIComponentUtils.createScrollableContentPanel(accessoryTable);

        // Bottom button panel
        JPanel bottomButtonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        addToStorageButton = UIComponentUtils.createFormattedButton("Add to Storage (+1)");
        addToStorageButton.addActionListener(new AddToStorageAction(this));
        removeFromStorageButton = UIComponentUtils.createFormattedButton("Remove from Storage (-1)");
        removeFromStorageButton.addActionListener(new RemoveFromStorageAction(this));
        bottomButtonPanel.add(addToStorageButton);
        bottomButtonPanel.add(removeFromStorageButton);

        JPopupMenu tablePopup = new JPopupMenu();
        JMenuItem addToStorageItem = new JMenuItem("Add to Storage (+1)");
        addToStorageItem.addActionListener(new AddToStorageAction(this));
        tablePopup.add(addToStorageItem);
        JMenuItem removeFromStorageItem = new JMenuItem("Remove from Storage (-1)");
        removeFromStorageItem.addActionListener(new RemoveFromStorageAction(this));
        tablePopup.add(removeFromStorageItem);
        JMenuItem moveAccessoryItem = new JMenuItem("Move Accessories");
        moveAccessoryItem.addActionListener(new MoveAccessoryAction(this));
        tablePopup.add(moveAccessoryItem);
        JMenuItem deleteAccessoryItem = new JMenuItem("Delete Accessory Type");
        deleteAccessoryItem.addActionListener(new DeleteAccessoryAction(this));
        tablePopup.add(deleteAccessoryItem);

        accessoryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (java.awt.event.MouseEvent.BUTTON3 == evt.getButton() && !isSummaryView) {
                    int row = accessoryTable.rowAtPoint(evt.getPoint());
                    if (row >= 0) {
                        accessoryTable.setRowSelectionInterval(row, row);
                        tablePopup.show(evt.getComponent(), evt.getX(), evt.getY());
                    }
                }
            }
        });

        rightPanel.add(topRightPanel, BorderLayout.NORTH);
        rightPanel.add(tableScrollPane, BorderLayout.CENTER);
        rightPanel.add(bottomButtonPanel, BorderLayout.SOUTH);

        splitPane.add(leftPanel, JSplitPane.LEFT);
        splitPane.add(rightPanel, JSplitPane.RIGHT);

        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        refreshTree();
    }

    private void filterTable() {
        String searchText = searchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);
        if (currentAccessories != null) {
            for (AccessoriesDAO.AccessoryEntry accessory : currentAccessories) {
                if (searchText.isEmpty() || accessory.accessoryType.toLowerCase().contains(searchText)) {
                    tableModel.addRow(new Object[]{accessory.accessoryType, accessory.count});
                }
            }
        }
        statusLabel.setText("Filtered accessories for: " + (searchText.isEmpty() ? "All" : searchText));
    }

    private void buildTree() {
        try {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

            List<AccessoriesDAO.AccessoryEntry> unassignedAccessories = AccessoriesDAO.getAccessoriesByLocation(UNASSIGNED_LOCATION);
            boolean hasUnassignedAccessories = false;
            for (AccessoriesDAO.AccessoryEntry accessory : unassignedAccessories) {
                if (!accessory.accessoryType.startsWith("Placeholder_")) {
                    hasUnassignedAccessories = true;
                    break;
                }
            }
            if (hasUnassignedAccessories) {
                root.add(new DefaultMutableTreeNode(UNASSIGNED_LOCATION));
            }

            List<String> allLocations = AccessoriesDAO.getAllLocations();
            for (String location : allLocations) {
                if (!location.equals(UNASSIGNED_LOCATION)) {
                    addLocationToTree(root, location);
                }
            }

            DefaultTreeModel model = (DefaultTreeModel) locationTree.getModel();
            model.setRoot(root);
            for (int i = 0; i < locationTree.getRowCount(); i++) {
                locationTree.expandRow(i);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error building tree: {0}", e.getMessage());
            statusLabel.setText("Error building tree: " + e.getMessage());
        }
    }

    private void addLocationToTree(DefaultMutableTreeNode parentNode, String fullPath) throws SQLException {
        if (fullPath == null || fullPath.isEmpty()) {
            return;
        }

        String[] segments = fullPath.split(PATH_SEPARATOR);
        DefaultMutableTreeNode currentNode = parentNode;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (i > 0) {
                currentPath.append(PATH_SEPARATOR);
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
        List<AccessoriesDAO.AccessoryEntry> directAccessories = AccessoriesDAO.getAccessoriesByLocation(fullPathStr);
        List<String> subLocations = AccessoriesDAO.getSubLocations(fullPathStr);

        boolean hasDirectAccessories = false;
        for (AccessoriesDAO.AccessoryEntry accessory : directAccessories) {
            if (!accessory.accessoryType.startsWith("Placeholder_")) {
                hasDirectAccessories = true;
                break;
            }
        }

        if (hasDirectAccessories && !subLocations.isEmpty() && !fullPathStr.equals(UNASSIGNED_LOCATION)) {
            boolean hasUnassignedNode = false;
            for (int i = 0; i < currentNode.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) currentNode.getChildAt(i);
                if (child.getUserObject().equals(UNASSIGNED_IN_LOCATION)) {
                    hasUnassignedNode = true;
                    break;
                }
            }
            if (!hasUnassignedNode) {
                currentNode.add(new DefaultMutableTreeNode(UNASSIGNED_IN_LOCATION));
            }
        }
    }

    private void refreshTable(String location, boolean isSummary) {
        tableModel.setRowCount(0);
        isSummaryView = isSummary;
        try {
            if (isSummary) {
                currentAccessories = AccessoriesDAO.getAccessoriesSummary(location);
                titleLabel.setText("Summary of Accessories in " + location + "'s sub-locations");
            } else {
                currentAccessories = AccessoriesDAO.getAccessoriesByLocation(location);
                titleLabel.setText("Items in " + location);
            }
            currentAccessories.sort((c1, c2) -> {
                int cmp = Integer.compare(c2.count, c1.count);
                if (cmp == 0) {
                    cmp = c1.accessoryType.compareTo(c2.accessoryType);
                }
                return cmp;
            });
            filterTable(); // Apply filter after loading accessories
            statusLabel.setText("Table refreshed for location: " + location);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing table for location {0}: {1}", new Object[]{location, e.getMessage()});
            statusLabel.setText("Error refreshing table: " + e.getMessage());
        }
    }

    private void updateButtonStates() {
        boolean enabled = !isSummaryView;
        addAccessoryButton.setEnabled(enabled);
        addToStorageButton.setEnabled(enabled);
        removeFromStorageButton.setEnabled(enabled);
        deleteAccessoryButton.setEnabled(enabled);
        moveAccessoryButton.setEnabled(enabled);
        accessoryTable.setRowSelectionAllowed(enabled);
        // Search field remains enabled in all views
    }

    public void refresh() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
        if (node == null) {
            refreshTable(UNASSIGNED_LOCATION, false);
            return;
        }
        String nodeValue = (String) node.getUserObject();
        String fullPath;
        if (nodeValue.equals(UNASSIGNED_IN_LOCATION)) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            fullPath = buildPathFromNode(parentNode);
            if (fullPath == null) {
                fullPath = UNASSIGNED_LOCATION;
            }
        } else {
            fullPath = buildPathFromNode(node);
            if (fullPath == null) {
                fullPath = UNASSIGNED_LOCATION;
            }
        }
        fullPath = fullPath.replace(DISPLAY_SEPARATOR, PATH_SEPARATOR);
        boolean isSummary = node.getChildCount() > 0 && !nodeValue.equals(UNASSIGNED_IN_LOCATION);
        refreshTable(fullPath, isSummary);
        updateButtonStates();
    }

    public void refreshTree() {
        buildTree();
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    public JTable getAccessoryTable() {
        return accessoryTable;
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
        TreePath path = new TreePath(node.getPath());
        Object[] nodes = path.getPath();
        StringBuilder fullPath = new StringBuilder();
        for (int i = 1; i < nodes.length; i++) {
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