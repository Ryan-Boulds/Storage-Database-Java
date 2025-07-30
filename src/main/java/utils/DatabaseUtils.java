package utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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

public class DatabaseUtils {
    private static String DB_URL = "jdbc:ucanaccess://C:/Users/ami6985/OneDrive - AISIN WORLD CORP/Documents/InventoryManagement.accdb";
    private static final Logger LOGGER = Logger.getLogger(DatabaseUtils.class.getName());

    public static void setDatabasePath(String path) {
        DB_URL = "jdbc:ucanaccess://" + path.replace("\\", "/");
        LOGGER.log(Level.INFO, "Database path set to: {0}", DB_URL);
    }

    public static Connection getConnection() throws SQLException {
        LOGGER.log(Level.INFO, "Connecting to database: {0}", DB_URL);
        return DriverManager.getConnection(DB_URL);
    }

    public static List<String> getTableNames() throws SQLException {
        List<String> tableNames = new ArrayList<>();
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (!tableName.startsWith("MSys")) {
                        tableNames.add(tableName);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving table names: {0}", e.getMessage());
            throw e;
        }
        LOGGER.log(Level.INFO, "Retrieved table names: {0}", tableNames);
        return tableNames;
    }

    public static void createTable(String tableName) throws SQLException {
        String sql = "CREATE TABLE [" + tableName + "] ([AssetName] VARCHAR(255) PRIMARY KEY)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            LOGGER.log(Level.INFO, "Executing CREATE TABLE SQL: {0}", sql);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating table {0}: {1}", new Object[]{tableName, e.getMessage()});
            throw e;
        }
    }

    public static void addColumnsToTable(String tableName, String[] columnNames) throws SQLException {
        try (Connection conn = getConnection()) {
            List<String> existingColumns = getInventoryColumnNames(tableName);
            for (String columnName : columnNames) {
                if (!existingColumns.contains(columnName) && !columnName.equals("AssetName")) {
                    String sql = "ALTER TABLE [" + tableName + "] ADD COLUMN [" + columnName + "] VARCHAR(255)";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        LOGGER.log(Level.INFO, "Executing ALTER TABLE SQL: {0}", sql);
                        stmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding columns to table {0}: {1}", new Object[]{tableName, e.getMessage()});
            throw e;
        }
    }

    public static ArrayList<String> getInventoryColumnNames(String tableName) throws SQLException {
        ArrayList<String> columns = new ArrayList<>();
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    columns.add(columnName);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving columns for table {0}: {1}", new Object[]{tableName, e.getMessage()});
            throw e;
        }
        LOGGER.log(Level.INFO, "Retrieved columns for table {0}: {1}", new Object[]{tableName, columns});
        return columns;
    }

    public static Map<String, String> getInventoryColumnTypes(String tableName) throws SQLException {
        Map<String, String> columnTypes = new HashMap<>();
        try (Connection conn = getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM [" + tableName + "] WHERE 1=0")) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                int sqlType = metaData.getColumnType(i);
                String typeName;
                switch (sqlType) {
                    case Types.VARCHAR:
                        typeName = "VARCHAR(255)";
                        break;
                    case Types.INTEGER:
                        typeName = "INTEGER";
                        break;
                    case Types.DOUBLE:
                        typeName = "DOUBLE";
                        break;
                    case Types.DATE:
                    case Types.TIMESTAMP:
                        typeName = "DATE";
                        break;
                    case Types.BOOLEAN:
                        typeName = "BOOLEAN";
                        break;
                    default:
                        typeName = "VARCHAR(255)";
                        break;
                }
                columnTypes.put(columnName, typeName);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving column types for table {0}: {1}", new Object[]{tableName, e.getMessage()});
            throw e;
        }
        LOGGER.log(Level.INFO, "Retrieved column types for table {0}: {1}", new Object[]{tableName, columnTypes});
        return columnTypes;
    }

    public static void saveDevice(String tableName, HashMap<String, String> device) throws SQLException {
        String sql = SQLGenerator.generateInsertSQL(tableName, device);
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            Map<String, String> columnTypes = getInventoryColumnTypes(tableName);
            int index = 1;
            for (Map.Entry<String, String> entry : device.entrySet()) {
                String column = entry.getKey();
                String value = entry.getValue();
                String sqlType = columnTypes.getOrDefault(column, "VARCHAR(255)");
                if (sqlType.equals("DATE") || 
                    column.equals("Warranty_Expiry_Date") || column.equals("Last_Maintenance") || 
                    column.equals("Maintenance_Due") || column.equals("Date_Of_Purchase")) {
                    if (value == null || value.trim().isEmpty()) {
                        stmt.setNull(index++, Types.DATE);
                    } else {
                        try {
                            java.util.Date date = parseDate(value);
                            stmt.setDate(index++, new java.sql.Date(date.getTime()));
                        } catch (java.text.ParseException e) {
                            LOGGER.log(Level.SEVERE, "Failed to parse date for column {0}: {1}", new Object[]{column, value});
                            stmt.setNull(index++, Types.DATE);
                        }
                    }
                } else {
                    stmt.setString(index++, value != null ? value : "");
                }
            }
            LOGGER.log(Level.INFO, "Executing INSERT SQL: {0}", sql);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting device into table {0}: {1}", new Object[]{tableName, e.getMessage()});
            throw e;
        }
    }

    public static void updateDevice(String tableName, HashMap<String, String> device) throws SQLException {
        if (!device.containsKey("AssetName") || device.get("AssetName") == null || device.get("AssetName").trim().isEmpty()) {
            throw new SQLException("AssetName is required for update");
        }

        StringBuilder sql = new StringBuilder("UPDATE [" + tableName + "] SET ");
        List<String> columns = new ArrayList<>();
        for (String column : device.keySet()) {
            if (!column.equals("AssetName")) {
                columns.add("[" + column + "] = ?");
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
            Map<String, String> columnTypes = getInventoryColumnTypes(tableName);
            int index = 1;
            String assetName = device.get("AssetName");

            for (Map.Entry<String, String> entry : device.entrySet()) {
                String column = entry.getKey();
                if (!column.equals("AssetName")) {
                    String value = entry.getValue();
                    String sqlType = columnTypes.getOrDefault(column, "VARCHAR(255)");
                    if (sqlType.equals("DATE") || 
                        column.equals("Warranty_Expiry_Date") || column.equals("Last_Maintenance") || 
                        column.equals("Maintenance_Due") || column.equals("Date_Of_Purchase")) {
                        if (value == null || value.trim().isEmpty()) {
                            stmt.setNull(index++, Types.DATE);
                        } else {
                            try {
                                java.util.Date date = parseDate(value);
                                stmt.setDate(index++, new java.sql.Date(date.getTime()));
                            } catch (java.text.ParseException e) {
                                LOGGER.log(Level.SEVERE, "Failed to parse date for column {0}: {1}", new Object[]{column, value});
                                stmt.setNull(index++, Types.DATE);
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
                LOGGER.log(Level.WARNING, "No rows updated for AssetName: {0} in table {1}", new Object[]{assetName, tableName});
                throw new SQLException("No device found with AssetName: " + assetName);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating device with AssetName {0} in table {1}: {2}\nSQL: {3}", 
                       new Object[]{device.get("AssetName"), tableName, e.getMessage(), debugSql});
            throw e;
        }
    }

    public static void deleteDevice(String tableName, String assetName) throws SQLException {
        String sql = "DELETE FROM [" + tableName + "] WHERE AssetName = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, assetName);
            LOGGER.log(Level.INFO, "Executing DELETE SQL: {0}", sql);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting device with AssetName {0} from table {1}: {2}", 
                       new Object[]{assetName, tableName, e.getMessage()});
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
        String sql = "UPDATE [" + table + "] SET Count = Count + ? WHERE " + typeColumn + " = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, countDelta);
            stmt.setString(2, peripheralType);
            LOGGER.log(Level.INFO, "Executing UPDATE peripheral SQL: {0}", sql);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                sql = "INSERT INTO [" + table + "] (" + typeColumn + ", Count) VALUES (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                    insertStmt.setString(1, peripheralType);
                    insertStmt.setInt(2, countDelta);
                    LOGGER.log(Level.INFO, "Executing INSERT peripheral SQL: {0}", sql);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating peripheral {0}: {1}", new Object[]{peripheralType, e.getMessage()});
            throw e;
        }
    }

    public static void addNewField(String tableName, String fieldName, String fieldType) throws SQLException {
        String sql = "ALTER TABLE [" + tableName + "] ADD COLUMN [" + fieldName + "] " + fieldType;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            LOGGER.log(Level.INFO, "Executing ALTER TABLE SQL: {0}", sql);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding new field {0} to table {1}: {2}", new Object[]{fieldName, tableName, e.getMessage()});
            throw e;
        }
    }

    public static List<String> getDeviceTypes(String tableName) throws SQLException {
        Set<String> deviceTypes = new HashSet<>();
        String sql = "SELECT [Device_Type] FROM [" + tableName + "] WHERE [Device_Type] IS NOT NULL";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String type = rs.getString("Device_Type");
                if (type != null && !type.trim().isEmpty()) {
                    deviceTypes.add(type);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving device types from table {0}: {1}", new Object[]{tableName, e.getMessage()});
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
            LOGGER.log(Level.INFO, "Executing INSERT template SQL: {0}", sql);
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
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
            }
            while (rs.next()) {
                HashMap<String, String> template = new HashMap<>();
                for (String column : columns) {
                    String value = rs.getString(column);
                    template.put(column, value != null ? value.trim() : "");
                }
                templates.add(template);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading templates: {0}", e.getMessage());
            throw e;
        }
        return templates;
    }

    public static ArrayList<HashMap<String, String>> loadDevices(String tableName) throws SQLException {
        ArrayList<HashMap<String, String>> devices = new ArrayList<>();
        String sql = "SELECT * FROM [" + tableName + "]";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
            }
            LOGGER.log(Level.INFO, "loadDevices: Columns fetched for table {0}: {1}", new Object[]{tableName, columns});
            while (rs.next()) {
                HashMap<String, String> device = new HashMap<>();
                for (String column : columns) {
                    String value = rs.getString(column);
                    device.put(column, value != null ? value.trim() : "");
                }
                devices.add(device);
            }
            LOGGER.log(Level.INFO, "loadDevices: Retrieved {0} devices from table {1}", new Object[]{devices.size(), tableName});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading devices from table {0}: {1}", new Object[]{tableName, e.getMessage()});
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
        String sql = "SELECT " + typeColumn + ", Count FROM [" + table + "]";
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
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        String column = metaData.getColumnName(i);
                        String value = rs.getString(column);
                        template.put(column, value != null ? value.trim() : "");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading template details for {0}: {1}", new Object[]{templateName, e.getMessage()});
            throw e;
        }
        return template;
    }

    public static HashMap<String, String> getDeviceByAssetName(String tableName, String assetName) throws SQLException {
        String sql = "SELECT * FROM [" + tableName + "] WHERE AssetName = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, assetName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    HashMap<String, String> device = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        String column = metaData.getColumnName(i);
                        String value = rs.getString(column);
                        device.put(column, value != null ? value.trim() : "");
                    }
                    return device;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving device with AssetName {0} from table {1}: {2}", 
                       new Object[]{assetName, tableName, e.getMessage()});
            throw e;
        }
        return null;
    }

    private static java.util.Date parseDate(String value) throws java.text.ParseException {
        String[] formats = {"MM/dd/yyyy", "yyyy-MM-dd", "MM-dd-yyyy"};
        for (String format : formats) {
            try {
                return new SimpleDateFormat(format).parse(value);
            } catch (java.text.ParseException e) {
                // Try next format
            }
        }
        throw new java.text.ParseException("Unparseable date: " + value, 0);
    }
}