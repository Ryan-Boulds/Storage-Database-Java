package log_cables;

import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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

    public LogCablesTab() {
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
        newLocationButton.addActionListener(e -> new NewLocationDialog(this).showDialog());
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
                String location = (String) node.getUserObject();
                refreshTable(location);
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
        treePopup.add(deleteLocationItem);

        locationTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    TreePath path = locationTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        locationTree.setSelectionPath(path);
                        treePopup.show(locationTree, e.getX(), e.getY());
                    }
                }
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    TreePath path = locationTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        locationTree.setSelectionPath(path);
                        treePopup.show(locationTree, e.getX(), e.getY());
                    }
                }
            }
        });

        moveCableItem.addActionListener(new MoveCableAction(this));
        deleteCableItem.addActionListener(new DeleteCableAction(this));
        deleteLocationItem.addActionListener(new DeleteLocationAction(this));

        refreshTree();
    }

    public void refreshTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        Set<String> locations = CablesDAO.getAllLocations();
        for (String location : locations) {
            root.add(new DefaultMutableTreeNode(location));
        }
        locationTree.setModel(new DefaultTreeModel(root));
        for (int i = 0; i < locationTree.getRowCount(); i++) {
            locationTree.expandRow(i);
        }
    }

    public void refreshTable(String location) {
        try {
            // Update table columns based on location
            if (location.equals(UNASSIGNED_LOCATION)) {
                tableModel = new DefaultTableModel(new String[]{"Cable Type", "Count", "Previous Location"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            } else {
                tableModel = new DefaultTableModel(new String[]{"Cable Type", "Count"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            }
            cableTable.setModel(tableModel);

            tableModel.setRowCount(0);
            List<CablesDAO.CableEntry> cables = CablesDAO.getCablesByLocation(location);
            // Sort cables: non-zero counts first (alphabetically), then zero counts (alphabetically)
            cables.sort(Comparator.comparingInt((CablesDAO.CableEntry c) -> c.count == 0 ? 1 : 0)
                    .thenComparing(c -> c.cableType));
            for (CablesDAO.CableEntry cable : cables) {
                if (location.equals(UNASSIGNED_LOCATION)) {
                    tableModel.addRow(new Object[]{cable.cableType, cable.count, cable.previousLocation});
                } else {
                    tableModel.addRow(new Object[]{cable.cableType, cable.count});
                }
            }
            statusLabel.setText("Table refreshed for location: " + (location.equals(UNASSIGNED_LOCATION) ? UNASSIGNED_LOCATION : location));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing table: {0}", e.getMessage());
            setStatus("Error refreshing table: " + e.getMessage());
        }
    }

    public void refresh() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) locationTree.getLastSelectedPathComponent();
        String location = node != null ? (String) node.getUserObject() : UNASSIGNED_LOCATION;
        refreshTable(location);
        refreshTree();
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
}