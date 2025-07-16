package utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class InventoryData {
    private static ArrayList<HashMap<String, String>> devices = new ArrayList<>();
    private static ArrayList<HashMap<String, String>> cables = new ArrayList<>();
    private static ArrayList<HashMap<String, String>> accessories = new ArrayList<>();
    private static ArrayList<String> templates = new ArrayList<>();

    public static void saveDevice(HashMap<String, String> device) {
        try {
            DatabaseUtils.saveDevice(device);
            devices.add(device);
        } catch (SQLException e) {
            throw new RuntimeException("Error saving device to database: " + e.getMessage());
        }
    }

    public static ArrayList<HashMap<String, String>> getDevices() {
        try {
            devices = FileUtils.loadDevices(); // Ensure this returns consistent date format
            return new ArrayList<>(devices); // Return a copy to avoid modification issues
        } catch (SQLException e) {
            throw new RuntimeException("Error loading devices: " + e.getMessage());
        }
    }

    public static ArrayList<HashMap<String, String>> getCables() {
        try {
            cables = FileUtils.loadCables(); // Assume this now returns count by type
            return new ArrayList<>(cables); // Return a copy
        } catch (SQLException e) {
            throw new RuntimeException("Error loading cables: " + e.getMessage());
        }
    }

    public static ArrayList<HashMap<String, String>> getAccessories() {
        try {
            accessories = FileUtils.loadAccessories();
        } catch (SQLException e) {
            throw new RuntimeException("Error loading accessories: " + e.getMessage());
        }
        return accessories;
    }

    public static ArrayList<String> getTemplates() {
        try {
            templates = FileUtils.loadTemplates();
        } catch (SQLException e) {
            throw new RuntimeException("Error loading templates: " + e.getMessage());
        }
        return templates;
    }

    public static void saveTemplate(HashMap<String, String> template, String templateName) {
        try {
            DatabaseUtils.saveTemplate(template, templateName);
            if (!templates.contains(templateName)) {
                templates.add(templateName);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving template: " + e.getMessage());
        }
    }

    public static HashMap<String, String> getTemplateDetails(String templateName) {
        try {
            return FileUtils.loadTemplateDetails(templateName);
        } catch (SQLException e) {
            throw new RuntimeException("Error loading template details: " + e.getMessage());
        }
    }

    public static void deleteDevice(String serialNumber) {
        try {
            DatabaseUtils.deleteDevice(serialNumber);
            devices.removeIf(device -> serialNumber.equals(device.get("Serial_Number")));
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting device: " + e.getMessage());
        }
    }
}