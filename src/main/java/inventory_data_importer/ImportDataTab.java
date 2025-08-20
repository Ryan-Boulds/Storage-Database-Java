package inventory_data_importer;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import inventory_data_importer.ui.ComparisonDialog;
import utils.DataEntry;
import utils.DatabaseUtils;
import utils.UIComponentUtils;

public class ImportDataTab extends javax.swing.JPanel {
    private final DefaultTableModel tableModel;
    private JTable table;
    private String[] tableColumns; // Dynamic based on Excel headers
    private final DataImporter dataImporter;
    public final DataDisplayManager dataDisplayManager;
    private final DataSaver dataSaver;
    private final MappingViewer mappingViewer;
    private final DuplicateManager duplicateManager;
    private final JComboBox<String> tableSelector;
    private String selectedTable;
    private final javax.swing.JLabel statusLabel;

    public ImportDataTab(javax.swing.JLabel statusLabel) {
        this.statusLabel = statusLabel;
        this.dataImporter = new DataImporter(this, statusLabel);
        this.dataDisplayManager = new DataDisplayManager(this, statusLabel);
        this.dataSaver = new DataSaver(this, statusLabel);
        this.mappingViewer = new MappingViewer(this, statusLabel);
        this.duplicateManager = new DuplicateManager(this, statusLabel);
        setLayout(new BorderLayout(10, 10));

        // Initialize table selector dropdown
        tableSelector = new JComboBox<>();
        tableSelector.setPreferredSize(new java.awt.Dimension(150, 30));
        updateTableSelectorOptions();
        selectedTable = null; // Default to no table selected
        tableSelector.addActionListener(e -> {
            selectedTable = (String) tableSelector.getSelectedItem();
            updateTableColumns();
            statusLabel.setText("Selected table: " + (selectedTable != null ? selectedTable : "None"));
            java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                java.util.logging.Level.INFO, "Selected table changed to: {0}", selectedTable);
        });

        // Button to create a new table
        javax.swing.JButton createTableButton = UIComponentUtils.createFormattedButton("Create New Table");
        createTableButton.addActionListener(e -> createNewTable());

        // Panel for table selector and create table button
        JPanel selectorPanel = new JPanel(new BorderLayout(5, 5));
        selectorPanel.add(tableSelector, BorderLayout.CENTER);
        selectorPanel.add(createTableButton, BorderLayout.SOUTH);
        add(selectorPanel, BorderLayout.WEST);

