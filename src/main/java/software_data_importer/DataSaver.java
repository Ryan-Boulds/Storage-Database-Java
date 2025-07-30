package software_data_importer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

public class DataSaver {
    private final ImportDataTab parent;
    private final javax.swing.JLabel statusLabel;
    private static final Logger LOGGER = Logger.getLogger(DataSaver.class.getName());

    public DataSaver(ImportDataTab parent, javax.swing.JLabel statusLabel) {
        this.parent = parent;
        this.statusLabel = statusLabel;
    }

    public void saveToDatabase() {
        List<String[]> importedData = parent.getOriginalData().stream().map(utils.DataEntry::getValues).collect(Collectors.toList());
        if (importedData.isEmpty()) {
            statusLabel.setText("No data to save.");
            JOptionPane.showMessageDialog(parent, "No data to save.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean allGreenOrWhite = true;
        List<utils.DataEntry> redRows = new ArrayList<>();
        List<utils.DataEntry> yellowRows = new ArrayList<>();
        List<utils.DataEntry> greenRows = new ArrayList<>();
        List<utils.DataEntry> whiteRows = new ArrayList<>();

        for (int i = 0; i < parent.getOriginalData().size(); i++) {
            utils.DataEntry entry = parent.getOriginalData().get(i);
            String status = parent.getRowStatus().get(i);
            if (status.equals("red")) {
                redRows.add(entry);
                allGreenOrWhite = false;
            } else if (status.equals("yellow")) {
                yellowRows.add(entry);
                allGreenOrWhite = false;
            } else if (status.equals("green") || entry.isResolved()) {
                greenRows.add(entry);
            } else {
                whiteRows.add(entry);
            }
        }

        String tableName = parent.getSelectedTable();
        if (allGreenOrWhite) {
            int savedCount = 0;
            List<utils.DataEntry> dataToRemove = new ArrayList<>();
            for (utils.DataEntry entry : whiteRows) {
                try {
                    HashMap<String, String> cleanedDevice = cleanDeviceData(entry.getData());
                    utils.DatabaseUtils.saveDevice(tableName, cleanedDevice);
                    savedCount++;
                    dataToRemove.add(entry);
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error saving new device for AssetName {0} to table {1}: {2}", 
                               new Object[]{entry.getData().get("AssetName"), tableName, e.getMessage()});
                    String errorMessage = "Error saving new device for AssetName " + entry.getData().get("AssetName") + ": " + e.getMessage();
                    statusLabel.setText(errorMessage);
                    JOptionPane.showMessageDialog(parent, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            for (utils.DataEntry entry : greenRows) {
                try {
                    HashMap<String, String> cleanedDevice = cleanDeviceData(entry.getData());
                    utils.DatabaseUtils.updateDevice(tableName, cleanedDevice);
                    savedCount++;
                    dataToRemove.add(entry);
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error updating device for AssetName {0} in table {1}: {2}", 
                               new Object[]{entry.getData().get("AssetName"), tableName, e.getMessage()});
                    String errorMessage = "Error updating device for AssetName " + entry.getData().get("AssetName") + ": " + e.getMessage();
                    statusLabel.setText(errorMessage);
                    JOptionPane.showMessageDialog(parent, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            parent.getOriginalData().removeAll(dataToRemove);
            parent.getRowStatus().clear();
            for (int i = 0; i < parent.getOriginalData().size(); i++) {
                String status = parent.dataDisplayManager.computeRowStatus(i, parent.getOriginalData().get(i));
                parent.getRowStatus().put(i, status);
            }
            parent.dataDisplayManager.updateTableDisplay();

            statusLabel.setText("Data saved to table " + tableName + " successfully!");
            LOGGER.log(Level.INFO, "Successfully saved {0} devices to table {1}.", new Object[]{savedCount, tableName});
            JOptionPane.showMessageDialog(parent, "Data saved to table " + tableName + " successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder message = new StringBuilder("Found potential issues:\n");
        if (!redRows.isEmpty()) {
            message.append("- ").append(redRows.size()).append(" exact duplicates (red)\n");
        }
        if (!yellowRows.isEmpty()) {
            message.append("- ").append(yellowRows.size()).append(" conflicting entries (yellow)\n");
        }
        message.append("\nChoose an action:");
        String[] options = {"Update Missing Fields Only", "Overwrite Conflicts", "Skip Conflicts", "Cancel"};
        int choice = JOptionPane.showOptionDialog(parent, message.toString(), "Resolve Conflicts",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        try {
            List<utils.DataEntry> dataToSave = new ArrayList<>();
            switch (choice) {
                case 0: // Update Missing Fields Only
                    dataToSave.addAll(whiteRows);
                    dataToSave.addAll(greenRows);
                    for (utils.DataEntry entry : yellowRows) {
                        HashMap<String, String> mergedDevice = new HashMap<>();
                        HashMap<String, String> existingDevice = parent.getDatabaseHandler().getDeviceByAssetNameFromDB(tableName, entry.getData().get("AssetName"));
                        if (existingDevice != null) {
                            mergedDevice.putAll(existingDevice);
                            for (Map.Entry<String, String> field : entry.getData().entrySet()) {
                                String key = field.getKey();
                                String newValue = field.getValue();
                                if (newValue != null && !newValue.trim().isEmpty() && 
                                    (mergedDevice.get(key) == null || mergedDevice.get(key).trim().isEmpty())) {
                                    mergedDevice.put(key, newValue);
                                }
                            }
                            dataToSave.add(new utils.DataEntry(entry.getValues(), mergedDevice));
                        }
                    }
                    break;
                case 1: // Overwrite Conflicts
                    dataToSave.addAll(whiteRows);
                    dataToSave.addAll(greenRows);
                    dataToSave.addAll(yellowRows);
                    break;
                case 2: // Skip Conflicts
                    dataToSave.addAll(whiteRows);
                    dataToSave.addAll(greenRows);
                    break;
                case 3: // Cancel
                    statusLabel.setText("Save operation cancelled.");
                    return;
                default:
                    statusLabel.setText("Invalid option selected.");
                    return;
            }

            int savedCount = 0;
            List<utils.DataEntry> dataToRemove = new ArrayList<>();
            for (utils.DataEntry entry : dataToSave) {
                HashMap<String, String> device = entry.getData();
                String assetName = device.get("AssetName");
                if (assetName != null && !assetName.trim().isEmpty()) {
                    HashMap<String, String> existingDevice = parent.getDatabaseHandler().getDeviceByAssetNameFromDB(tableName, assetName);
                    try {
                        if (existingDevice != null) {
                            HashMap<String, String> cleanedDevice = cleanDeviceData(device);
                            utils.DatabaseUtils.updateDevice(tableName, cleanedDevice);
                        } else {
                            HashMap<String, String> cleanedDevice = cleanDeviceData(device);
                            utils.DatabaseUtils.saveDevice(tableName, cleanedDevice);
                        }
                        savedCount++;
                        dataToRemove.add(entry);
                    } catch (SQLException e) {
                        LOGGER.log(Level.SEVERE, "Error saving/updating device for AssetName {0} in table {1}: {2}", 
                                   new Object[]{assetName, tableName, e.getMessage()});
                        throw e;
                    }
                }
            }
            parent.getOriginalData().removeAll(dataToRemove);
            parent.getRowStatus().clear();
            for (int i = 0; i < parent.getOriginalData().size(); i++) {
                String status = parent.dataDisplayManager.computeRowStatus(i, parent.getOriginalData().get(i));
                parent.getRowStatus().put(i, status);
            }
            parent.dataDisplayManager.updateTableDisplay();

            statusLabel.setText("Data saved to table " + tableName + " successfully!");
            LOGGER.log(Level.INFO, "Successfully saved {0} devices to table {1} after conflict resolution.", 
                       new Object[]{savedCount, tableName});
            JOptionPane.showMessageDialog(parent, "Data saved to table " + tableName + " successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving to table {0}: {1}", new Object[]{tableName, e.getMessage()});
            String errorMessage = "Error saving to table " + tableName + ": " + e.getMessage();
            statusLabel.setText(errorMessage);
            JOptionPane.showMessageDialog(parent, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private HashMap<String, String> cleanDeviceData(HashMap<String, String> device) {
        HashMap<String, String> cleanedDevice = new HashMap<>(device);
        for (Map.Entry<String, String> entry : cleanedDevice.entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue();
            String fieldType = parent.getFieldTypes().getOrDefault(field, "");
            if (fieldType.equalsIgnoreCase("DATE") && (value == null || value.trim().isEmpty())) {
                cleanedDevice.put(field, null);
            }
        }
        return cleanedDevice;
    }
}