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
            // Invalidate cache to ensure fresh data on next getDevices call
            devices.clear();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving device to database: " + e.getMessage());
        }
    }

    public static ArrayList<HashMap<String, String>> getDevices() {
        try {
            System.out.println("Fetching devices from database");
            devices = FileUtils.loadDevices(); // Always reload from database
            System.out.println("Retrieved " + devices.size() + " devices from InventoryData");
            return new ArrayList<>(devices);
        } catch (SQLException e) {
            System.err.println("Error loading devices: " + e.getMessage());
            throw new RuntimeException("Error loading devices: " + e.getMessage());
        }
    }

    public static void deleteDevice(String assetName) {
        try {
            DatabaseUtils.deleteDevice(assetName);
            // Invalidate cache
            devices.clear();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting device: " + e.getMessage());
        }
    }

    public static ArrayList<HashMap<String, String>> getCables() {
        try {
            cables = FileUtils.loadCables();
            return new ArrayList<>(cables);
        } catch (SQLException e) {
            throw new RuntimeException("Error loading cables: " + e.getMessage());
        }
    }

    public static ArrayList<HashMap<String, String>> getAccessories() {
        try {
            accessories = FileUtils.loadAccessories();
            return new ArrayList<>(accessories);
        } catch (SQLException e) {
            throw new RuntimeException("Error loading accessories: " + e.getMessage());
        }
    }

    public static ArrayList<String> getTemplates() {
        try {
            templates = FileUtils.loadTemplates();
            return new ArrayList<>(templates);
        } catch (SQLException e) {
            throw new RuntimeException("Error loading templates: " + e.getMessage());
        }
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
}