package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataUtils {
    private static final Logger LOGGER = Logger.getLogger(DataUtils.class.getName());

    public static String validateDevice(Map<String, String> data, String originalAssetName, String tableName) {
        if (data == null || tableName == null || tableName.trim().isEmpty()) {
            return "Invalid data or table name";
        }
        if (data.containsKey("AssetName")) {
            String newAssetName = data.get("AssetName");
            if (newAssetName != null && !newAssetName.trim().isEmpty() && !newAssetName.equals(originalAssetName)) {
                try (Connection conn = DatabaseUtils.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("SELECT AssetName FROM [" + tableName + "] WHERE AssetName = ?")) {
                    stmt.setString(1, newAssetName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return "AssetName '" + newAssetName + "' already exists in table '" + tableName + "'";
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error validating AssetName: {0}", e.getMessage());
                    return "Database error: " + e.getMessage();
                }
            }
        }
        return null; // No validation errors
    }

    public static String validateDevice(Map<String, String> data, String originalAssetName) {
        String tableName = data.getOrDefault("TableName", null);
        if (tableName == null) {
            LOGGER.warning("TableName not provided in device data for validation");
            return "Table name is required for validation";
        }
        return validateDevice(data, originalAssetName, tableName);
    }

    public static String validateData(HashMap<String, String> data, Map<String, Integer> columnTypes) {
        if (data == null || columnTypes == null) {
            return "Invalid data or column types";
        }
        if (!data.containsKey("AssetName") || data.get("AssetName") == null || data.get("AssetName").trim().isEmpty()) {
            return "AssetName is required and cannot be empty";
        }
        for (Map.Entry<String, Integer> entry : columnTypes.entrySet()) {
            String column = entry.getKey();
            Integer sqlType = entry.getValue();
            String value = data.get(column);
            if (value != null && !value.trim().isEmpty()) {
                switch (sqlType) {
                    case Types.INTEGER:
                        try {
                            Integer.valueOf(value);
                        } catch (NumberFormatException e) {
                            return "Invalid INTEGER value for column " + column + ": " + value;
                        }
                        break;
                    case Types.DOUBLE:
                    case Types.FLOAT:
                    case Types.DECIMAL:
                    case Types.NUMERIC:
                        try {
                            Double.valueOf(value);
                        } catch (NumberFormatException e) {
                            return "Invalid numeric value for column " + column + ": " + value;
                        }
                        break;
                    case Types.DATE:
                    case Types.TIMESTAMP:
                        try {
                            String[] formats = {"MM/dd/yyyy", "yyyy-MM-dd", "MM-dd-yyyy"};
                            boolean valid = false;
                            for (String format : formats) {
                                try {
                                    new java.text.SimpleDateFormat(format).parse(value);
                                    valid = true;
                                    break;
                                } catch (java.text.ParseException ignored) {
                                }
                            }
                            if (!valid) {
                                return "Invalid date format for column " + column + ": " + value;
                            }
                        } catch (Exception e) {
                            return "Invalid date value for column " + column + ": " + value;
                        }
                        break;
                    case Types.BOOLEAN:
                        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                            return "Invalid BOOLEAN value for column " + column + ": " + value;
                        }
                        break;
                }
            }
        }
        return null; // No validation errors
    }

    public static String capitalizeWords(String input) {
        if (input == null || input.trim().isEmpty()) {
            LOGGER.warning("Attempted to capitalize null or empty string");
            return input;
        }
        String[] words = input.trim().toLowerCase().split("(?=\\s+|-|_)+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) continue;
            result.append(word.substring(0, 1).toUpperCase())
                  .append(word.substring(1));
            if (i < words.length - 1) {
                result.append(word.matches("[-_]+") ? word : " ");
            }
        }
        String capitalized = result.toString();
        LOGGER.log(Level.INFO, "Capitalized ''{0}'' to ''{1}''", new Object[]{input, capitalized});
        return capitalized;
    }
}