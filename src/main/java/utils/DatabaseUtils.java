package utils;

import java.sql.Connection;
import java.sql.DriverManager;
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

public class DatabaseUtils {
    private static String DB_URL = "jdbc:ucanaccess://C:/Users/ami6985/OneDrive - AISIN WORLD CORP/Documents/InventoryManagement.accdb";
    private static final Logger LOGGER = Logger.getLogger(DatabaseUtils.class.getName());

    public static void setDatabasePath(String path) {
        DB_URL = "jdbc:ucanaccess://" + path.replace("\\", "/");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void saveDevice(HashMap<String, String> device) throws SQLException {
        String sql = SQLGenerator.generateInsertSQL("Inventory", device);
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            SimpleDateFormat excelDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            for (Map.Entry<String, String> entry : device.entrySet()) {
                String column = entry.getKey();
                String value = entry.getValue();
                if (DefaultColumns.getInventoryColumnDefinitions().containsKey(column) &&
                    DefaultColumns.getInventoryColumnDefinitions().get(column).equals("DATE")) {
                    if (value == null || value.trim().isEmpty()) {
                        stmt.setNull(index++, java.sql.Types.DATE);
                    } else {
                        try {
                            java.util.Date date;
                            try {
                                date = excelDateFormat.parse(value);
                            } catch (java.text.ParseException e) {
                                date = dateFormat.parse(value);
                            }
                            stmt.setDate(index++, new java.sql.Date(date.getTime()));
                        } catch (java.text.ParseException e) {
                            LOGGER.log(Level.SEVERE, "Failed to parse date for column {0}: {1}", new Object[]{column, value});
                            stmt.setNull(index++, java.sql.Types.DATE);
                        }
                    }
                } else {
                    stmt.setString(index++, value != null ? value : "");
                }
            }
            LOGGER.log(Level.INFO, "Executing INSERT SQL: {0}", new Object[]{sql});
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting device: {0}", new Object[]{e.getMessage()});
            throw e;
        }
    }

