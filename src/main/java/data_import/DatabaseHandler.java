package data_import;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
    private static final Logger LOGGER = Logger.getLogger(DatabaseHandler.class.getName());

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
                String fieldType = DefaultColumns.getInventoryColumnDefinitions().getOrDefault(column, "");

                switch (fieldType) {
                    case "DATE":
                        if (value == null || value.trim().isEmpty()) {
                            stmt.setNull(index++, Types.DATE);
                        } else {
                            try {
                                java.util.Date date = dbDateFormat.parse(value);
                                stmt.setDate(index++, new java.sql.Date(date.getTime()));
                            } catch (java.text.ParseException e) {
                                LOGGER.log(Level.SEVERE, "Failed to parse date for column {0} in save: {1}", new Object[]{column, e.getMessage()});
                                stmt.setNull(index++, Types.DATE);
                            }
                        }   break;
                    case "DOUBLE":
                        if (value == null || value.trim().isEmpty()) {
                            stmt.setNull(index++, Types.DOUBLE);
                        } else {
                            try {
                                stmt.setDouble(index++, Double.parseDouble(value));
                            } catch (NumberFormatException e) {
                                LOGGER.log(Level.WARNING, "Invalid DOUBLE value for column {0}: {1}", new Object[]{column, value});
                                stmt.setNull(index++, Types.DOUBLE);
                            }
                        }   break;
                    default:
                        stmt.setString(index++, value != null ? value : "");
                        break;
                }
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving device: {0}", e.getMessage());
            throw e;
        }
    }

    public void updateDeviceInDB(HashMap<String, String> device) throws SQLException {
        if (!device.containsKey("AssetName") || device.get("AssetName") == null || device.get("AssetName").trim().isEmpty()) {
            throw new SQLException("AssetName is required for update");
        }

        StringBuilder sql = new StringBuilder("UPDATE Inventory SET ");
        List<String> columns = new ArrayList<>();
        for (String column : device.keySet()) {
            if (!column.equals("AssetName")) {
                String normalizedColumn = column.replace("PROCCESSOR", "PROCESSOR")
                                               .replace("LAST_SCUCCESSFUL SCAN", "LAST_SUCCESSFUL_SCAN");
                columns.add("[" + normalizedColumn + "] = ?");
            }
        }
        if (columns.isEmpty()) {
            throw new SQLException("No columns to update for device with AssetName: " + device.get("AssetName"));
        }
        sql.append(String.join(", ", columns)).append(" WHERE AssetName = ?");

        String debugSql = sql.toString();
        LOGGER.log(Level.INFO, "Generated UPDATE SQL: {0}", debugSql);
        LOGGER.log(Level.INFO, "Parameters: {0}", device);

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int index = 1;
            String assetName = device.get("AssetName");

            for (Map.Entry<String, String> entry : device.entrySet()) {
                String column = entry.getKey();
                if (!column.equals("AssetName")) {
                    String value = entry.getValue();
                    String fieldType = DefaultColumns.getInventoryColumnDefinitions().getOrDefault(column, "");

                    switch (fieldType) {
                        case "DATE":
                            if (value == null || value.trim().isEmpty()) {
                                stmt.setNull(index++, Types.DATE);
                            } else {
                                try {
                                    java.util.Date date = dbDateFormat.parse(value);
                                    stmt.setDate(index++, new java.sql.Date(date.getTime()));
                                } catch (java.text.ParseException e) {
                                    LOGGER.log(Level.SEVERE, "Failed to parse date for column {0}: {1}", new Object[]{column, value});
                                    stmt.setNull(index++, Types.DATE);
                                }
                            }   break;
                        case "DOUBLE":
                            if (value == null || value.trim().isEmpty()) {
                                stmt.setNull(index++, Types.DOUBLE);
                            } else {
                                try {
                                    stmt.setDouble(index++, Double.parseDouble(value));
                                } catch (NumberFormatException e) {
                                    LOGGER.log(Level.WARNING, "Invalid DOUBLE value for column {0}: {1}", new Object[]{column, value});
                                    stmt.setNull(index++, Types.DOUBLE);
                                }
                            }   break;
                        default:
                            stmt.setString(index++, value != null ? value : "");
                            break;
                    }
                }
            }
            stmt.setString(index, assetName);
            LOGGER.log(Level.INFO, "Executing UPDATE SQL for AssetName: {0}", assetName);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                LOGGER.log(Level.WARNING, "No rows updated for AssetName: {0}", assetName);
                throw new SQLException("No device found with AssetName: " + assetName);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating device with AssetName {0}: {1}\nSQL: {2}", 
                       new Object[]{device.get("AssetName"), e.getMessage(), debugSql});
            throw e;
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
                                if (value.contains(" ")) {
                                    value = value.split(" ")[0];
                                }
                                java.util.Date date = new SimpleDateFormat("yyyy-MM-dd").parse(value);
                                value = dbDateFormat.format(date);
                            } catch (java.text.ParseException e) {
                                LOGGER.log(Level.WARNING, "Failed to normalize date for column {0}: {1}", new Object[]{column, value});
                            }
                        }
                        device.put(column, value);
                    }
                    return device;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving device with AssetName {0}: {1}", new Object[]{assetName, e.getMessage()});
            throw e;
        }
        return null;
    }

    public void addNewField(String tableName, String fieldName, String fieldType) throws SQLException {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN [" + fieldName + "] " + fieldType;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding new field {0}: {1}", new Object[]{fieldName, e.getMessage()});
            throw e;
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
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving device types: {0}", e.getMessage());
            throw e;
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
                            if (value.contains(" ")) {
                                value = value.split(" ")[0];
                            }
                            java.util.Date date = new SimpleDateFormat("yyyy-MM-dd").parse(value);
                            value = dbDateFormat.format(date);
                        } catch (java.text.ParseException e) {
                            LOGGER.log(Level.WARNING, "Failed to normalize date for column {0}: {1}", new Object[]{columns[i], value});
                        }
                    }
                    device.put(columns[i], value);
                    values[i] = value != null ? value : "";
                }
                devices.add(new DataEntry(values, device));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading devices: {0}", e.getMessage());
            throw e;
        }
        return devices;
    }
}