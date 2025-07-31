package view_inventorytab.view_device_details;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import utils.DatabaseUtils;
import utils.UIComponentUtils;
import view_inventorytab.ViewInventoryTab;

public class DeviceDetailsPanel extends JPanel {
    private final String assetName;
    private final ViewInventoryTab parentTab;
    private static final int CONTENT_WIDTH = 800; // Fixed width for content
    private static final int SPECS_HEIGHT = 200; // Fixed height for Inventory specs
    private static final int TABLE_HEIGHT = 400; // Fixed height for table
    private static final Logger LOGGER = Logger.getLogger(DeviceDetailsPanel.class.getName());

    public DeviceDetailsPanel(String assetName, ViewInventoryTab parentTab) {
        this.assetName = assetName;
        this.parentTab = parentTab;
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

        // Top panel with title and back button (anchored)
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(backButton, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Content panel for Inventory specs and other tables
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setPreferredSize(new Dimension(CONTENT_WIDTH, SPECS_HEIGHT + TABLE_HEIGHT + 50));
        contentPanel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

        // Inventory specs display
        JTextArea inventorySpecsArea = createInventorySpecsArea();
        contentPanel.add(inventorySpecsArea);
        contentPanel.add(Box.createVerticalStrut(10)); // Space before separator

        // Separator
        JLabel separatorLabel = new JLabel("Other Tables");
        separatorLabel.setFont(new Font("Arial", Font.BOLD, 14));
        separatorLabel.setAlignmentX(LEFT_ALIGNMENT);
        contentPanel.add(separatorLabel);
        contentPanel.add(Box.createVerticalStrut(10)); // Space after separator

        // Table for other entries
        JTable otherTables = createOtherTables();
        contentPanel.add(otherTables.getTableHeader());
        contentPanel.add(otherTables);

        // Single scroll pane for all content
        JScrollPane scrollPane = new JScrollPane(contentPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JTextArea createInventorySpecsArea() {
        JTextArea specsArea = new JTextArea();
        specsArea.setEditable(false);
        specsArea.setFont(new Font("Arial", Font.PLAIN, 12));
        specsArea.setBorder(javax.swing.BorderFactory.createTitledBorder("Inventory Specifications"));
        specsArea.setAlignmentX(LEFT_ALIGNMENT);
        specsArea.setPreferredSize(new Dimension(CONTENT_WIDTH, SPECS_HEIGHT));
        specsArea.setMaximumSize(new Dimension(CONTENT_WIDTH, SPECS_HEIGHT));

        // Get Inventory entry
        HashMap<String, String> inventoryEntry;
        try {
            inventoryEntry = DatabaseUtils.getDeviceByAssetName("Inventory", assetName);
            if (inventoryEntry != null && !inventoryEntry.isEmpty()) {
                LOGGER.log(Level.INFO, "Inventory entry columns for AssetName {0}: {1}", new Object[]{assetName, inventoryEntry.keySet()});
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying Inventory table: {0}", e.getMessage());
            specsArea.setText("Error loading Inventory data: " + e.getMessage());
            return specsArea;
        }

        if (inventoryEntry == null || inventoryEntry.isEmpty()) {
            specsArea.setText("No entry found in Inventory table for AssetName: " + assetName);
            return specsArea;
        }

        // Build specs string
        StringBuilder specs = new StringBuilder();
        for (String column : inventoryEntry.keySet()) {
            if (!column.equals("AssetName") && !column.equals("TableName")) {
                String value = inventoryEntry.get(column);
                if (value != null && !value.trim().isEmpty()) {
                    specs.append(column).append(": ").append(value).append("\n");
                }
            }
        }
        specsArea.setText(specs.length() > 0 ? specs.toString() : "No specifications available");
        return specsArea;
    }

    private JTable createOtherTables() {
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        table.setPreferredSize(new Dimension(CONTENT_WIDTH, TABLE_HEIGHT));
        table.setMaximumSize(new Dimension(CONTENT_WIDTH, TABLE_HEIGHT));

        // Get all table names from the database
        List<String> tableNames;
        try {
            tableNames = DatabaseUtils.getTableNames();
            LOGGER.log(Level.INFO, "Retrieved table names: {0}", tableNames);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving table names: {0}", e.getMessage());
            model.addColumn("Error");
            model.addRow(new Object[]{"Failed to load data: " + e.getMessage()});
            return table;
        }

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
        }

        // Collect unique columns from other tables
        Set<String> otherColumns = new HashSet<>();
        List<HashMap<String, String>> otherEntries = new ArrayList<>();

        // Track columns with non-empty values in other tables
        HashMap<String, Boolean> columnHasNonEmptyData = new HashMap<>();

        for (String tableName : tableNames) {
            if (!tableName.equals("Inventory")) {
                try {
                    HashMap<String, String> entry = DatabaseUtils.getDeviceByAssetName(tableName, assetName);
                    if (entry != null && !entry.isEmpty()) {
                        otherEntries.add(entry);
                        Set<String> entryColumns = new HashSet<>(entry.keySet());
                        entryColumns.remove("AssetName");
                        entryColumns.remove("TableName");
                        LOGGER.log(Level.INFO, "Table {0} columns: {1}", new Object[]{tableName, entryColumns});
                        for (String column : entryColumns) {
                            // Include column if not in Inventory or if Inventory has empty value
                            if (!inventoryNonEmptyColumns.contains(column)) {
                                otherColumns.add(column);
                                // Check if this column has non-empty data
                                String value = entry.get(column);
                                if (value != null && !value.trim().isEmpty()) {
                                    columnHasNonEmptyData.put(column, true);
                                } else {
                                    columnHasNonEmptyData.putIfAbsent(column, false);
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error querying table {0}: {1}", new Object[]{tableName, e.getMessage()});
                }
            }
        }

        // Filter otherColumns to include only those with non-empty data
        Set<String> filteredColumns = new HashSet<>();
        for (String column : otherColumns) {
            if (columnHasNonEmptyData.getOrDefault(column, false)) {
                filteredColumns.add(column);
            }
        }
        LOGGER.log(Level.INFO, "Filtered columns with non-empty data from other tables: {0}", filteredColumns);

        // Set up table columns
        List<String> columnList = new ArrayList<>();
        columnList.add("Table"); // First column for table name
        columnList.addAll(filteredColumns); // Add columns with non-empty data
        model.setColumnIdentifiers(columnList.toArray());
        LOGGER.log(Level.INFO, "Table columns set: {0}", columnList);

        // Add other entries
        for (HashMap<String, String> entry : otherEntries) {
            Object[] row = new Object[columnList.size()];
            row[0] = entry.getOrDefault("TableName", "Unknown");
            for (int i = 1; i < columnList.size(); i++) {
                String column = columnList.get(i);
                row[i] = entry.getOrDefault(column, "");
            }
            model.addRow(row);
        }

        // If no other entries found, add a message
        if (otherEntries.isEmpty()) {
            model.addColumn("Message");
            model.addRow(new Object[]{"No entries found in other tables for AssetName: " + assetName});
        }

        return table;
    }
}