package view_software_list_tab.import_spreadsheet_file;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import utils.DatabaseUtils;
import utils.UIComponentUtils;
import view_software_list_tab.TableListPanel;
import view_software_list_tab.ViewSoftwareListTab;

public class ImportDataTab extends javax.swing.JPanel {
    private final DefaultTableModel tableModel;
    private JTable table;
    private String[] tableColumns; // Dynamic based on Excel headers
    private final DataImporter dataImporter;
    public final DataDisplayManager dataDisplayManager;
    private final DataSaver dataSaver;
    private final MappingViewer mappingViewer;
    private final DuplicateManager duplicateManager;
    private final TableListPanel tableListPanel; // Replaced JComboBox with TableListPanel
    private String selectedTable;
    public ImportDataTab(javax.swing.JLabel statusLabel, ViewSoftwareListTab viewSoftwareListTab) {
        this.dataImporter = new DataImporter(this, statusLabel);
        this.dataDisplayManager = new DataDisplayManager(this, statusLabel);
        this.dataSaver = new DataSaver(this, statusLabel);
        this.mappingViewer = new MappingViewer(this, statusLabel);
        this.duplicateManager = new DuplicateManager(this, statusLabel);
        setLayout(new BorderLayout(10, 10));

        // Initialize TableListPanel
        tableListPanel = new TableListPanel(viewSoftwareListTab);
        tableListPanel.setPreferredSize(new java.awt.Dimension(150, 200)); // Initial size, adjustable by JSplitPane

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
        JScrollPane tableScrollPane = new JScrollPane(table);

        // Create JSplitPane to allow resizing
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, tableListPanel, tableScrollPane);
        splitPane.setDividerLocation(200); // Match ViewSoftwareListTab
        splitPane.setResizeWeight(0.3); // Match ViewSoftwareListTab
        add(splitPane, BorderLayout.CENTER);

        // Add ListSelectionListener to the JList in TableListPanel using lambda
        tableListPanel.getTableList().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedTable = tableListPanel.getTableList().getSelectedValue();
                updateTableColumns();
                statusLabel.setText("Selected table: " + (selectedTable != null ? selectedTable : "None"));
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(
                    java.util.logging.Level.INFO, "Selected table changed to: {0}", selectedTable);
            }
        });

        // Existing buttons with Back button
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

        JPopupMenu popupMenu = new JPopupMenu();
        javax.swing.JMenuItem resolveItem = new javax.swing.JMenuItem("Resolve Conflict");
        resolveItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1 && renderer.isYellowOrOrange(selectedRow)) {
                // Placeholder for resolve conflict action (unchanged)
                JOptionPane.showMessageDialog(this, "Resolve conflict for row " + selectedRow, "Resolve Conflict", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        popupMenu.add(resolveItem);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        table.setRowSelectionInterval(row, row);
                        popupMenu.show(table, e.getX(), e.getY());
                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        table.setRowSelectionInterval(row, row);
                        popupMenu.show(table, e.getX(), e.getY());
                    }
                }
            }
        });
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

    public javax.swing.JList<String> getTableList() {
        return tableListPanel.getTableList();
    }
}