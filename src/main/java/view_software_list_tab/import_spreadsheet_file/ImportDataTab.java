package view_software_list_tab.import_spreadsheet_file;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import utils.DataEntry;
import utils.DatabaseUtils;
import utils.UIComponentUtils;
import view_software_list_tab.ViewSoftwareListTab;
import view_software_list_tab.import_spreadsheet_file.ui.ComparisonDialog;

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
    private final ViewSoftwareListTab viewSoftwareListTab; // Reference to parent tab

    public ImportDataTab(javax.swing.JLabel statusLabel, ViewSoftwareListTab viewSoftwareListTab) {
        this.statusLabel = statusLabel;
        this.viewSoftwareListTab = viewSoftwareListTab;
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

        // Existing buttons with Back button added
        javax.swing.JButton backButton = UIComponentUtils.createFormattedButton("Back");
        backButton.addActionListener(e -> {
            if (viewSoftwareListTab != null) {
                viewSoftwareListTab.showMainView();
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.INFO, "Back button clicked, returning to main view");
            }
        });

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

        javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.GridLayout(1, 5, 10, 10));
        buttonPanel.add(backButton);
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
        javax.swing.JMenuItem resolveItem = new javax.swing.JMenuItem("Resolve Conflict");
        resolveItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1 && renderer.isYellowOrOrange(selectedRow)) {
                try {
                    String tableName = selectedTable.replace(" ", "_");
                    String assetName = (String) tableModel.getValueAt(selectedRow, table.convertColumnIndexToModel(0));
                    HashMap<String, String> existingDevice = dataImporter.getDatabaseHandler().getDeviceByAssetNameFromDB(tableName, assetName);
                    DataEntry newEntry = getOriginalData().get(selectedRow);
                    ComparisonDialog comparisonDialog = new ComparisonDialog(this, newEntry, tableColumns, existingDevice);
                    DataEntry resolvedEntry = comparisonDialog.showDialog();
                    if (resolvedEntry != null) {
                        resolvedEntry.setResolved(true);
                        getOriginalData().set(selectedRow, resolvedEntry);
                        String newStatus = dataDisplayManager.computeRowStatus(selectedRow, resolvedEntry);
                        getRowStatus().put(selectedRow, newStatus);
                        for (int col = 0; col < tableModel.getColumnCount(); col++) {
                            tableModel.setValueAt(resolvedEntry.getValues()[col], selectedRow, col);
                        }
                        table.repaint();
                    }
                } catch (SQLException ex) {
                    java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                        java.util.logging.Level.SEVERE, "Error resolving conflict: {0}", ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Error resolving conflict: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        popupMenu.add(resolveItem);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
                        if (renderer.isYellowOrOrange(row)) {
                            popupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                }
            }
        });

        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void createNewTable() {
        String newTableName = JOptionPane.showInputDialog(this, "Enter new table name:");
        if (newTableName == null || newTableName.trim().isEmpty()) {
            return;
        }
        newTableName = newTableName.trim();

        try (Connection conn = DatabaseUtils.getConnection()) {
            if (tableExists(newTableName, conn)) {
                JOptionPane.showMessageDialog(this, "Table already exists", "Error", JOptionPane.ERROR_MESSAGE);
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.WARNING, "Attempted to create existing table '{0}'", newTableName);
                return;
            }

            // Create table
            String createSql = "CREATE TABLE [" + newTableName + "] (AssetName VARCHAR(255) PRIMARY KEY)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createSql);
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.INFO, "Created new table '{0}'", newTableName);
            }

            // Add to Settings
            int nextId = getNextSettingsId(conn);
            String insertSql = "INSERT INTO Settings (ID, SoftwareTables) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, nextId);
                ps.setString(2, newTableName);
                ps.executeUpdate();
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.INFO, "Added table '{0}' to Settings with ID {1}", new Object[]{newTableName, nextId});
            }

            updateTableSelectorOptions();
            tableSelector.setSelectedItem(newTableName);
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                java.util.logging.Level.SEVERE, "SQLException creating table '{0}': {1}", new Object[]{newTableName, e.getMessage()});
            JOptionPane.showMessageDialog(this, "Error creating table: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean tableExists(String tableName, Connection conn) throws SQLException {
        java.sql.DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void createSettingsTable(Connection conn) throws SQLException {
        String createSql = "CREATE TABLE Settings (ID INTEGER PRIMARY KEY, SoftwareTables VARCHAR(255))";
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

    public final void updateTableSelectorOptions() {
        tableSelector.removeAllItems();
        try (Connection conn = DatabaseUtils.getConnection()) {
            if (!tableExists("Settings", conn)) {
                createSettingsTable(conn);
            }
            List<String> tables = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DISTINCT SoftwareTables FROM Settings WHERE SoftwareTables IS NOT NULL")) {
                while (rs.next()) {
                    String tableName = rs.getString("SoftwareTables");
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
                    java.util.logging.Level.WARNING, "No software tables available in Settings");
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                java.util.logging.Level.SEVERE, "Error retrieving software tables: {0}", e.getMessage());
            JOptionPane.showMessageDialog(this, "Error retrieving software tables: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

    public JComboBox<String> getTableSelector() {
        return tableSelector;
    }
}