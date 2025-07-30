package software_data_importer;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import software_data_importer.ui.ComparisonDialog;
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

    public ImportDataTab(javax.swing.JLabel statusLabel) {
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
        selectedTable = "Inventory"; // Default table
        tableSelector.setSelectedItem(selectedTable);
        tableSelector.addActionListener(e -> {
            selectedTable = (String) tableSelector.getSelectedItem();
            updateTableColumns();
            statusLabel.setText("Selected table: " + selectedTable);
            java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                java.util.logging.Level.INFO, "Selected table changed to: {0}", selectedTable);
        });

        // Button to create a new table
        javax.swing.JButton createTableButton = UIComponentUtils.createFormattedButton("Create New Table");
        createTableButton.addActionListener(e -> createNewTable(statusLabel));

        // Panel for table selector and create table button
        JPanel selectorPanel = new JPanel(new BorderLayout(5, 5));
        selectorPanel.add(tableSelector, BorderLayout.CENTER);
        selectorPanel.add(createTableButton, BorderLayout.SOUTH);
        add(selectorPanel, BorderLayout.WEST);

        // Existing buttons
        javax.swing.JButton importButton = UIComponentUtils.createFormattedButton("Import Data (.csv, .xlsx, .xls)");
        importButton.addActionListener(e -> dataImporter.importData());

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
            if (selectedRow >= 0 && renderer.isYellowOrOrange(selectedRow)) {
                ComparisonDialog dialog = new ComparisonDialog(this, dataDisplayManager.getOriginalData().get(selectedRow), tableColumns);
                utils.DataEntry resolvedEntry = dialog.showDialog();
                if (resolvedEntry != null) {
                    resolvedEntry.setResolved(true);
                    List<utils.DataEntry> originalData = dataDisplayManager.getOriginalData();
                    originalData.set(selectedRow, resolvedEntry);
                    String status = dataDisplayManager.computeRowStatus(selectedRow, resolvedEntry);
                    dataDisplayManager.getRowStatus().put(selectedRow, status);
                    dataDisplayManager.updateTableDisplay();
                    statusLabel.setText("Row " + (selectedRow + 1) + " resolved, status: " + status);
                    java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                        java.util.logging.Level.INFO, "Resolved row {0} for AssetName: {1}, new status: {2}",
                        new Object[]{selectedRow, resolvedEntry.getData().get("AssetName"), status});
                }
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
                        compareItem.setEnabled(renderer.isYellowOrOrange(row));
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        compareItem.setEnabled(renderer.isYellowOrOrange(row));
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        javax.swing.JScrollPane tableScrollPane = UIComponentUtils.createScrollableContentPanel(table);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    private void updateTableSelectorOptions() {
        tableSelector.removeAllItems();
        try {
            List<String> tableNames = DatabaseUtils.getTableNames();
            for (String tableName : tableNames) {
                tableSelector.addItem(tableName);
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                java.util.logging.Level.SEVERE, "Error retrieving table names: {0}", e.getMessage());
            JOptionPane.showMessageDialog(this, "Error retrieving table names: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createNewTable(javax.swing.JLabel statusLabel) {
        String tableName = JOptionPane.showInputDialog(this, "Enter new table name:");
        if (tableName != null && !tableName.trim().isEmpty()) {
            try {
                DatabaseUtils.createTable(tableName);
                updateTableSelectorOptions();
                tableSelector.setSelectedItem(tableName);
                selectedTable = tableName;
                updateTableColumns();
                statusLabel.setText("Table '" + tableName + "' created and selected.");
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.INFO, "Created and selected new table: {0}", tableName);
            } catch (SQLException e) {
                String errorMessage = "Error creating table '" + tableName + "': " + e.getMessage();
                statusLabel.setText(errorMessage);
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.SEVERE, errorMessage, e);
                JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            statusLabel.setText("Table creation cancelled.");
        }
    }

    private void updateTableColumns() {
        try {
            List<String> columns = DatabaseUtils.getInventoryColumnNames(selectedTable);
            tableColumns = columns.toArray(new String[0]);
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
        this.tableColumns = columns;
        tableModel.setColumnIdentifiers(columns);
        table.repaint();
    }

    public DatabaseHandler getDatabaseHandler() {
        return dataImporter.getDatabaseHandler();
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