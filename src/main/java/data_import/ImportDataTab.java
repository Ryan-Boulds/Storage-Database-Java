package data_import;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import data_import.ui.MappingDialog;
import data_import.ui.PreviewDialog;
import utils.UIComponentUtils;

public class ImportDataTab extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final String[] tableColumns = utils.DefaultColumns.getInventoryColumns();
    private Map<String, String> columnMappings = new java.util.HashMap<>();
    private java.util.List<String[]> importedData;
    private final JLabel statusLabel;
    private boolean showDuplicates = true;
    private final DataProcessor dataProcessor;
    private final DatabaseHandler databaseHandler;
    private java.util.List<utils.DataEntry> originalData;

    public ImportDataTab(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        this.dataProcessor = new DataProcessor();
        this.databaseHandler = new DatabaseHandler();
        setLayout(new BorderLayout(10, 10));

        JButton importButton = UIComponentUtils.createFormattedButton("Import Data (.csv, .xlsx, .xls)");
        importButton.addActionListener(e -> importData());

        JButton saveButton = UIComponentUtils.createFormattedButton("Save to Database");
        saveButton.addActionListener(e -> saveToDatabase());

        JButton viewMappingsButton = UIComponentUtils.createFormattedButton("View Current Mappings");
        viewMappingsButton.addActionListener(e -> showCurrentMappings());

        JButton toggleDuplicatesButton = UIComponentUtils.createFormattedButton("Toggle Duplicates");
        toggleDuplicatesButton.addActionListener(e -> toggleDuplicates());

        JButton removeDuplicatesButton = UIComponentUtils.createFormattedButton("Remove Duplicates from Import List");
        removeDuplicatesButton.addActionListener(e -> removeDuplicates());

        JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        buttonPanel.add(importButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(viewMappingsButton);
        buttonPanel.add(toggleDuplicatesButton);
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
        table.setDefaultRenderer(Object.class, new TableColorRenderer(this));
        table.getColumnModel().getColumn(0).setPreferredWidth(150); // Adjust AssetName column width

        JScrollPane tableScrollPane = UIComponentUtils.createScrollableContentPanel(table);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    private void importData() {
        PreviewDialog previewDialog = new PreviewDialog(this);
        java.util.List<String[]> data = previewDialog.showDialog();
        if (data != null) {
            statusLabel.setText("Data loaded for mapping.");
            importedData = data;
            java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "Imported Data Headers: {0}", java.util.Arrays.toString(importedData.get(0)));
            for (int i = 1; i < java.lang.Math.min(importedData.size(), 6); i++) {
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "Row {0}: {1}", new Object[]{i, java.util.Arrays.toString(importedData.get(i))});
            }
            MappingDialog mappingDialog = new MappingDialog(this, data);
            statusLabel.setText("Mapping dialog opened.");
            mappingDialog.showDialog();
            displayData(mappingDialog.getColumnMappings(), mappingDialog.getNewFields(), mappingDialog.getDeviceTypeMappings());
            statusLabel.setText("Data displayed for review.");
        } else {
            statusLabel.setText("No data selected.");
        }
    }

    public void importData(Map<String, String> columnMappings, Map<String, String> newFields, Map<String, String> deviceTypeMappings, java.util.List<String[]> data) {
        this.importedData = data;
        displayData(columnMappings, newFields, deviceTypeMappings);
    }

    private void displayData(Map<String, String> columnMappings, Map<String, String> newFields, Map<String, String> deviceTypeMappings) {
        tableModel.setRowCount(0);
        this.columnMappings = columnMappings;
        java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "DefaultColumns Inventory Columns: {0}", java.util.Arrays.toString(tableColumns));
        java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "Column Mappings: {0}", columnMappings);
        java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "Device Type Mappings: {0}", deviceTypeMappings);

        for (Map.Entry<String, String> entry : newFields.entrySet()) {
            try {
                databaseHandler.addNewField("Inventory", entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding new field '" + entry.getKey() + "': " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        originalData = dataProcessor.processData(importedData, columnMappings, deviceTypeMappings, tableColumns);
        for (utils.DataEntry entry : originalData) {
            tableModel.addRow(entry.getValues());
        }

        if (!newFields.isEmpty()) {
            StringBuilder newFieldsMessage = new StringBuilder("New fields added to database schema:\n");
            for (Map.Entry<String, String> entry : newFields.entrySet()) {
                newFieldsMessage.append(entry.getKey()).append(" (").append(entry.getValue()).append(")\n");
            }
            JOptionPane.showMessageDialog(this, newFieldsMessage.toString(), "New Fields Added", JOptionPane.INFORMATION_MESSAGE);
        }

        if (!deviceTypeMappings.isEmpty()) {
            StringBuilder newTypesMessage = new StringBuilder("New Device Types added:\n");
            for (String newType : deviceTypeMappings.values()) {
                newTypesMessage.append(newType).append("\n");
            }
            JOptionPane.showMessageDialog(this, newTypesMessage.toString(), "New Device Types Added", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void toggleDuplicates() {
        showDuplicates = !showDuplicates;
        tableModel.setRowCount(0);
        if (showDuplicates) {
            for (utils.DataEntry entry : originalData) {
                tableModel.addRow(entry.getValues());
            }
        } else {
            java.util.List<utils.DataEntry> nonDuplicates = new java.util.ArrayList<>();
            for (utils.DataEntry entry : originalData) {
                try {
                    String assetName = entry.getData().get("AssetName");
                    if (assetName != null && !assetName.trim().isEmpty()) {
                        HashMap<String, String> existingDevice = databaseHandler.getDeviceByAssetNameFromDB(assetName);
                        if (existingDevice == null) {
                            nonDuplicates.add(entry);
                        }
                    } else {
                        nonDuplicates.add(entry);
                    }
                } catch (SQLException e) {
                    java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "Error checking duplicate: {0}", e.getMessage());
                }
            }
            for (utils.DataEntry entry : nonDuplicates) {
                tableModel.addRow(entry.getValues());
            }
        }
        table.repaint();
        statusLabel.setText("Duplicates " + (showDuplicates ? "shown" : "hidden") + ".");
    }

    private void removeDuplicates() {
        java.util.List<utils.DataEntry> nonDuplicates = new java.util.ArrayList<>();
        for (utils.DataEntry entry : originalData) {
            try {
                String assetName = entry.getData().get("AssetName");
                if (assetName != null && !assetName.trim().isEmpty()) {
                    HashMap<String, String> existingDevice = databaseHandler.getDeviceByAssetNameFromDB(assetName);
                    if (existingDevice == null) {
                        nonDuplicates.add(entry);
                    } else {
                        java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "Removed duplicate: {0}", assetName);
                    }
                } else {
                    nonDuplicates.add(entry);
                }
            } catch (SQLException e) {
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "Error checking duplicate for removal: {0}", e.getMessage());
            }
        }
        originalData = nonDuplicates;
        tableModel.setRowCount(0);
        for (utils.DataEntry entry : originalData) {
            tableModel.addRow(entry.getValues());
        }
        statusLabel.setText("Duplicates removed from import list.");
    }

    private void saveToDatabase() {
        if (importedData == null || importedData.isEmpty()) {
            statusLabel.setText("No data to save.");
            JOptionPane.showMessageDialog(this, "No data to save.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        java.util.List<utils.DataEntry> dataToSave = originalData;
        try {
            for (utils.DataEntry entry : dataToSave) {
                databaseHandler.saveDevice(entry.getData());
            }
            statusLabel.setText("Data saved to database successfully!");
            JOptionPane.showMessageDialog(this, "Data saved to database successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            statusLabel.setText("Error saving to database: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error saving to database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showCurrentMappings() {
        if (columnMappings.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No mappings defined.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel mappingPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        for (Map.Entry<String, String> entry : columnMappings.entrySet()) {
            mappingPanel.add(UIComponentUtils.createAlignedLabel("Source: " + entry.getKey()));
            mappingPanel.add(UIComponentUtils.createAlignedLabel("Maps to: " + entry.getValue()));
        }

        JScrollPane mappingScrollPane = UIComponentUtils.createScrollableContentPanel(mappingPanel);
        mappingScrollPane.setPreferredSize(new Dimension(400, 200));

        JOptionPane.showMessageDialog(this, mappingScrollPane, "Current Column Mappings", JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean isShowDuplicates() {
        return showDuplicates;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public String[] getTableColumns() {
        return tableColumns;
    }
}