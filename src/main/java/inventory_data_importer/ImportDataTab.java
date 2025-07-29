package inventory_data_importer;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import inventory_data_importer.ui.ComparisonDialog;
import utils.UIComponentUtils;

public class ImportDataTab extends javax.swing.JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final String[] tableColumns = utils.DefaultColumns.getInventoryColumns();
    private final DataImporter dataImporter;
    public final DataDisplayManager dataDisplayManager;
    private final DataSaver dataSaver;
    private final MappingViewer mappingViewer;
    private final DuplicateManager duplicateManager;

    public ImportDataTab(javax.swing.JLabel statusLabel) {
        this.dataImporter = new DataImporter(this, statusLabel);
        this.dataDisplayManager = new DataDisplayManager(this, statusLabel);
        this.dataSaver = new DataSaver(this, statusLabel);
        this.mappingViewer = new MappingViewer(this, statusLabel);
        this.duplicateManager = new DuplicateManager(this, statusLabel);
        setLayout(new BorderLayout(10, 10));

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
        table.getColumnModel().getColumn(0).setPreferredWidth(150);

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
}