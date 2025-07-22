package data_import;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import utils.DataEntry;
import utils.DefaultColumns;
import utils.SQLGenerator;

public class DatabaseHandler {
    private static final String DB_URL = "jdbc:ucanaccess://C:/Users/ami6985/OneDrive - AISIN WORLD CORP/Documents/InventoryManagement.accdb";
    private static final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public DatabaseHandler() {
    }

    private Connection getConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(DB_URL);
    }

    public void saveDevice(HashMap<String, String> device) throws SQLException {
        String sql = SQLGenerator.generateInsertSQL("Inventory", device);
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (Map.Entry<String, String> entry : device.entrySet()) {
                String column = entry.getKey();
                String value = entry.getValue();
                if (DefaultColumns.getInventoryColumnDefinitions().containsKey(column) &&
                    DefaultColumns.getInventoryColumnDefinitions().get(column).equals("DATE")) {
                    if (value == null || value.trim().isEmpty()) {
                        stmt.setNull(index++, java.sql.Types.DATE);
                    } else {
                        try {
                            java.util.Date date = dbDateFormat.parse(value);
                            stmt.setDate(index++, new java.sql.Date(date.getTime()));
                        } catch (java.text.ParseException e) {
                            Logger.getLogger(DatabaseHandler.class.getName()).log(
                                Level.SEVERE, "Failed to parse date for column {0}: {1}",
                                new Object[]{column, e.getMessage()});
                            stmt.setNull(index++, java.sql.Types.DATE);
                        }
                    }
                } else {
                    stmt.setString(index++, value != null ? value : "");
                }
            }
            stmt.executeUpdate();
        }
    }

    public void updateDeviceInDB(HashMap<String, String> device) throws SQLException {
        String sql = SQLGenerator.generateInsertSQL("Inventory", device);
        sql = sql.replace("INSERT INTO", "UPDATE").replace("VALUES", "SET") + " WHERE AssetName = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            String assetName = device.get("AssetName");
            for (Map.Entry<String, String> entry : device.entrySet()) {
                String column = entry.getKey();
                if (!column.equals("AssetName")) {
                    String value = entry.getValue();
                    if (DefaultColumns.getInventoryColumnDefinitions().containsKey(column) &&
                        DefaultColumns.getInventoryColumnDefinitions().get(column).equals("DATE")) {
                        if (value == null || value.trim().isEmpty()) {
                            stmt.setNull(index++, java.sql.Types.DATE);
                        } else {
                            try {
                                java.util.Date date = dbDateFormat.parse(value);
                                stmt.setDate(index++, new java.sql.Date(date.getTime()));
                            } catch (java.text.ParseException e) {
                                Logger.getLogger(DatabaseHandler.class.getName()).log(
                                    Level.SEVERE, "Failed to parse date for column {0}: {1}",
                                    new Object[]{column, e.getMessage()});
                                stmt.setNull(index++, java.sql.Types.DATE);
                            }
                        }
                    } else {
                        stmt.setString(index++, value != null ? value : "");
                    }
                }
            }
            stmt.setString(index, assetName);
            stmt.executeUpdate();
        }
    }

    public HashMap<String, String> getDeviceByAssetNameFromDB(String assetName) throws SQLException {
        String sql = "SELECT * FROM Inventory WHERE AssetName = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, assetName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    HashMap<String, String> device = new HashMap<>();
                    for (String column : DefaultColumns.getInventoryColumns()) {
                        String value = rs.getString(column);
                        if (value != null && DefaultColumns.getInventoryColumnDefinitions().getOrDefault(column, "").equals("DATE")) {
                            try {
                                // Normalize database date (e.g., 2025-07-10 00:00:00.000000) to yyyy-MM-dd
                                if (value.contains(" ")) {
                                    value = value.split(" ")[0];
                                }
                                java.util.Date date = new SimpleDateFormat("yyyy-MM-dd").parse(value);
                                value = dbDateFormat.format(date);
                            } catch (java.text.ParseException e) {
                                Logger.getLogger(DatabaseHandler.class.getName()).log(
                                    Level.WARNING, "Failed to normalize date for column {0}: {1}",
                                    new Object[]{column, value});
                            }
                        }
                        device.put(column, value);
                    }
                    return device;
                }
            }
        }
        return null;
    }

    public void addNewField(String tableName, String fieldName, String fieldType) throws SQLException {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN [" + fieldName + "] " + fieldType;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public List<String> getDeviceTypesFromDB() throws SQLException {
        Set<String> deviceTypes = new HashSet<>();
        String sql = "SELECT [Device_Type] FROM Inventory WHERE [Device_Type] IS NOT NULL";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String type = rs.getString("Device_Type");
                if (type != null && !type.trim().isEmpty()) {
                    deviceTypes.add(type);
                }
            }
        }
        return new ArrayList<>(deviceTypes);
    }

    public List<DataEntry> loadAllDevices() throws SQLException {
        List<DataEntry> devices = new ArrayList<>();
        String sql = "SELECT * FROM Inventory";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            String[] columns = DefaultColumns.getInventoryColumns();
            while (rs.next()) {
                HashMap<String, String> device = new HashMap<>();
                String[] values = new String[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    String value = rs.getString(columns[i]);
                    if (value != null && DefaultColumns.getInventoryColumnDefinitions().getOrDefault(columns[i], "").equals("DATE")) {
                        try {
                            // Normalize database date to yyyy-MM-dd
                            if (value.contains(" ")) {
                                value = value.split(" ")[0];
                            }
                            java.util.Date date = new SimpleDateFormat("yyyy-MM-dd").parse(value);
                            value = dbDateFormat.format(date);
                        } catch (java.text.ParseException e) {
                            Logger.getLogger(DatabaseHandler.class.getName()).log(
                                Level.WARNING, "Failed to normalize date for column {0}: {1}",
                                new Object[]{columns[i], value});
                        }
                    }
                    device.put(columns[i], value);
                    values[i] = value != null ? value : "";
                }
                devices.add(new DataEntry(values, device));
            }
        }
        return devices;
    }
}