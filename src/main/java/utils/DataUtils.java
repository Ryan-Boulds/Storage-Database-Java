package utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataUtils {
    public static String capitalizeWords(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        return Arrays.stream(input.trim().split("\\s+"))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static String validateDevice(Map<String, String> data, String originalAssetName) {
        String assetName = data.get("AssetName");
        String ipAddress = data.get("IP_Address");

        if (assetName == null || assetName.trim().isEmpty()) {
            return "AssetName is required";
        }
        for (HashMap<String, String> device : InventoryData.getDevices()) {
            String existingAssetName = device.getOrDefault("AssetName", "");
            if (originalAssetName == null || !existingAssetName.equals(originalAssetName)) {
                if (existingAssetName.equals(assetName)) {
                    return "AssetName '" + assetName + "' already exists";
                }
            }
        }
        if (ipAddress != null && !ipAddress.trim().isEmpty()) {
            if (!Pattern.matches("^((\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|([0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}))$", ipAddress)) {
                return "IP Address must be a valid IP or MAC address";
            }
        }
        return null;
    }

    public static String validateDevice(Map<String, String> data) {
        return validateDevice(data, null);
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