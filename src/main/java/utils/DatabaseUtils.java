package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatabaseUtils {
    private static String DB_URL = "jdbc:ucanaccess://C:/Users/ami6985/OneDrive - AISIN WORLD CORP/Documents/InventoryManagement.accdb";

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
            for (String value : device.values()) {
                stmt.setString(index++, value != null ? value : "");
            }
            stmt.executeUpdate();
        }
    }

    public static void deleteDevice(String assetName) throws SQLException {
        String sql = "DELETE FROM Inventory WHERE AssetName = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, assetName);
            stmt.executeUpdate();
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
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                sql = "INSERT INTO " + table + " (" + typeColumn + ", Count) VALUES (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                    insertStmt.setString(1, peripheralType);
                    insertStmt.setInt(2, countDelta);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    public static void addNewField(String tableName, String fieldName, String fieldType) throws SQLException {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN [" + fieldName + "] " + fieldType;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
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
            stmt.executeUpdate();
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
        }
        return template;
    }

    // New method for duplicate checking
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
        }
        return null;
    }

    // New method for updating device
    public static void updateDevice(HashMap<String, String> device) throws SQLException {
        String sql = SQLGenerator.generateInsertSQL("Inventory", device);
        sql = sql.replace("INSERT INTO", "UPDATE").replace("VALUES", "SET") + " WHERE AssetName = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
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

    // Existing deleteDevice renamed to match expected signature
    public static void deleteDeviceByAssetName(String assetName) throws SQLException {
        deleteDevice(assetName);
    }
}