package utils;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataUtils {
    public static String capitalizeWords(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        return Arrays.stream(input.trim().split("\\s+"))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static String validateDevice(Map<String, String> data, String originalSerialNumber) {
        String deviceName = data.get("Device_Name");
        String serialNumber = data.get("Serial_Number");
        String purchaseCost = data.get("Purchase_Cost");
        String networkAddress = data.get("Network_Address");

        if (deviceName == null || deviceName.trim().isEmpty()) {
            return "Device Name is required";
        }
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            return "Serial Number is required";
        }
        for (HashMap<String, String> device : InventoryData.getDevices()) {
            String existingSerial = device.getOrDefault("Serial_Number", "");
            // Skip the current device if originalSerialNumber is provided and matches
            if (originalSerialNumber == null || !existingSerial.equals(originalSerialNumber)) {
                if (device.get("Device_Name").equals(deviceName)) {
                    return "Device Name '" + deviceName + "' already exists";
                }
                if (device.get("Serial_Number").equals(serialNumber)) {
                    return "Serial Number '" + serialNumber + "' already exists";
                }
            }
        }
        if (purchaseCost != null && !purchaseCost.trim().isEmpty()) {
            try {
                Double.valueOf(purchaseCost);
            } catch (NumberFormatException e) {
                return "Purchase Cost must be a valid number";
            }
        }
        if (networkAddress != null && !networkAddress.trim().isEmpty()) {
            if (!Pattern.matches("^((\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|([0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}))$", networkAddress)) {
                return "Network Address must be a valid IP or MAC address";
            }
        }
        return null;
    }

    public static String validateDevice(Map<String, String> data) {
        // Call the overloaded method with null originalSerialNumber for adding new devices
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