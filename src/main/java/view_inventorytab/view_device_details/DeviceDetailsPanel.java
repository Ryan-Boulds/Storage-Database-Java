package view_inventorytab.view_device_details;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;

import utils.DatabaseUtils;
import utils.TablesNotIncludedList;
import utils.UIComponentUtils;
import view_inventorytab.ModifyDialog;
import view_inventorytab.TableManager;
import view_inventorytab.ViewInventoryTab;

public class DeviceDetailsPanel extends JPanel {
    private final String assetName;
    private final ViewInventoryTab parentTab;
    private final TableManager tableManager;
    private static final int CONTENT_WIDTH = 800; // Fixed width for content
    private static final int SPECS_HEIGHT = 200; // Fixed height for Inventory specs
    private static final int LIST_HEIGHT = 400; // Fixed height for list
    private static final int LIST_WIDTH = 200; // Width for table list
    private static final Logger LOGGER = Logger.getLogger(DeviceDetailsPanel.class.getName());
    private JList<String> tableList;
    private JPanel dataListPanel;
    private JTextArea inventorySpecsArea; // Store as field for refresh

    public DeviceDetailsPanel(String assetName, ViewInventoryTab parentTab) {
        this.assetName = assetName;
        this.parentTab = parentTab;
        this.tableManager = parentTab.getTableManager();
        setLayout(new BorderLayout(10, 10));
        initializeComponents();
    }

