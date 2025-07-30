package software_data_importer;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class DataDisplayManager {
    private final ImportDataTab parent;
    private final javax.swing.JLabel statusLabel;
    private final List<utils.DataEntry> originalData;
    private final Map<String, String> fieldTypes;
    private final HashMap<Integer, String> rowStatus;
    private static final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final Logger LOGGER = Logger.getLogger(DataDisplayManager.class.getName());

    public DataDisplayManager(ImportDataTab parent, javax.swing.JLabel statusLabel) {
        this.parent = parent;
        this.statusLabel = statusLabel;
        this.originalData = new ArrayList<>();
        this.fieldTypes = new HashMap<>();
        this.rowStatus = new HashMap<>();
    }

    public void displayData(Map<String, String> columnMappings, Map<String, String> deviceTypeMappings, 
                           List<String[]> importedData, String tableName) {
        DefaultTableModel tableModel = parent.getTableModel();
        tableModel.setRowCount(0);
        fieldTypes.clear();
        try {
            fieldTypes.putAll(utils.DatabaseUtils.getInventoryColumnTypes(tableName));
        } catch (SQLException e) {
            String errorMessage = "Error retrieving column types for table " + tableName + ": " + e.getMessage();
            statusLabel.setText(errorMessage);
            LOGGER.log(Level.SEVERE, errorMessage, e);
            JOptionPane.showMessageDialog(parent, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        originalData.clear();
        originalData.addAll(new DataProcessor().processData(importedData, columnMappings, deviceTypeMappings, 
                                                           parent.getTableColumns(), fieldTypes));
        for (int i = 0; i < originalData.size(); i++) {
            rowStatus.put(i, computeRowStatus(i, originalData.get(i)));
            LOGGER.log(Level.INFO, "Row {0} status computed as: {1}", new Object[]{i, rowStatus.get(i)});
        }
        updateTableDisplay();

        statusLabel.setText("Data displayed for review with " + originalData.size() + " rows.");
        LOGGER.log(Level.INFO, "Successfully displayed {0} rows for review in table {1}.", 
                   new Object[]{originalData.size(), tableName});
    }

    public String normalizeDateValue(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        if (fieldTypes.getOrDefault(field, "").equals("DATE")) {
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
                LOGGER.log(Level.WARNING, "Failed to normalize date for field {0}: {1}", new Object[]{field, value});
            }
        }
        return value;
    }

    public String computeRowStatus(int rowIndex, utils.DataEntry entry) {
        String assetName = entry.getData().get("AssetName");
        String status = "white"; // Default to white (new entry)
        if (assetName != null && !assetName.trim().isEmpty()) {
            try {
                HashMap<String, String> existingDevice = parent.getDatabaseHandler().getDeviceByAssetNameFromDB(parent.getSelectedTable(), assetName);
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
                LOGGER.log(Level.SEVERE, "Error checking row status for AssetName {0} in table {1}: {2}", 
                           new Object[]{assetName, parent.getSelectedTable(), e.getMessage()});
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
        renderer.setRowStatus(rowStatus); // Ensure renderer has the latest status
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