        // Existing buttons
        javax.swing.JButton importButton = UIComponentUtils.createFormattedButton("Import Data (.csv, .xlsx, .xls)");
        importButton.addActionListener(e -> {
            try {
                dataImporter.importData();
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.SEVERE, "Error importing data: {0}", ex.getMessage());
                JOptionPane.showMessageDialog(this, "Error importing data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        javax.swing.JButton saveButton = UIComponentUtils.createFormattedButton("Save to Database");
        saveButton.addActionListener(e -> dataSaver.saveToDatabase());

        javax.swing.JButton viewMappingsButton = UIComponentUtils.createFormattedButton("View Current Mappings");
        viewMappingsButton.addActionListener(e -> mappingViewer.showCurrentMappings());

        javax.swing.JButton removeDuplicatesButton = UIComponentUtils.createFormattedButton("Remove Duplicates from Import List");
        removeDuplicatesButton.addActionListener(e -> duplicateManager.removeDuplicates());

        javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.GridLayout(1, 4, 10, 10));
        buttonPanel.add(importButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(viewMappingsButton);
        buttonPanel.add(removeDuplicatesButton);
        add(buttonPanel, BorderLayout.NORTH);

        // Initialize table with dynamic columns (will be set after import)
        tableColumns = new String[0];
        tableModel = new DefaultTableModel(tableColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColorRenderer renderer = new TableColorRenderer(this);
        table.setDefaultRenderer(Object.class, renderer);
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(150);
        }

        JPopupMenu popupMenu = new JPopupMenu();
        javax.swing.JMenuItem compareItem = new javax.swing.JMenuItem("Compare and Resolve");
        compareItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0 && dataDisplayManager.getRowStatus().get(selectedRow) != null && 
                dataDisplayManager.getRowStatus().get(selectedRow).equals("yellow")) {
                DataEntry entry = dataDisplayManager.getOriginalData().get(selectedRow);
                ComparisonDialog dialog = new ComparisonDialog(this, entry, tableColumns);
                DataEntry resolvedEntry = dialog.showDialog();
                if (resolvedEntry != null) {
                    dataDisplayManager.getOriginalData().set(selectedRow, resolvedEntry);
                    resolvedEntry.setResolved(true);
                    String newStatus = dataDisplayManager.computeRowStatus(selectedRow, resolvedEntry);
                    dataDisplayManager.getRowStatus().put(selectedRow, newStatus);
                    dataDisplayManager.updateTableDisplay();
                    statusLabel.setText("Row " + (selectedRow + 1) + " resolved.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a row with conflicts (yellow) to resolve.", 
                    "Invalid Selection", JOptionPane.WARNING_MESSAGE);
            }
        });
        popupMenu.add(compareItem);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        popupMenu.show(table, e.getX(), e.getY());
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        popupMenu.show(table, e.getX(), e.getY());
                    }
                }
            }
        });

        add(new javax.swing.JScrollPane(table), BorderLayout.CENTER);
    }

    private void createNewTable() {
        String inputTableName = JOptionPane.showInputDialog(this, "Enter new table name:");
        if (inputTableName != null && !inputTableName.trim().isEmpty()) {
            String sanitizedTableName = inputTableName.trim().replace(" ", "_");
            if (sanitizedTableName.matches("\\d+")) {
                String errorMessage = "Error: Table name cannot be purely numeric";
                statusLabel.setText(errorMessage);
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.SEVERE, "Attempted to create numeric table name '{0}'", sanitizedTableName);
                JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try (Connection conn = DatabaseUtils.getConnection()) {
                List<String> existingTables = DatabaseUtils.getTableNames();
                if (existingTables.stream().anyMatch(t -> t.equalsIgnoreCase(sanitizedTableName))) {
                    String errorMessage = "Error: Table '" + sanitizedTableName + "' already exists";
                    statusLabel.setText(errorMessage);
                    java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                        java.util.logging.Level.SEVERE, "Attempted to create existing table '{0}'", sanitizedTableName);
                    JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String sql = "CREATE TABLE [" + sanitizedTableName + "] ([AssetName] VARCHAR(255) PRIMARY KEY)";
                conn.createStatement().executeUpdate(sql);
                if (!tableExists("Settings", conn)) {
                    createSettingsTable(conn);
                }
                int nextId = getNextSettingsId(conn);
                try (PreparedStatement insertPs = conn.prepareStatement("INSERT INTO Settings (ID, InventoryTables) VALUES (?, ?)")) {
                    insertPs.setInt(1, nextId);
                    insertPs.setString(2, sanitizedTableName);
                    insertPs.executeUpdate();
                }
                selectedTable = sanitizedTableName;
                updateTableSelectorOptions();
                tableSelector.setSelectedItem(sanitizedTableName);
                updateTableColumns();
                statusLabel.setText("Table '" + sanitizedTableName + "' created and selected.");
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.INFO, "Created and selected new table: {0}", sanitizedTableName);
            } catch (SQLException e) {
                String errorMessage = "Error creating table '" + sanitizedTableName + "': " + e.getMessage();
                statusLabel.setText(errorMessage);
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.SEVERE, errorMessage, e);
                JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            statusLabel.setText("Table creation cancelled.");
        }
    }

    private boolean tableExists(String tableName, Connection conn) throws SQLException {
        java.sql.DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void createSettingsTable(Connection conn) throws SQLException {
        String createSql = "CREATE TABLE Settings (ID INTEGER PRIMARY KEY, InventoryTables VARCHAR(255))";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createSql);
        }
    }

    private int getNextSettingsId(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(ID) FROM Settings")) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
            return 1;
        }
    }

    private void updateTableSelectorOptions() {
        tableSelector.removeAllItems();
        try (Connection conn = DatabaseUtils.getConnection()) {
            if (!tableExists("Settings", conn)) {
                createSettingsTable(conn);
            }
            List<String> tables = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DISTINCT InventoryTables FROM Settings WHERE InventoryTables IS NOT NULL")) {
                while (rs.next()) {
                    String tableName = rs.getString("InventoryTables");
                    if (tableName != null && !tableName.trim().isEmpty()) {
                        tables.add(tableName);
                    }
                }
            }
            for (String tableName : tables) {
                tableSelector.addItem(tableName);
            }
            if (tableSelector.getItemCount() > 0) {
                if (selectedTable == null || !tables.contains(selectedTable)) {
                    selectedTable = (String) tableSelector.getItemAt(0);
                    tableSelector.setSelectedItem(selectedTable);
                }
            } else {
                selectedTable = null;
                statusLabel.setText("No tables available. Please create a new table.");
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.WARNING, "No Inventory tables available in Settings");
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                java.util.logging.Level.SEVERE, "Error retrieving Inventory tables: {0}", e.getMessage());
            JOptionPane.showMessageDialog(this, "Error retrieving Inventory tables: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTableColumns() {
        if (selectedTable == null) {
            tableModel.setColumnIdentifiers(new String[0]);
            tableModel.setRowCount(0);
            dataDisplayManager.getOriginalData().clear();
            dataDisplayManager.getRowStatus().clear();
            dataDisplayManager.getFieldTypes().clear();
            table.repaint();
            return;
        }
        try {
            List<String> columns = DatabaseUtils.getInventoryColumnNames(selectedTable);
            tableColumns = columns.stream().map(col -> col.replace(" ", "_")).toArray(String[]::new);
            tableModel.setColumnIdentifiers(tableColumns);
            tableModel.setRowCount(0);
            dataDisplayManager.getOriginalData().clear();
            dataDisplayManager.getRowStatus().clear();
            dataDisplayManager.getFieldTypes().clear();
            dataDisplayManager.getFieldTypes().putAll(DatabaseUtils.getInventoryColumnTypes(selectedTable));
            table.repaint();
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                java.util.logging.Level.SEVERE, "Error retrieving columns for table {0}: {1}", 
                new Object[]{selectedTable, e.getMessage()});
            JOptionPane.showMessageDialog(this, "Error retrieving columns: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setTableColumns(String[] columns) {
        this.tableColumns = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            this.tableColumns[i] = columns[i].replace(" ", "_");
        }
        tableModel.setColumnIdentifiers(this.tableColumns);
        table.repaint();
    }

    public DatabaseHandler getDatabaseHandler() {
        return dataImporter.getDatabaseHandler();
    }

    public DataImporter getDataImporter() {
        return dataImporter;
    }

    public java.util.Map<String, String> getFieldTypes() {
        return dataDisplayManager.getFieldTypes();
    }

    public boolean isShowDuplicates() {
        return duplicateManager.isShowDuplicates();
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTable getTable() {
        return table;
    }

    public String[] getTableColumns() {
        return tableColumns;
    }

    public List<utils.DataEntry> getOriginalData() {
        return dataDisplayManager.getOriginalData();
    }

    public java.util.HashMap<Integer, String> getRowStatus() {
        return dataDisplayManager.getRowStatus();
    }

    public String getSelectedTable() {
        return selectedTable;
    }
}