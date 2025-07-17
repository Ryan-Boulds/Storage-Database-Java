package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class DatabaseUtils {
    private static String DB_URL = "jdbc:ucanaccess://C:/Users/ami6985/OneDrive - AISIN WORLD CORP/Documents/InventoryManagement.accdb";

    public static void setDatabasePath(String path) {
        DB_URL = "jdbc:ucanaccess://" + path.replace("\\", "/");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void saveDevice(HashMap<String, String> device) throws SQLException {
        String sql = "INSERT INTO Inventory (Device_Name, Device_Type, Brand, Model, Serial_Number, " +
                     "Building_Location, Room_Desk, Specification, Processor_Type, Storage_Capacity, " +
                     "Network_Address, OS_Version, Department, Added_Memory, Status, Assigned_User, " +
                     "Warranty_Expiry_Date, Last_Maintenance, Maintenance_Due, Date_Of_Purchase, " +
                     "Purchase_Cost, Vendor, Memory_RAM) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, device.getOrDefault("Device_Name", ""));
            stmt.setString(2, device.getOrDefault("Device_Type", ""));
            stmt.setString(3, device.getOrDefault("Brand", ""));
            stmt.setString(4, device.getOrDefault("Model", ""));
            stmt.setString(5, device.getOrDefault("Serial_Number", ""));
            stmt.setString(6, device.getOrDefault("Building_Location", ""));
            stmt.setString(7, device.getOrDefault("Room_Desk", ""));
            stmt.setString(8, device.getOrDefault("Specification", ""));
            stmt.setString(9, device.getOrDefault("Processor_Type", ""));
            stmt.setString(10, device.getOrDefault("Storage_Capacity", ""));
            stmt.setString(11, device.getOrDefault("Network_Address", ""));
            stmt.setString(12, device.getOrDefault("OS_Version", ""));
            stmt.setString(13, device.getOrDefault("Department", ""));
            stmt.setString(14, device.getOrDefault("Added_Memory", null));
            stmt.setString(15, device.getOrDefault("Status", ""));
            stmt.setString(16, device.getOrDefault("Assigned_User", ""));
            stmt.setString(17, device.getOrDefault("Warranty_Expiry_Date", ""));
            stmt.setString(18, device.getOrDefault("Last_Maintenance", ""));
            stmt.setString(19, device.getOrDefault("Maintenance_Due", ""));
            stmt.setString(20, device.getOrDefault("Date_Of_Purchase", ""));
            stmt.setString(21, device.getOrDefault("Purchase_Cost", ""));
            stmt.setString(22, device.getOrDefault("Vendor", ""));
            stmt.setString(23, device.getOrDefault("Memory_RAM", ""));
            stmt.executeUpdate();
        }
    }

    public static void deleteDevice(String serialNumber) throws SQLException {
        String sql = "DELETE FROM Inventory WHERE Serial_Number = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, serialNumber);
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
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + fieldName + " " + fieldType;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public static void saveTemplate(HashMap<String, String> template, String templateName) throws SQLException {
        String sql = "INSERT INTO Templates (Template_Name, Device_Name, Device_Type, Brand, Model, Serial_Number, " +
                     "Building_Location, Room_Desk, Specification, Processor_Type, Storage_Capacity, " +
                     "Network_Address, OS_Version, Department, Added_Memory, Status, Assigned_User, " +
                     "Warranty_Expiry_Date, Last_Maintenance, Maintenance_Due, Date_Of_Purchase, " +
                     "Purchase_Cost, Vendor, Memory_RAM) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, templateName);
            stmt.setString(2, template.getOrDefault("Device_Name", ""));
            stmt.setString(3, template.getOrDefault("Device_Type", ""));
            stmt.setString(4, template.getOrDefault("Brand", ""));
            stmt.setString(5, template.getOrDefault("Model", ""));
            stmt.setString(6, template.getOrDefault("Serial_Number", ""));
            stmt.setString(7, template.getOrDefault("Building_Location", ""));
            stmt.setString(8, template.getOrDefault("Room_Desk", ""));
            stmt.setString(9, template.getOrDefault("Specification", ""));
            stmt.setString(10, template.getOrDefault("Processor_Type", ""));
            stmt.setString(11, template.getOrDefault("Storage_Capacity", ""));
            stmt.setString(12, template.getOrDefault("Network_Address", ""));
            stmt.setString(13, template.getOrDefault("OS_Version", ""));
            stmt.setString(14, template.getOrDefault("Department", ""));
            stmt.setString(15, template.getOrDefault("Added_Memory", null));
            stmt.setString(16, template.getOrDefault("Status", ""));
            stmt.setString(17, template.getOrDefault("Assigned_User", ""));
            stmt.setString(18, template.getOrDefault("Warranty_Expiry_Date", ""));
            stmt.setString(19, template.getOrDefault("Last_Maintenance", ""));
            stmt.setString(20, template.getOrDefault("Maintenance_Due", ""));
            stmt.setString(21, template.getOrDefault("Date_Of_Purchase", ""));
            stmt.setString(22, template.getOrDefault("Purchase_Cost", ""));
            stmt.setString(23, template.getOrDefault("Vendor", ""));
            stmt.setString(24, template.getOrDefault("Memory_RAM", ""));
            stmt.executeUpdate();
        }
    }

    public static ArrayList<HashMap<String, String>> loadTemplates() throws SQLException {
        ArrayList<HashMap<String, String>> templates = new ArrayList<>();
        String sql = "SELECT * FROM Templates";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                HashMap<String, String> template = new HashMap<>();
                template.put("Template_Name", rs.getString("Template_Name"));
                template.put("Device_Name", rs.getString("Device_Name"));
                template.put("Device_Type", rs.getString("Device_Type"));
                template.put("Brand", rs.getString("Brand"));
                template.put("Model", rs.getString("Model"));
                template.put("Serial_Number", rs.getString("Serial_Number"));
                template.put("Building_Location", rs.getString("Building_Location"));
                template.put("Room_Desk", rs.getString("Room_Desk"));
                template.put("Specification", rs.getString("Specification"));
                template.put("Processor_Type", rs.getString("Processor_Type"));
                template.put("Storage_Capacity", rs.getString("Storage_Capacity"));
                template.put("Network_Address", rs.getString("Network_Address"));
                template.put("OS_Version", rs.getString("OS_Version"));
                template.put("Department", rs.getString("Department"));
                template.put("Added_Memory", rs.getString("Added_Memory"));
                template.put("Status", rs.getString("Status"));
                template.put("Assigned_User", rs.getString("Assigned_User"));
                template.put("Warranty_Expiry_Date", rs.getString("Warranty_Expiry_Date"));
                template.put("Last_Maintenance", rs.getString("Last_Maintenance"));
                template.put("Maintenance_Due", rs.getString("Maintenance_Due"));
                template.put("Date_Of_Purchase", rs.getString("Date_Of_Purchase")); // Fixed typo
                template.put("Purchase_Cost", rs.getString("Purchase_Cost"));
                template.put("Vendor", rs.getString("Vendor"));
                template.put("Memory_RAM", rs.getString("Memory_RAM"));
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
                device.put("Device_Name", rs.getString("Device_Name"));
                device.put("Device_Type", rs.getString("Device_Type"));
                device.put("Brand", rs.getString("Brand"));
                device.put("Model", rs.getString("Model"));
                device.put("Serial_Number", rs.getString("Serial_Number"));
                device.put("Building_Location", rs.getString("Building_Location"));
                device.put("Room_Desk", rs.getString("Room_Desk"));
                device.put("Specification", rs.getString("Specification"));
                device.put("Processor_Type", rs.getString("Processor_Type"));
                device.put("Storage_Capacity", rs.getString("Storage_Capacity"));
                device.put("Network_Address", rs.getString("Network_Address"));
                device.put("OS_Version", rs.getString("OS_Version"));
                device.put("Department", rs.getString("Department"));
                device.put("Added_Memory", rs.getString("Added_Memory"));
                device.put("Status", rs.getString("Status"));
                device.put("Assigned_User", rs.getString("Assigned_User"));
                device.put("Warranty_Expiry_Date", rs.getString("Warranty_Expiry_Date"));
                device.put("Last_Maintenance", rs.getString("Last_Maintenance"));
                device.put("Maintenance_Due", rs.getString("Maintenance_Due"));
                device.put("Date_Of_Purchase", rs.getString("Date_Of_Purchase"));
                device.put("Purchase_Cost", rs.getString("Purchase_Cost"));
                device.put("Vendor", rs.getString("Vendor"));
                device.put("Memory_RAM", rs.getString("Memory_RAM"));
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
                    template.put("Template_Name", rs.getString("Template_Name"));
                    template.put("Device_Name", rs.getString("Device_Name"));
                    template.put("Device_Type", rs.getString("Device_Type"));
                    template.put("Brand", rs.getString("Brand"));
                    template.put("Model", rs.getString("Model"));
                    template.put("Serial_Number", rs.getString("Serial_Number"));
                    template.put("Building_Location", rs.getString("Building_Location"));
                    template.put("Room_Desk", rs.getString("Room_Desk"));
                    template.put("Specification", rs.getString("Specification"));
                    template.put("Processor_Type", rs.getString("Processor_Type"));
                    template.put("Storage_Capacity", rs.getString("Storage_Capacity"));
                    template.put("Network_Address", rs.getString("Network_Address"));
                    template.put("OS_Version", rs.getString("OS_Version"));
                    template.put("Department", rs.getString("Department"));
                    template.put("Added_Memory", rs.getString("Added_Memory"));
                    template.put("Status", rs.getString("Status"));
                    template.put("Assigned_User", rs.getString("Assigned_User"));
                    template.put("Warranty_Expiry_Date", rs.getString("Warranty_Expiry_Date"));
                    template.put("Last_Maintenance", rs.getString("Last_Maintenance"));
                    template.put("Maintenance_Due", rs.getString("Maintenance_Due"));
                    template.put("Date_Of_Purchase", rs.getString("Date_Of_Purchase"));
                    template.put("Purchase_Cost", rs.getString("Purchase_Cost"));
                    template.put("Vendor", rs.getString("Vendor"));
                    template.put("Memory_RAM", rs.getString("Memory_RAM"));
                }
            }
        }
        return template;
    }
}