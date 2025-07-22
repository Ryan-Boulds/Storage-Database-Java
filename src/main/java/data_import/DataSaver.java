package data_import;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

public class DataSaver {
    private final ImportDataTab parent;
    private final javax.swing.JLabel statusLabel;

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

        if (allGreenOrWhite) {
            for (utils.DataEntry entry : whiteRows) {
                try {
                    HashMap<String, String> cleanedDevice = cleanDeviceData(entry.getData());
                    parent.getDatabaseHandler().saveDevice(cleanedDevice);
                } catch (SQLException e) {
                    String errorMessage = "Error saving new device for AssetName " + entry.getData().get("AssetName") + ": " + e.getMessage();
                    statusLabel.setText(errorMessage);
                    JOptionPane.showMessageDialog(parent, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            for (utils.DataEntry entry : greenRows) {
                try {
                    HashMap<String, String> cleanedDevice = cleanDeviceData(entry.getData());
                    parent.getDatabaseHandler().updateDeviceInDB(cleanedDevice);
                } catch (SQLException e) {
                    String errorMessage = "Error updating device for AssetName " + entry.getData().get("AssetName") + ": " + e.getMessage();
                    statusLabel.setText(errorMessage);
                    JOptionPane.showMessageDialog(parent, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            statusLabel.setText("Data saved to database successfully!");
            JOptionPane.showMessageDialog(parent, "Data saved to database successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
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
        String[] options = {"Merge All", "Overwrite Conflicts", "Skip Conflicts", "Cancel"};
        int choice = JOptionPane.showOptionDialog(parent, message.toString(), "Resolve Conflicts",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        try {
            List<utils.DataEntry> dataToSave = new ArrayList<>();
            switch (choice) {
                case 0: // Merge All
                    dataToSave.addAll(whiteRows);
                    dataToSave.addAll(greenRows);
                    for (utils.DataEntry entry : yellowRows) {
                        HashMap<String, String> mergedDevice = new HashMap<>(entry.getData());
                        HashMap<String, String> existingDevice = parent.getDatabaseHandler().getDeviceByAssetNameFromDB(mergedDevice.get("AssetName"));
                        for (String key : existingDevice.keySet()) {
                            String existingValue = parent.getOriginalData().stream()
                                .filter(e -> e.getData().get("AssetName").equals(mergedDevice.get("AssetName")))
                                .findFirst()
                                .map(e -> parent.getOriginalData().indexOf(e))
                                .map(idx -> parent.getRowStatus().get(idx))
                                .map(status -> parent.getOriginalData().stream()
                                    .filter(e -> e.getData().get("AssetName").equals(mergedDevice.get("AssetName")))
                                    .findFirst()
                                    .map(e -> parent.getOriginalData().indexOf(e))
                                    .map(idx2 -> parent.getOriginalData().get(idx2).getData().get(key))
                                    .orElse(""))
                                .orElse("");
                            if (!mergedDevice.containsKey(key) || mergedDevice.get(key).trim().isEmpty()) {
                                mergedDevice.put(key, existingValue);
                            }
                        }
                        dataToSave.add(new utils.DataEntry(entry.getValues(), mergedDevice));
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

            for (utils.DataEntry entry : dataToSave) {
                HashMap<String, String> device = entry.getData();
                String assetName = device.get("AssetName");
                if (assetName != null && !assetName.trim().isEmpty()) {
                    HashMap<String, String> existingDevice = parent.getDatabaseHandler().getDeviceByAssetNameFromDB(assetName);
                    if (existingDevice != null) {
                        HashMap<String, String> cleanedDevice = cleanDeviceData(device);
                        parent.getDatabaseHandler().updateDeviceInDB(cleanedDevice);
                    } else {
                        HashMap<String, String> cleanedDevice = cleanDeviceData(device);
                        parent.getDatabaseHandler().saveDevice(cleanedDevice);
                    }
                }
            }
            statusLabel.setText("Data saved to database successfully!");
            JOptionPane.showMessageDialog(parent, "Data saved to database successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            String errorMessage = "Error saving to database: " + e.getMessage();
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
            if (fieldType.equalsIgnoreCase("DATETIME") && (value == null || value.trim().isEmpty())) {
                cleanedDevice.put(field, null);
                java.util.logging.Logger.getLogger(DataSaver.class.getName()).log(
                    Level.INFO, "Cleaned DATETIME field {0} for AssetName {1}: set to null",
                    new Object[]{field, device.get("AssetName")});
            }
        }
        return cleanedDevice;
    }
}