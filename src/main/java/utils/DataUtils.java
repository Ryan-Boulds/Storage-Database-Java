package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DataUtils {
    private static final Logger LOGGER = Logger.getLogger(DataUtils.class.getName());

    public static String capitalizeWords(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        return Arrays.stream(input.trim().split("\\s+"))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static String validateDevice(Map<String, String> data, String originalAssetName, String tableName) {
        String assetName = data.get("AssetName");
        String ipAddress = data.get("IP_Address");

        if (assetName == null || assetName.trim().isEmpty()) {
            return "AssetName is required";
        }
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT AssetName FROM [" + tableName + "] WHERE AssetName = ?")) {
            ps.setString(1, assetName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String existingAssetName = rs.getString("AssetName");
                    if (originalAssetName == null || !existingAssetName.equals(originalAssetName)) {
                        return "AssetName '" + assetName + "' already exists in table " + tableName;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error validating device in table {0}: {1}", new Object[]{tableName, e.getMessage()});
            return "Error validating device: " + e.getMessage();
        }
        if (ipAddress != null && !ipAddress.trim().isEmpty()) {
            if (!Pattern.matches("^((\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|([0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}))$", ipAddress)) {
                return "IP Address must be a valid IP or MAC address";
            }
        }
        return null;
    }

    public static String validateDevice(Map<String, String> data, String tableName) {
        return validateDevice(data, null, tableName);
    }

    public static String validatePeripheral(Map<String, String> data) {
        String peripheralName = data.get("Peripheral_Type");
        String count = data.get("Count");
        if (peripheralName == null || peripheralName.trim().isEmpty()) {
            return "Peripheral Type is required";
        }
        if (count != null && !count.trim().isEmpty()) {
            try {
                int c = Integer.parseInt(count);
                if (c < 0) {
                    return "Count cannot be negative";
                }
            } catch (NumberFormatException e) {
                return "Count must be a valid number";
            }
        }
        return null;
    }

    public static String normalizeColumnName(String csvColumn) {
        if (csvColumn == null) return "";
        String normalized = csvColumn.trim().toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");
        return normalized.isEmpty() ? "unnamed_column" : normalized;
    }
}