    public static void updateDevice(HashMap<String, String> device) throws SQLException {
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
        LOGGER.log(Level.INFO, "Generated UPDATE SQL: {0}", new Object[]{debugSql});
        LOGGER.log(Level.INFO, "Parameters: {0}", new Object[]{device});

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int index = 1;
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            SimpleDateFormat excelDateFormat = new SimpleDateFormat("yyyy-MM-dd");
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
                                java.util.Date date;
                                try {
                                    date = excelDateFormat.parse(value);
                                } catch (java.text.ParseException e) {
                                    date = dateFormat.parse(value);
                                }
                                stmt.setDate(index++, new java.sql.Date(date.getTime()));
                            } catch (java.text.ParseException e) {
                                LOGGER.log(Level.SEVERE, "Failed to parse date for column {0}: {1}", new Object[]{column, value});
                                stmt.setNull(index++, java.sql.Types.DATE);
                            }
                        }
                    } else {
                        stmt.setString(index++, value != null ? value : "");
                    }
                }
            }
            stmt.setString(index, assetName);
            LOGGER.log(Level.INFO, "Executing UPDATE SQL: {0} for AssetName: {1}", new Object[]{debugSql, assetName});
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                LOGGER.log(Level.WARNING, "No rows updated for AssetName: {0}", new Object[]{assetName});
                throw new SQLException("No device found with AssetName: " + assetName);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating device with AssetName {0}: {1}\nSQL: {2}", new Object[]{device.get("AssetName"), e.getMessage(), debugSql});
            throw e;
        }
    }

    public static void deleteDevice(String assetName) throws SQLException {
        String sql = "DELETE FROM Inventory WHERE AssetName = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, assetName);
            LOGGER.log(Level.INFO, "Executing DELETE SQL: {0}", new Object[]{sql});
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting device with AssetName {0}: {1}", new Object[]{assetName, e.getMessage()});
            throw e;
        }
    }

    public static void updatePeripheralCount(String peripheralType, int countDelta, String category) throws SQLException {
        String table;
        String typeColumn;
        switch (category) {
            case "Cable":
                table = "Cables";
                typeColumn = "Cable_Type";
                break;
            case "Accessory":
                table = "Accessories";
                typeColumn = "Peripheral_Type";
                break;
            case "Adapter":
                table = "Adapters";
                typeColumn = "Adapter_Type";
                break;
            default:
                throw new IllegalArgumentException("Unknown category: " + category);
        }
        String sql = "UPDATE " + table + " SET Count = Count + ? WHERE " + typeColumn + " = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, countDelta);
            stmt.setString(2, peripheralType);
            LOGGER.log(Level.INFO, "Executing UPDATE peripheral SQL: {0}", new Object[]{sql});
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                sql = "INSERT INTO " + table + " (" + typeColumn + ", Count) VALUES (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                    insertStmt.setString(1, peripheralType);
                    insertStmt.setInt(2, countDelta);
                    LOGGER.log(Level.INFO, "Executing INSERT peripheral SQL: {0}", new Object[]{sql});
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating peripheral {0}: {1}", new Object[]{peripheralType, e.getMessage()});
            throw e;
        }
    }

    public static void addNewField(String tableName, String fieldName, String fieldType) throws SQLException {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN [" + fieldName + "] " + fieldType;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            LOGGER.log(Level.INFO, "Executing ALTER TABLE SQL: {0}", new Object[]{sql});
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding new field {0}: {1}", new Object[]{fieldName, e.getMessage()});
            throw e;
        }
    }

    public static List<String> getDeviceTypes() throws SQLException {
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
            LOGGER.log(Level.SEVERE, "Error retrieving device types: {0}", new Object[]{e.getMessage()});
            throw e;
        }
        return new ArrayList<>(deviceTypes);
    }

    public static void saveTemplate(HashMap<String, String> template, String templateName) throws SQLException {
        String sql = SQLGenerator.generateInsertSQL("Templates", template);
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (String value : template.values()) {
                stmt.setString(index++, value != null ? value : "");
            }
            LOGGER.log(Level.INFO, "Executing INSERT template SQL: {0}", new Object[]{sql});
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving template {0}: {1}", new Object[]{templateName, e.getMessage()});
            throw e;
        }
    }

    public static ArrayList<HashMap<String, String>> loadTemplates() throws SQLException {
        ArrayList<HashMap<String, String>> templates = new ArrayList<>();
        String sql = "SELECT * FROM Templates";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                HashMap<String, String> template = new HashMap<>();
                for (String column : DefaultColumns.getInventoryColumns()) {
                    template.put(column, rs.getString(column));
                }
                template.put("Template_Name", rs.getString("Template_Name"));
                templates.add(template);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading templates: {0}", new Object[]{e.getMessage()});
            throw e;
        }
        return templates;
    }

    public static ArrayList<HashMap<String, String>> loadDevices() throws SQLException {
        ArrayList<HashMap<String, String>> devices = new ArrayList<>();
        String sql = "SELECT * FROM Inventory";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                HashMap<String, String> device = new HashMap<>();
                for (String column : DefaultColumns.getInventoryColumns()) {
                    device.put(column, rs.getString(column));
                }
                devices.add(device);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading devices: {0}", new Object[]{e.getMessage()});
            throw e;
        }
        return devices;
    }

    public static ArrayList<HashMap<String, String>> loadPeripherals(String category) throws SQLException {
        ArrayList<HashMap<String, String>> peripherals = new ArrayList<>();
        String table;
        String typeColumn;
        switch (category) {
            case "Cable":
                table = "Cables";
                typeColumn = "Cable_Type";
                break;
            case "Accessory":
                table = "Accessories";
                typeColumn = "Peripheral_Type";
                break;
            case "Adapter":
                table = "Adapters";
                typeColumn = "Adapter_Type";
                break;
            default:
                throw new IllegalArgumentException("Unknown category: " + category);
        }
        String sql = "SELECT " + typeColumn + ", Count FROM " + table;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                HashMap<String, String> peripheral = new HashMap<>();
                peripheral.put(typeColumn, rs.getString(typeColumn));
                peripheral.put("Count", String.valueOf(rs.getInt("Count")));
                peripherals.add(peripheral);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading peripherals for category {0}: {1}", new Object[]{category, e.getMessage()});
            throw e;
        }
        return peripherals;
    }

    public static HashMap<String, String> loadTemplateDetails(String templateName) throws SQLException {
        HashMap<String, String> template = new HashMap<>();
        String sql = "SELECT * FROM Templates WHERE Template_Name = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, templateName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    for (String column : DefaultColumns.getInventoryColumns()) {
                        template.put(column, rs.getString(column));
                    }
                    template.put("Template_Name", rs.getString("Template_Name"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading template details for {0}: {1}", new Object[]{templateName, e.getMessage()});
            throw e;
        }
        return template;
    }

    public static HashMap<String, String> getDeviceByAssetName(String assetName) throws SQLException {
        String sql = "SELECT * FROM Inventory WHERE AssetName = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving device with AssetName {0}: {1}", new Object[]{assetName, e.getMessage()});
            throw e;
        }
        return null;
    }

    public static void deleteDeviceByAssetName(String assetName) throws SQLException {
        deleteDevice(assetName);
    }
}