    private void initializeComponents() {
        // Title with Asset Name
        JLabel titleLabel = UIComponentUtils.createAlignedLabel("Device Details: " + assetName);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        // Back button
        JButton backButton = UIComponentUtils.createFormattedButton("Back");
        backButton.addActionListener(e -> parentTab.showMainView());

        // Edit button
        JButton editButton = UIComponentUtils.createFormattedButton("Edit");
        editButton.addActionListener(e -> {
            try {
                HashMap<String, String> device = DatabaseUtils.getDeviceByAssetName("Inventory", assetName);
                if (device != null && !device.isEmpty()) {
                    ModifyDialog.showModifyDialog(
                        (javax.swing.JFrame) javax.swing.SwingUtilities.getWindowAncestor(this),
                        device,
                        tableManager
                    );
                    // Check if device still exists after dialog
                    HashMap<String, String> updatedDevice = DatabaseUtils.getDeviceByAssetName("Inventory", assetName);
                    if (updatedDevice == null || updatedDevice.isEmpty()) {
                        parentTab.showMainView();
                    } else {
                        parentTab.refreshDataAndTabs();
                        refreshDetails();
                    }
                } else {
                    javax.swing.JOptionPane.showMessageDialog(
                        this,
                        "Error: Device not found for AssetName: " + assetName,
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                    );
                    LOGGER.log(Level.SEVERE, "Device not found for AssetName: {0}", assetName);
                }
            } catch (SQLException ex) {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Error retrieving device data: " + ex.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                );
                LOGGER.log(Level.SEVERE, "Error retrieving device data for {0}: {1}", new Object[]{assetName, ex.getMessage()});
            }
        });

        // Top panel with title, back button, and edit button
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        buttonPanel.add(backButton);
        buttonPanel.add(editButton);
        topPanel.add(buttonPanel, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Content panel for Inventory specs and table selection
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setPreferredSize(new Dimension(CONTENT_WIDTH, SPECS_HEIGHT + LIST_HEIGHT + 50));
        contentPanel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

        // Inventory specs display
        inventorySpecsArea = createInventorySpecsArea();
        contentPanel.add(inventorySpecsArea);
        contentPanel.add(Box.createVerticalStrut(10)); // Space before separator

        // Separator
        JLabel separatorLabel = new JLabel("Software Details");
        separatorLabel.setFont(new Font("Arial", Font.BOLD, 14));
        separatorLabel.setAlignmentX(LEFT_ALIGNMENT);
        contentPanel.add(separatorLabel);
        contentPanel.add(Box.createVerticalStrut(10)); // Space after separator

        // Split pane for table list and data list
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(LIST_WIDTH); // Initial divider position
        splitPane.setDividerSize(5); // Visible divider
        splitPane.setResizeWeight(0.25); // Allocate 25% of space to left panel
        splitPane.setPreferredSize(new Dimension(CONTENT_WIDTH, LIST_HEIGHT));
        splitPane.setMinimumSize(new Dimension(LIST_WIDTH + 100, LIST_HEIGHT)); // Ensure minimum width

        // Left panel: List of tables
        tableList = createTableList();
        JScrollPane listScrollPane = new JScrollPane(tableList);
        listScrollPane.setPreferredSize(new Dimension(LIST_WIDTH, LIST_HEIGHT));
        listScrollPane.setMinimumSize(new Dimension(LIST_WIDTH, LIST_HEIGHT)); // Prevent collapse
        splitPane.setLeftComponent(listScrollPane);

        // Right panel: Data list
        dataListPanel = new JPanel();
        dataListPanel.setLayout(new BoxLayout(dataListPanel, BoxLayout.Y_AXIS));
        dataListPanel.setPreferredSize(new Dimension(CONTENT_WIDTH - LIST_WIDTH - 5, LIST_HEIGHT));
        dataListPanel.setMaximumSize(new Dimension(CONTENT_WIDTH - LIST_WIDTH - 5, LIST_HEIGHT));
        Border border = new LineBorder(Color.GRAY, 1); // Border for data list
        dataListPanel.setBorder(border);
        JScrollPane dataScrollPane = new JScrollPane(dataListPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dataScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        dataScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        splitPane.setRightComponent(dataScrollPane);

        contentPanel.add(splitPane);

        // Single scroll pane for all content
        JScrollPane contentScrollPane = new JScrollPane(contentPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        contentScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        add(contentScrollPane, BorderLayout.CENTER);
    }

    private JTextArea createInventorySpecsArea() {
        JTextArea specsArea = new JTextArea();
        specsArea.setEditable(false);
        specsArea.setFont(new Font("Arial", Font.PLAIN, 12));
        specsArea.setPreferredSize(new Dimension(CONTENT_WIDTH, SPECS_HEIGHT));
        specsArea.setMaximumSize(new Dimension(CONTENT_WIDTH, SPECS_HEIGHT));
        specsArea.setLineWrap(true);
        specsArea.setWrapStyleWord(true);
        updateInventorySpecsArea(specsArea);
        return specsArea;
    }

    private void updateInventorySpecsArea(JTextArea specsArea) {
        StringBuilder specs = new StringBuilder();
        try {
            HashMap<String, String> inventoryEntry = DatabaseUtils.getDeviceByAssetName("Inventory", assetName);
            if (inventoryEntry != null && !inventoryEntry.isEmpty()) {
                for (String column : inventoryEntry.keySet()) {
                    String value = inventoryEntry.get(column);
                    if (value != null && !value.trim().isEmpty()) {
                        specs.append(column).append(": ").append(value).append("\n");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying Inventory table: {0}", e.getMessage());
            specs.append("Error loading specifications: ").append(e.getMessage());
        }
        specsArea.setText(specs.length() > 0 ? specs.toString() : "No specifications available");
    }

    private JList<String> createTableList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(new Font("Arial", Font.PLAIN, 12));
        list.setFixedCellWidth(LIST_WIDTH - 20); // Account for scroll pane borders
        list.setFixedCellHeight(25);

        // Get all table names and filter out excluded ones
        List<String> tableNames;
        try {
            tableNames = DatabaseUtils.getTableNames();
            List<String> excludedTables = TablesNotIncludedList.getExcludedTablesForSoftwareImporter();
            LOGGER.log(Level.INFO, "Retrieved table names: {0}, Excluded tables: {1}", new Object[]{tableNames, excludedTables});
            for (String tableName : tableNames) {
                if (!excludedTables.contains(tableName)) {
                    listModel.addElement(tableName);
                }
            }
            if (listModel.isEmpty()) {
                listModel.addElement("No tables available");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving table names: {0}", e.getMessage());
            listModel.addElement("Error: " + e.getMessage());
        }

        // Add selection listener to update data list
        list.addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = list.getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    updateDataList(selectedTable);
                }
            }
        });

        return list;
    }

    private void updateDataList(String tableName) {
        dataListPanel.removeAll();

        // Get Inventory columns with non-empty values
        Set<String> inventoryNonEmptyColumns = new HashSet<>();
        try {
            HashMap<String, String> inventoryEntry = DatabaseUtils.getDeviceByAssetName("Inventory", assetName);
            if (inventoryEntry != null && !inventoryEntry.isEmpty()) {
                for (String column : inventoryEntry.keySet()) {
                    String value = inventoryEntry.get(column);
                    if (value != null && !value.trim().isEmpty()) {
                        inventoryNonEmptyColumns.add(column);
                    }
                }
                LOGGER.log(Level.INFO, "Inventory columns with non-empty values: {0}", inventoryNonEmptyColumns);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying Inventory table: {0}", e.getMessage());
            JLabel errorLabel = new JLabel("Error loading data: " + e.getMessage());
            dataListPanel.add(errorLabel);
            dataListPanel.revalidate();
            dataListPanel.repaint();
            return;
        }

        // Get data for the selected table
        HashMap<String, String> entry;
        try {
            entry = DatabaseUtils.getDeviceByAssetName(tableName, assetName);
            LOGGER.log(Level.INFO, "Table {0} entry for AssetName {1}: {2}", new Object[]{tableName, assetName, entry});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying table {0}: {1}", new Object[]{tableName, e.getMessage()});
            JLabel errorLabel = new JLabel("Error loading data: " + e.getMessage());
            dataListPanel.add(errorLabel);
            dataListPanel.revalidate();
            dataListPanel.repaint();
            return;
        }

        if (entry == null || entry.isEmpty()) {
            JLabel messageLabel = new JLabel("No entry found in table " + tableName + " for AssetName: " + assetName);
            dataListPanel.add(messageLabel);
            dataListPanel.revalidate();
            dataListPanel.repaint();
            return;
        }

        // Collect columns with non-empty data
        Set<String> columns = new HashSet<>();
        for (String column : entry.keySet()) {
            if (!column.equals("AssetName") && !column.equals("TableName") && !inventoryNonEmptyColumns.contains(column)) {
                String value = entry.get(column);
                if (value != null && !value.trim().isEmpty()) {
                    columns.add(column);
                }
            }
        }

        // Display data in list format
        JLabel tableLabel = new JLabel("Table: " + tableName);
        tableLabel.setFont(new Font("Arial", Font.BOLD, 12));
        dataListPanel.add(tableLabel);
        for (String column : columns) {
            String value = entry.get(column);
            JLabel dataLabel = new JLabel(column + ": " + value);
            dataLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            dataListPanel.add(dataLabel);
        }

        dataListPanel.revalidate();
        dataListPanel.repaint();
    }

    private void refreshDetails() {
        updateInventorySpecsArea(inventorySpecsArea);
        String selectedTable = tableList.getSelectedValue();
        if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
            updateDataList(selectedTable);
        }
        revalidate();
        repaint();
    }
}