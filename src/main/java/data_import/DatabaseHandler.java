package data_import;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHandler {
    public void addNewField(String tableName, String fieldName, String fieldType) throws SQLException {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + fieldName + " " + fieldType;
        try (Connection conn = utils.DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public HashMap<String, String> getDeviceByAssetNameFromDB(String assetName) throws SQLException {
        String sql = "SELECT * FROM Inventory WHERE AssetName = ?";
        try (Connection conn = utils.DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, assetName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    HashMap<String, String> device = new HashMap<>();
                    for (String column : utils.DefaultColumns.getInventoryColumns()) {
                        device.put(column, rs.getString(column));
                    }
                    return device;
                }
            }
        }
        return null;
    }

    public void saveDevice(HashMap<String, String> device) throws SQLException {
        utils.DatabaseUtils.saveDevice(device);
    }

    public void deleteDevice(String assetName) throws SQLException {
        utils.DatabaseUtils.deleteDevice(assetName);
    }

    public void updateDeviceInDB(HashMap<String, String> device) throws SQLException {
        String sql = utils.SQLGenerator.generateInsertSQL("Inventory", device);
        sql = sql.replace("INSERT INTO", "UPDATE").replace("VALUES", "SET") + " WHERE AssetName = ?";
        try (Connection conn = utils.DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            String assetName = device.get("AssetName");
            for (java.util.Map.Entry<String, String> entry : device.entrySet()) {
                if (!entry.getKey().equals("AssetName")) {
                    stmt.setString(index++, entry.getValue() != null ? entry.getValue() : "");
                }
            }
            stmt.setString(index, assetName);
            stmt.executeUpdate();
        }
    }

    public void mergeDeviceInDB(HashMap<String, String> device) throws SQLException {
        HashMap<String, String> existingDevice = getDeviceByAssetNameFromDB(device.get("AssetName"));
        if (existingDevice != null) {
            HashMap<String, String> mergedDevice = new HashMap<>(existingDevice);
            for (Map.Entry<String, String> entry : device.entrySet()) {
                String key = entry.getKey();
                String newValue = entry.getValue();
                String oldValue = existingDevice.get(key);
                if (newValue != null && !newValue.trim().isEmpty() && 
                    (oldValue == null || oldValue.trim().isEmpty())) {
                    mergedDevice.put(key, newValue);
                }
            }
            updateDeviceInDB(mergedDevice);
        }
    }
}