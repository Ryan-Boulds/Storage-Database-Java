package data_import;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class DataDisplayManager {
    private final ImportDataTab parent;
    private final javax.swing.JLabel statusLabel;
    private final List<utils.DataEntry> originalData;
    private final Map<String, String> fieldTypes;
    private final HashMap<Integer, String> rowStatus;
    private static final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public DataDisplayManager(ImportDataTab parent, javax.swing.JLabel statusLabel) {
        this.parent = parent;
        this.statusLabel = statusLabel;
        this.originalData = new ArrayList<>();
        this.fieldTypes = new HashMap<>();
        this.rowStatus = new HashMap<>();
    }

    public void displayData(Map<String, String> columnMappings, Map<String, String> newFields, Map<String, String> deviceTypeMappings, List<String[]> importedData) {
        DefaultTableModel tableModel = parent.getTableModel();
        tableModel.setRowCount(0);
        fieldTypes.clear();
        fieldTypes.putAll(newFields);
        rowStatus.clear();

        java.util.logging.Logger.getLogger(DataDisplayManager.class.getName()).log(
            Level.INFO, "DefaultColumns Inventory Columns: {0}", new Object[]{java.util.Arrays.toString(parent.getTableColumns())});
        java.util.logging.Logger.getLogger(DataDisplayManager.class.getName()).log(
            Level.INFO, "Column Mappings: {0}", new Object[]{columnMappings});
        java.util.logging.Logger.getLogger(DataDisplayManager.class.getName()).log(
            Level.INFO, "Device Type Mappings: {0}", new Object[]{deviceTypeMappings});

        for (Map.Entry<String, String> entry : newFields.entrySet()) {
            try {
                parent.getDatabaseHandler().addNewField("Inventory", entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                String errorMessage = "Error adding new field '" + entry.getKey() + "': " + e.getMessage();
                statusLabel.setText(errorMessage);
                JOptionPane.showMessageDialog(parent, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        originalData.clear();
        originalData.addAll(new DataProcessor().processData(importedData, columnMappings, deviceTypeMappings, parent.getTableColumns(), fieldTypes));
        for (int i = 0; i < originalData.size(); i++) {
            rowStatus.put(i, computeRowStatus(i, originalData.get(i)));
            if (parent.isShowDuplicates() || !rowStatus.get(i).equals("red")) {
                tableModel.addRow(originalData.get(i).getValues());
            }
        }
        ((TableColorRenderer) parent.getTable().getDefaultRenderer(Object.class)).setRowStatus(rowStatus);
        java.util.logging.Logger.getLogger(DataDisplayManager.class.getName()).log(
            Level.INFO, "Displayed {0} rows in table.", new Object[]{tableModel.getRowCount()});

        if (!newFields.isEmpty()) {
            StringBuilder newFieldsMessage = new StringBuilder("New fields added to database schema:\n");
            for (Map.Entry<String, String> entry : newFields.entrySet()) {
                newFieldsMessage.append(entry.getKey()).append(" (").append(entry.getValue()).append(")\n");
            }
            statusLabel.setText("New fields added.");
            JOptionPane.showMessageDialog(parent, newFieldsMessage.toString(), "New Fields Added", JOptionPane.INFORMATION_MESSAGE);
        }

        if (!deviceTypeMappings.isEmpty()) {
            StringBuilder newTypesMessage = new StringBuilder("New Device Types added:\n");
            for (String newType : deviceTypeMappings.values()) {
                newTypesMessage.append(newType).append("\n");
            }
            statusLabel.setText("New device types added.");
            JOptionPane.showMessageDialog(parent, newTypesMessage.toString(), "New Device Types Added", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public String normalizeDateValue(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        if (utils.DefaultColumns.getInventoryColumnDefinitions().getOrDefault(field, "").equals("DATE")) {
            try {
                if (value.contains(" ")) {
                    value = value.split(" ")[0];
                }
                for (SimpleDateFormat df : new SimpleDateFormat[]{
                    new SimpleDateFormat("yyyy-MM-dd"),
                    new SimpleDateFormat("MM/dd/yyyy"),
                    new SimpleDateFormat("dd-MM-yyyy")
                }) {
                    try {
                        return dbDateFormat.format(df.parse(value));
                    } catch (java.text.ParseException e) {
                        // Try next format
                    }
                }
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(DataDisplayManager.class.getName()).log(
                    Level.WARNING, "Failed to normalize date for field {0}: {1}", new Object[]{field, value});
            }
        }
        return value;
    }

    public String computeRowStatus(int rowIndex, utils.DataEntry entry) {
        String assetName = entry.getData().get("AssetName");
        String status = "white"; // Default to white (new entry)
        if (assetName != null && !assetName.trim().isEmpty()) {
            try {
                HashMap<String, String> existingDevice = parent.getDatabaseHandler().getDeviceByAssetNameFromDB(assetName);
                if (existingDevice != null) {
                    boolean isExactMatch = true;
                    boolean hasNewData = false;
                    boolean hasConflict = false;
                    for (Map.Entry<String, String> field : entry.getData().entrySet()) {
                        String key = field.getKey();
                        String newValue = field.getValue();
                        String oldValue = existingDevice.get(key);
                        String newVal = (newValue == null || newValue.trim().isEmpty()) ? "" : newValue;
                        String oldVal = normalizeDateValue(oldValue != null ? oldValue : "", key);
                        if (!newVal.equals(oldVal)) {
                            isExactMatch = false;
                            if (newVal.isEmpty()) {
                                entry.getData().put(key, oldVal);
                            } else if (oldVal.isEmpty()) {
                                hasNewData = true;
                            } else {
                                hasConflict = true;
                            }
                        }
                    }
                    if (isExactMatch) {
                        status = "red"; // Exact duplicate, no new data
                    } else if (hasConflict) {
                        status = "yellow"; // Conflict in non-empty fields
                    } else if (hasNewData) {
                        status = "green"; // New data to overwrite
                    }
                }
            } catch (SQLException e) {
                java.util.logging.Logger.getLogger(DataDisplayManager.class.getName()).log(
                    Level.INFO, "Error checking row status for AssetName {0}: {1}",
                    new Object[]{assetName, e.getMessage()});
            }
        }
        if (entry.isResolved()) {
            status = status.equals("red") ? "red" : "green"; // Green unless exact match
        }
        return status;
    }

    public void updateTableDisplay() {
        DefaultTableModel tableModel = parent.getTableModel();
        tableModel.setRowCount(0);
        TableColorRenderer renderer = (TableColorRenderer) parent.getTable().getDefaultRenderer(Object.class);
        for (int i = 0; i < originalData.size(); i++) {
            utils.DataEntry entry = originalData.get(i);
            if (parent.isShowDuplicates() || !renderer.isExactDuplicate(i)) {
                tableModel.addRow(entry.getValues());
            }
        }
        parent.getTable().repaint();
    }

    public List<utils.DataEntry> getOriginalData() {
        return originalData;
    }

    public Map<String, String> getFieldTypes() {
        return fieldTypes;
    }

    public HashMap<Integer, String> getRowStatus() {
        return rowStatus;
    }
}