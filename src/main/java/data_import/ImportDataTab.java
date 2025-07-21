package data_import;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import data_import.ui.ComparisonDialog;
import data_import.ui.MappingDialog;
import data_import.ui.PreviewDialog;
import utils.UIComponentUtils;

public class ImportDataTab extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final String[] tableColumns = utils.DefaultColumns.getInventoryColumns();
    private Map<String, String> columnMappings = new HashMap<>();
    private List<String[]> importedData;
    private final JLabel statusLabel;
    private boolean showDuplicates = true;
    private final DataProcessor dataProcessor;
    private final DatabaseHandler databaseHandler;
    private List<utils.DataEntry> originalData;

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

        // Add right-click context menu
        JPopupMenu popupMenu = new JPopupMenu();
        javax.swing.JMenuItem compareItem = new javax.swing.JMenuItem("Compare and Resolve");
        compareItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                TableColorRenderer renderer = (TableColorRenderer) table.getDefaultRenderer(Object.class);
                if (renderer.isYellowOrOrange(selectedRow)) {
                    ComparisonDialog dialog = new ComparisonDialog(this, originalData.get(selectedRow), tableColumns);
                    utils.DataEntry resolvedEntry = dialog.showDialog();
                    if (resolvedEntry != null) {
                        resolvedEntry.setResolved(true); // Mark as resolved
                        originalData.set(selectedRow, resolvedEntry);
                        tableModel.setRowCount(0);
                        for (utils.DataEntry entry : originalData) {
                            if (showDuplicates || !renderer.isExactDuplicate(tableModel.getRowCount())) {
                                tableModel.addRow(entry.getValues());
                            }
                        }
                        table.repaint();
                        statusLabel.setText("Row " + (selectedRow + 1) + " resolved.");
                        java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "Resolved row {0} for AssetName: {1}", new Object[]{selectedRow, resolvedEntry.getData().get("AssetName")});
                    }
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
                        TableColorRenderer renderer = (TableColorRenderer) table.getDefaultRenderer(Object.class);
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
                        TableColorRenderer renderer = (TableColorRenderer) table.getDefaultRenderer(Object.class);
                        compareItem.setEnabled(renderer.isYellowOrOrange(row));
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane tableScrollPane = UIComponentUtils.createScrollableContentPanel(table);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    private void importData() {
        // Check if data is already loaded
        if (importedData != null && !importedData.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure that you want to import a different file? Any changes made here will be lost unless you save to database.",
                    "Confirm Import",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
                statusLabel.setText("Import cancelled.");
                return;
            }
        }

        PreviewDialog previewDialog = new PreviewDialog(this);
        List<String[]> data = previewDialog.showDialog();
        if (data != null) {
            statusLabel.setText("Data loaded for mapping.");
            importedData = data;
            java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "Imported Data Headers: {0}", java.util.Arrays.toString(importedData.get(0)));
            for (int i = 1; i < Math.min(importedData.size(), 6); i++) {
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

    public void importData(Map<String, String> columnMappings, Map<String, String> newFields, Map<String, String> deviceTypeMappings, List<String[]> data) {
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
        TableColorRenderer renderer = (TableColorRenderer) table.getDefaultRenderer(Object.class);
        for (utils.DataEntry entry : originalData) {
            if (showDuplicates || !renderer.isExactDuplicate(tableModel.getRowCount())) {
                tableModel.addRow(entry.getValues());
            }
        }
        table.repaint();
        statusLabel.setText("Duplicates " + (showDuplicates ? "shown" : "hidden") + ".");
    }

    private void removeDuplicates() {
        List<utils.DataEntry> nonDuplicates = new ArrayList<>();
        TableColorRenderer renderer = (TableColorRenderer) table.getDefaultRenderer(Object.class);
        for (utils.DataEntry entry : originalData) {
            tableModel.setRowCount(0);
            tableModel.addRow(entry.getValues());
            if (!renderer.isExactDuplicate(0)) {
                nonDuplicates.add(entry);
            } else {
                java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "Removed exact duplicate: {0}", entry.getData().get("AssetName"));
            }
        }
        originalData = nonDuplicates;
        tableModel.setRowCount(0);
        for (utils.DataEntry entry : originalData) {
            if (showDuplicates || !renderer.isExactDuplicate(tableModel.getRowCount())) {
                tableModel.addRow(entry.getValues());
            }
        }
        statusLabel.setText("Exact duplicates removed from import list.");
    }

    private void saveToDatabase() {
        if (importedData == null || importedData.isEmpty()) {
            statusLabel.setText("No data to save.");
            JOptionPane.showMessageDialog(this, "No data to save.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        TableColorRenderer renderer = (TableColorRenderer) table.getDefaultRenderer(Object.class);
        boolean allGreen = true;
        List<utils.DataEntry> redRows = new ArrayList<>();
        List<utils.DataEntry> orangeRows = new ArrayList<>();
        List<utils.DataEntry> yellowRows = new ArrayList<>();
        List<utils.DataEntry> greenRows = new ArrayList<>();

        // Categorize rows
        for (utils.DataEntry entry : originalData) {
            HashMap<String, String> device = entry.getData();
            String assetName = device.get("AssetName");
            if (entry.isResolved()) {
                greenRows.add(entry); // Resolved rows treated as green
            } else if (assetName != null && !assetName.trim().isEmpty()) {
                try {
                    HashMap<String, String> existingDevice = databaseHandler.getDeviceByAssetNameFromDB(assetName);
                    if (existingDevice != null) {
                        boolean isExactMatch = true;
                        boolean hasNewData = false;
                        boolean hasConflict = false;
                        for (Map.Entry<String, String> field : device.entrySet()) {
                            String key = field.getKey();
                            String newValue = field.getValue();
                            String oldValue = existingDevice.get(key);
                            String newVal = (newValue == null || newValue.trim().isEmpty()) ? "" : newValue;
                            String oldVal = (oldValue == null || oldValue.trim().isEmpty()) ? "" : oldValue;
                            if (!newVal.equals(oldVal)) {
                                isExactMatch = false;
                                if (oldVal.isEmpty()) {
                                    hasNewData = true;
                                } else {
                                    hasConflict = true;
                                }
                            }
                        }
                        if (isExactMatch) {
                            redRows.add(entry);
                            allGreen = false;
                        } else if (hasConflict) {
                            orangeRows.add(entry);
                            allGreen = false;
                        } else if (hasNewData) {
                            yellowRows.add(entry);
                            allGreen = false;
                        }
                    } else {
                        greenRows.add(entry);
                    }
                } catch (SQLException e) {
                    java.util.logging.Logger.getLogger(ImportDataTab.class.getName()).log(Level.INFO, "Error checking row status: {0}", e.getMessage());
                }
            } else {
                greenRows.add(entry);
            }
        }

        if (allGreen) {
            // All rows are green or resolved, save directly
            try {
                for (utils.DataEntry entry : greenRows) {
                    databaseHandler.saveDevice(entry.getData());
                }
                statusLabel.setText("Data saved to database successfully!");
                JOptionPane.showMessageDialog(this, "Data saved to database successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                statusLabel.setText("Error saving to database: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error saving to database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        // Prompt for conflict resolution
        StringBuilder message = new StringBuilder("Found potential issues:\n");
        if (!redRows.isEmpty()) {
            message.append("- ").append(redRows.size()).append(" exact duplicates (red)\n");
        }
        if (!orangeRows.isEmpty()) {
            message.append("- ").append(orangeRows.size()).append(" conflicting entries (orange)\n");
        }
        if (!yellowRows.isEmpty()) {
            message.append("- ").append(yellowRows.size()).append(" entries with new data (yellow)\n");
        }
        message.append("\nChoose an action:");
        String[] options = {"Merge All", "Overwrite Conflicts", "Skip Conflicts", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, message.toString(), "Resolve Conflicts",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        try {
            List<utils.DataEntry> dataToSave = new ArrayList<>();
            switch (choice) {
                case 0: // Merge All
                    dataToSave.addAll(greenRows);
                    for (utils.DataEntry entry : yellowRows) {
                        HashMap<String, String> mergedDevice = new HashMap<>(entry.getData());
                        HashMap<String, String> existingDevice = databaseHandler.getDeviceByAssetNameFromDB(mergedDevice.get("AssetName"));
                        for (String key : existingDevice.keySet()) {
                            if (!mergedDevice.containsKey(key) || mergedDevice.get(key).trim().isEmpty()) {
                                mergedDevice.put(key, existingDevice.get(key));
                            }
                        }
                        dataToSave.add(new utils.DataEntry(entry.getValues(), mergedDevice));
                    }
                    for (utils.DataEntry entry : orangeRows) {
                        dataToSave.add(entry); // Treat as overwrite
                    }
                    // Skip red rows
                    break;
                case 1: // Overwrite Conflicts
                    dataToSave.addAll(greenRows);
                    dataToSave.addAll(yellowRows);
                    dataToSave.addAll(orangeRows);
                    // Skip red rows
                    break;
                case 2: // Skip Conflicts
                    dataToSave.addAll(greenRows);
                    dataToSave.addAll(yellowRows);
                    // Skip orange and red rows
                    break;
                case 3: // Cancel
                    statusLabel.setText("Save operation cancelled.");
                    return;
                default:
                    statusLabel.setText("Invalid option selected.");
                    return;
            }

            // Save the selected data
            for (utils.DataEntry entry : dataToSave) {
                HashMap<String, String> device = entry.getData();
                String assetName = device.get("AssetName");
                if (assetName != null && !assetName.trim().isEmpty()) {
                    HashMap<String, String> existingDevice = databaseHandler.getDeviceByAssetNameFromDB(assetName);
                    if (existingDevice != null) {
                        if (yellowRows.contains(entry) && choice == 0) {
                            // Merge for yellow rows
                            HashMap<String, String> mergedDevice = new HashMap<>(existingDevice);
                            for (Map.Entry<String, String> field : device.entrySet()) {
                                String key = field.getKey();
                                String value = field.getValue();
                                if (value != null && !value.trim().isEmpty()) {
                                    mergedDevice.put(key, value);
                                }
                            }
                            databaseHandler.updateDeviceInDB(mergedDevice);
                        } else {
                            // Overwrite for orange or yellow (if not merging)
                            databaseHandler.updateDeviceInDB(device);
                        }
                    } else {
                        // Insert new (green) rows
                        databaseHandler.saveDevice(device);
                    }
                }
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

    public List<utils.DataEntry> getOriginalData() {
        return originalData;
    }
}