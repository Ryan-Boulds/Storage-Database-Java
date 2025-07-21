package data_import;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import data_import.ui.MappingDialog;
import data_import.ui.PreviewDialog;
import utils.DataUtils;
import utils.DatabaseUtils;
import utils.DefaultColumns;
import utils.UIComponentUtils;

public class ImportDataTab extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final String[] tableColumns = DefaultColumns.getInventoryColumns();
    private Map<String, String> columnMappings = new HashMap<>();
    private List<String[]> importedData;

    public ImportDataTab(JLabel statusLabel) {
        setLayout(new BorderLayout(10, 10));

        JButton importButton = UIComponentUtils.createFormattedButton("Import Data (.csv, .xlsx, .xls)");
        importButton.addActionListener(e -> importData(statusLabel));

        JButton saveButton = UIComponentUtils.createFormattedButton("Save to Database");
        saveButton.addActionListener(e -> saveToDatabase(statusLabel));

        JButton viewMappingsButton = UIComponentUtils.createFormattedButton("View Current Mappings");
        viewMappingsButton.addActionListener(e -> showCurrentMappings());

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        buttonPanel.add(importButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(viewMappingsButton);
        add(buttonPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(tableColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane tableScrollPane = UIComponentUtils.createScrollableContentPanel(table);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    private void importData(JLabel statusLabel) {
        PreviewDialog previewDialog = new PreviewDialog(this);
        List<String[]> data = previewDialog.showDialog();
        if (data != null) {
            statusLabel.setText("Data loaded for mapping.");
            importedData = data;
            // Debug: Print importedData
            System.out.println("Imported Data Headers: " + Arrays.toString(importedData.get(0)));
            for (int i = 1; i < Math.min(importedData.size(), 6); i++) {
                System.out.println("Row " + i + ": " + Arrays.toString(importedData.get(i)));
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

        // Debug: Print tableColumns and columnMappings
        System.out.println("DefaultColumns Inventory Columns: " + Arrays.toString(DefaultColumns.getInventoryColumns()));
        System.out.println("Table Columns: " + Arrays.toString(tableColumns));
        System.out.println("Column Mappings: " + columnMappings);
        System.out.println("Device Type Mappings: " + deviceTypeMappings);

        // Add new fields to the database
        for (Map.Entry<String, String> entry : newFields.entrySet()) {
            try {
                DatabaseUtils.addNewField("Inventory", entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding new field '" + entry.getKey() + "': " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Normalize function to match MappingDialog
        final java.util.function.Function<String, String> normalize = s -> s.replaceAll("[\\s_-]", "").toLowerCase();

        // Create a map of normalized CSV headers to their original indices
        Map<String, Integer> normalizedHeaderMap = new HashMap<>();
        String[] headers = importedData.get(0);
        for (int i = 0; i < headers.length; i++) {
            normalizedHeaderMap.put(normalize.apply(headers[i]), i);
        }

        for (int i = 1; i < importedData.size(); i++) {
            String[] csvRow = importedData.get(i);
            String[] tableRow = new String[tableColumns.length];
            Arrays.fill(tableRow, "");

            for (Map.Entry<String, String> mapping : columnMappings.entrySet()) {
                String csvColumn = mapping.getKey();
                String dbField = mapping.getValue();
                // Normalize the csvColumn to match the header
                String normalizedCsvColumn = normalize.apply(csvColumn);
                Integer csvIndex = normalizedHeaderMap.get(normalizedCsvColumn);
                // Debug: Log mapping details
                System.out.println("Mapping: " + csvColumn + " -> " + dbField + ", normalized: " + normalizedCsvColumn + ", csvIndex: " + csvIndex);
                if (csvIndex != null && csvIndex < csvRow.length) {
                    String value = csvRow[csvIndex];
                    System.out.println("Value for " + csvColumn + ": " + value);
                    if (dbField.equals("Device_Type") && deviceTypeMappings.containsKey(value)) {
                        value = deviceTypeMappings.get(value);
                    }
                    // Match dbField to tableColumns with space-to-underscore replacement
                    for (int k = 0; k < tableColumns.length; k++) {
                        String normalizedTableColumn = tableColumns[k].replace(" ", "_");
                        if (normalizedTableColumn.equals(dbField)) {
                            tableRow[k] = value;
                            break;
                        }
                    }
                } else {
                    System.out.println("Invalid csvIndex for " + csvColumn + ": " + csvIndex);
                }
            }
            tableModel.addRow(tableRow);
            // Debug: Print tableRow to verify data
            System.out.println("Table Row " + i + ": " + Arrays.toString(tableRow));
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

        JOptionPane.showMessageDialog(this, "Data loaded for review. Edit as needed and click 'Save to Database'.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveToDatabase(JLabel statusLabel) {
        if (importedData == null || importedData.isEmpty()) {
            statusLabel.setText("No data to save.");
            JOptionPane.showMessageDialog(this, "No data to save.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<HashMap<String, String>> devicesToSave = new ArrayList<>();
        SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        inputFormat.setLenient(true); // Handle parsing flexibility

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            HashMap<String, String> device = new HashMap<>();
            for (int j = 0; j < tableColumns.length; j++) {
                String dbField = tableColumns[j].replace(" ", "_");
                String value = (String) tableModel.getValueAt(i, j);
                if (value != null && !value.trim().isEmpty()) {
                    // Format date fields if applicable
                    if (dbField.equals("Created_at") || dbField.equals("Last_Successful_Scan")) {
                        try {
                            java.util.Date date = inputFormat.parse(value);
                            value = outputFormat.format(date); // Convert to MM/dd/yyyy HH:mm:ss
                        } catch (java.text.ParseException e) {
                            System.out.println("Error parsing date for " + dbField + " at row " + (i + 1) + ": " + value + " - " + e.getMessage());
                            value = ""; // Set to empty if parsing fails, or handle differently
                        }
                    }
                    device.put(dbField, value);
                }
            }

            String assetName = device.get("AssetName");
            String error = DataUtils.validateDevice(device, assetName);
            if (error != null) {
                statusLabel.setText("Error in row " + (i + 1) + ": " + error);
                JOptionPane.showMessageDialog(this, "Error in row " + (i + 1) + ": " + error, "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            devicesToSave.add(device);
        }

        try {
            for (HashMap<String, String> device : devicesToSave) {
                DatabaseUtils.saveDevice(device);
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
}