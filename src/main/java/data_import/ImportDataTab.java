package data_import;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.sql.SQLException;
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
import utils.UIComponentUtils;

public class ImportDataTab extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final String[] dbFields = {
        "Device_Name", "Device_Type", "Brand", "Model", "Serial_Number", "Building_Location",
        "Room_Desk", "Specification", "Processor_Type", "Storage_Capacity", "Network_Address",
        "OS_Version", "Department", "Added_Memory", "Status", "Assigned_User",
        "Warranty_Expiry_Date", "Last_Maintenance", "Maintenance_Due", "Date_Of_Purchase",
        "Purchase_Cost", "Vendor", "Memory_RAM"
    };
    private final String[] tableColumns = {
        "Device Name", "Device Type", "Brand", "Model", "Serial Number", "Status",
        "Department", "Warranty Expiry", "Network Address", "Purchase Cost", "Vendor",
        "OS Version", "Assigned User", "Building Location", "Room/Desk", "Specification",
        "Added Memory", "Added Storage", "Last Maintenance", "Maintenance Due", "Memory (RAM)"
    };
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
            importedData = data;
            MappingDialog mappingDialog = new MappingDialog(this, data, dbFields);
            mappingDialog.showDialog();
            displayData(mappingDialog.getColumnMappings(), mappingDialog.getNewFields());
        }
    }

    private void displayData(Map<String, String> columnMappings, Map<String, String> newFields) {
        tableModel.setRowCount(0);
        this.columnMappings = columnMappings;

        for (Map.Entry<String, String> entry : newFields.entrySet()) {
            try {
                DatabaseUtils.addNewField("Inventory", entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding new field '" + entry.getKey() + "': " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        for (int i = 1; i < importedData.size(); i++) {
            String[] csvRow = importedData.get(i);
            String[] tableRow = new String[tableColumns.length];
            Arrays.fill(tableRow, "");

            for (Map.Entry<String, String> mapping : columnMappings.entrySet()) {
                String csvColumn = mapping.getKey();
                String dbField = mapping.getValue();
                int csvIndex = Arrays.asList(importedData.get(0)).indexOf(csvColumn);
                if (csvIndex >= 0 && csvIndex < csvRow.length) {
                    String value = csvRow[csvIndex];
                    for (int k = 0; k < tableColumns.length; k++) {
                        if (tableColumns[k].replace(" ", "_").equals(dbField)) {
                            tableRow[k] = value;
                            break;
                        }
                    }
                }
            }
            tableModel.addRow(tableRow);
        }

        if (!newFields.isEmpty()) {
            StringBuilder newFieldsMessage = new StringBuilder("New fields added to database schema:\n");
            for (Map.Entry<String, String> entry : newFields.entrySet()) {
                newFieldsMessage.append(entry.getKey()).append(" (").append(entry.getValue()).append(")\n");
            }
            JOptionPane.showMessageDialog(this, newFieldsMessage.toString(), "New Fields Added", JOptionPane.INFORMATION_MESSAGE);
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
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            HashMap<String, String> device = new HashMap<>();
            for (int j = 0; j < tableColumns.length; j++) {
                String dbField = tableColumns[j].replace(" ", "_");
                String value = (String) tableModel.getValueAt(i, j);
                if (value != null && !value.trim().isEmpty()) {
                    device.put(dbField, value);
                }
            }

            String serialNumber = device.get("Serial_Number");
            String error = DataUtils.validateDevice(device, serialNumber);
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