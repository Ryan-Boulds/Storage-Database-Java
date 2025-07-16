package utils;

import java.util.Map;

public class SQLGenerator {
    public static String generateInsertSQL(String tableName, Map<String, String> data) {
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        for (String column : data.keySet()) {
            if (columns.length() > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(column);
            placeholders.append("?");
        }
        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
    }

    public static String formatDeviceSQL(Map<String, String> data) {
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        String[] fields = {"Device_Name", "Device_Type", "Brand", "Model", "Serial_Number", "Building_Location", "Room_Desk", "Specification", "Processor_Type", "Storage_Capacity", "Network_Address", "OS_Version", "Department", "Added_Memory", "Status", "Assigned_User", "Warranty_Expiry_Date", "Last_Maintenance", "Maintenance_Due", "Date_Of_Purchase", "Purchase_Cost", "Vendor", "Memory_RAM"};
        for (String field : fields) {
            if (columns.length() > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(field);
            placeholders.append("?");
        }
        return String.format("INSERT INTO Inventory (%s) VALUES (%s)", columns, placeholders);
    }

    public static String formatPeripheralSQL(Map<String, String> data) {
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        String[] fields = {"Peripheral_Name", "Peripheral_Type", "Brand", "Model", "Serial_Number", "Associated_PC", "Status", "Date_Of_Purchase", "Warranty_Expiry_Date", "Maintenance_Due"};
        for (String field : fields) {
            if (columns.length() > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(field);
            placeholders.append("?");
        }
        return String.format("INSERT INTO PeripheralInventory (%s) VALUES (%s)", columns, placeholders);
    }
}