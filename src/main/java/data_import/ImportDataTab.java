package data_import;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import utils.DatabaseUtils;
import utils.DefaultColumns;
import utils.SQLGenerator;
import utils.UIComponentUtils;

public class ImportDataTab extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final String[] tableColumns = DefaultColumns.getInventoryColumns();
    private Map<String, String> columnMappings = new HashMap<>();
    private List<String[]> importedData;
    private final JLabel statusLabel;

    public ImportDataTab(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        setLayout(new BorderLayout(10, 10));

        JButton importButton = UIComponentUtils.createFormattedButton("Import Data (.csv, .xlsx, .xls)");
        importButton.addActionListener(e -> importData());

        JButton saveButton = UIComponentUtils.createFormattedButton("Save to Database");
        saveButton.addActionListener(e -> saveToDatabase());

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

    private void importData() {
        PreviewDialog previewDialog = new PreviewDialog(this);
        List<String[]> data = previewDialog.showDialog();
        if (data != null) {
            statusLabel.setText("Data loaded for mapping.");
            importedData = data;
            System.out.println("Imported Data Headers: " + java.util.Arrays.toString(importedData.get(0)));
            for (int i = 1; i < Math.min(importedData.size(), 6); i++) {
                System.out.println("Row " + i + ": " + java.util.Arrays.toString(importedData.get(i)));
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

        System.out.println("DefaultColumns Inventory Columns: " + java.util.Arrays.toString(DefaultColumns.getInventoryColumns()));
        System.out.println("Table Columns: " + java.util.Arrays.toString(tableColumns));
        System.out.println("Column Mappings: " + columnMappings);
        System.out.println("Device Type Mappings: " + deviceTypeMappings);

        for (Map.Entry<String, String> entry : newFields.entrySet()) {
            try {
                DatabaseUtils.addNewField("Inventory", entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding new field '" + entry.getKey() + "': " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        final java.util.function.Function<String, String> normalize = s -> s.replaceAll("[\\s_-]", "").toLowerCase();

        Map<String, Integer> normalizedHeaderMap = new HashMap<>();
        String[] headers = importedData.get(0);
        for (int i = 0; i < headers.length; i++) {
            normalizedHeaderMap.put(normalize.apply(headers[i]), i);
        }

        // Define date formats to try parsing (date-only)
        SimpleDateFormat[] dateFormats = {
            new SimpleDateFormat("yyyy-MM-dd"),          // e.g., 2025-07-21
            new SimpleDateFormat("MM/dd/yyyy"),          // e.g., 07/21/2025
            new SimpleDateFormat("dd-MM-yyyy")           // e.g., 21-07-2025
        };
        for (SimpleDateFormat df : dateFormats) {
            df.setLenient(false); // Strict parsing
        }
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy");

        for (int i = 1; i < importedData.size(); i++) {
            String[] csvRow = importedData.get(i);
            String[] tableRow = new String[tableColumns.length];
            java.util.Arrays.fill(tableRow, "");

            for (Map.Entry<String, String> mapping : columnMappings.entrySet()) {
                String csvColumn = mapping.getKey();
                String dbField = mapping.getValue();
                String normalizedCsvColumn = normalize.apply(csvColumn);
                Integer csvIndex = normalizedHeaderMap.get(normalizedCsvColumn);
                System.out.println("Mapping: " + csvColumn + " -> " + dbField + ", normalized: " + normalizedCsvColumn + ", csvIndex: " + csvIndex);
                if (csvIndex != null && csvIndex < csvRow.length) {
                    String value = csvRow[csvIndex];
                    System.out.println("Value for " + csvColumn + ": " + value);
                    if (dbField.equals("Device_Type") && deviceTypeMappings.containsKey(value)) {
                        value = deviceTypeMappings.get(value);
                    }
                    if ((dbField.equals("Created_at") || dbField.equals("Last_Successful_Scan")) && !value.trim().isEmpty()) {
                        boolean parsed = false;
                        for (SimpleDateFormat df : dateFormats) {
                            try {
                                java.util.Date date = df.parse(value);
                                value = outputFormat.format(date);
                                parsed = true;
                                break;
                            } catch (ParseException e) {
                                System.out.println("Failed to parse " + value + " with format " + df.toPattern() + " at row " + i + ": " + e.getMessage());
                            }
                        }
                        if (!parsed) {
                            System.out.println("Unparseable date for " + dbField + " at row " + i + ": " + value + ". Using empty string.");
                            value = ""; // Default to empty if unparseable
                        }
                    }
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
            System.out.println("Table Row " + i + ": " + java.util.Arrays.toString(tableRow));
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

        checkDuplicates();
    }

    private void checkDuplicates() {
        List<HashMap<String, String>> devicesToCheck = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            HashMap<String, String> device = new HashMap<>();
            for (int j = 0; j < tableColumns.length; j++) {
                String dbField = tableColumns[j].replace(" ", "_");
                String value = (String) tableModel.getValueAt(i, j);
                if (value != null && !value.trim().isEmpty()) {
                    device.put(dbField, value);
                }
            }
            devicesToCheck.add(device);
        }

        List<String> duplicates = new ArrayList<>();
        for (HashMap<String, String> device : devicesToCheck) {
            String assetName = device.get("AssetName");
            if (assetName != null && !assetName.trim().isEmpty()) {
                try {
                    HashMap<String, String> existingDevice = getDeviceByAssetNameFromDB(assetName);
                    if (existingDevice != null) {
                        duplicates.add(assetName);
                    }
                } catch (SQLException e) {
                    System.out.println("Error checking duplicate for " + assetName + ": " + e.getMessage());
                }
            }
        }

        if (!duplicates.isEmpty()) {
            StringBuilder message = new StringBuilder("The following duplicate entries were not added automatically:\n");
            for (String duplicate : duplicates) {
                message.append(duplicate).append("\n");
            }
            Object[] options = {"Skip", "Update Missing Data", "Replace Old Entries"};
            int choice = JOptionPane.showOptionDialog(this, message.toString(), "Duplicate Entries Found",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

            switch (choice) {
                case 0: // Skip
                    statusLabel.setText("Duplicates skipped.");
                    break;
                case 1: // Update Missing Data
                    updateMissingData(devicesToCheck, duplicates);
                    break;
                case 2: // Replace Old Entries
                    replaceOldEntries(devicesToCheck, duplicates);
                    break;
                default: // Cancel or close
                    statusLabel.setText("Duplicate handling cancelled.");
                    break;
            }
        } else {
            JOptionPane.showMessageDialog(this, "No duplicates found. Data loaded for review.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateMissingData(List<HashMap<String, String>> devicesToCheck, List<String> duplicates) {
        try {
            for (HashMap<String, String> device : devicesToCheck) {
                String assetName = device.get("AssetName");
                if (duplicates.contains(assetName)) {
                    HashMap<String, String> existingDevice = getDeviceByAssetNameFromDB(assetName);
                    if (existingDevice != null) {
                        boolean updated = false;
                        for (Map.Entry<String, String> entry : device.entrySet()) {
                            String field = entry.getKey();
                            String newValue = entry.getValue();
                            if (newValue != null && !newValue.trim().isEmpty() && 
                                (existingDevice.get(field) == null || existingDevice.get(field).trim().isEmpty())) {
                                existingDevice.put(field, newValue);
                                updated = true;
                            }
                        }
                        if (updated) {
                            updateDeviceInDB(existingDevice);
                        }
                    }
                }
            }
            statusLabel.setText("Missing data updated for duplicates.");
            JOptionPane.showMessageDialog(this, "Missing data updated for duplicates.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            statusLabel.setText("Error updating missing data: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error updating missing data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void replaceOldEntries(List<HashMap<String, String>> devicesToCheck, List<String> duplicates) {
        try {
            for (HashMap<String, String> device : devicesToCheck) {
                String assetName = device.get("AssetName");
                if (duplicates.contains(assetName)) {
                    DatabaseUtils.deleteDevice(assetName);
                    DatabaseUtils.saveDevice(device);
                }
            }
            statusLabel.setText("Old entries replaced with new data.");
            JOptionPane.showMessageDialog(this, "Old entries replaced with new data.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            statusLabel.setText("Error replacing old entries: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error replacing old entries: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private HashMap<String, String> getDeviceByAssetNameFromDB(String assetName) throws SQLException {
        String sql = "SELECT * FROM Inventory WHERE AssetName = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, assetName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    HashMap<String, String> device = new HashMap<>();
                    for (String column : DefaultColumns.getInventoryColumns()) {
                        device.put(column, rs.getString(column));
                    }
                    return device;
                }
            }
        }
        return null;
    }

    private void updateDeviceInDB(HashMap<String, String> device) throws SQLException {
        String sql = SQLGenerator.generateInsertSQL("Inventory", device);
        sql = sql.replace("INSERT INTO", "UPDATE").replace("VALUES", "SET") + " WHERE AssetName = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            String assetName = device.get("AssetName");
            for (Map.Entry<String, String> entry : device.entrySet()) {
                if (!entry.getKey().equals("AssetName")) {
                    stmt.setString(index++, entry.getValue() != null ? entry.getValue() : "");
                }
            }
            stmt.setString(index, assetName);
            stmt.executeUpdate();
        }
    }

    private void saveToDatabase() {
        if (importedData == null || importedData.isEmpty()) {
            statusLabel.setText("No data to save.");
            JOptionPane.showMessageDialog(this, "No data to save.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<HashMap<String, String>> devicesToSave = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            HashMap<String, String> device = new HashMap<>();
            for (int j = 0; j < tableColumns.length; j++) {
                String dbField = tableColumns[j].replace(" ", "_");
                String value = (String) tableModel.getValueAt(i, j);
                if (value != null && !value.trim().isEmpty()) {
                    device.put(dbField, value);
                }
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