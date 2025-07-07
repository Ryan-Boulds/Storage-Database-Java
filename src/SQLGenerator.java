import java.util.Map;

public class SQLGenerator {
    public static String formatDeviceSQL(Map<String, String> data) {
        StringBuilder sql = new StringBuilder("INSERT INTO Inventory (Device_Name, Device_Type, Brand, Model, Serial_Number, Building_Location, Room_Desk, Specification, Processor_Type, Storage_Capacity, Network_Address, OS_Version, Department, Added_Memory, Status, Assigned_User, Warranty_Expiry_Date, Last_Maintenance, Maintenance_Due, Date_Of_Purchase, Purchase_Cost, Vendor, Memory_RAM) VALUES (");
        String[] fields = {"Device_Name", "Device_Type", "Brand", "Model", "Serial_Number", "Building_Location", "Room_Desk", "Specification", "Processor_Type", "Storage_Capacity", "Network_Address", "OS_Version", "Department", "Added_Memory", "Status", "Assigned_User", "Warranty_Expiry_Date", "Last_Maintenance", "Maintenance_Due", "Date_Of_Purchase", "Purchase_Cost", "Vendor", "Memory_RAM"};
        for (int i = 0; i < fields.length; i++) {
            String value = data.get(fields[i]);
            if (value == null || value.trim().isEmpty()) {
                sql.append("NULL");
            } else if (fields[i].contains("Date")) {
                sql.append("TO_DATE('").append(value).append("', 'MM-DD-YYYY')");
            } else if (fields[i].equals("Added_Memory")) {
                sql.append(value.equals("TRUE") ? "1" : "0");
            } else if (fields[i].equals("Purchase_Cost")) {
                sql.append(value);
            } else {
                sql.append("'").append(value.replace("'", "''")).append("'");
            }
            if (i < fields.length - 1) {
                sql.append(", ");
            }
        }
        sql.append(");");
        return sql.toString();
    }

    public static String formatPeripheralSQL(Map<String, String> data) {
        StringBuilder sql = new StringBuilder("INSERT INTO PeripheralInventory (Peripheral_Name, Peripheral_Type, Brand, Model, Serial_Number, Associated_PC, Status, Date_Of_Purchase, Warranty_Expiry_Date, Maintenance_Due) VALUES (");
        String[] fields = {"Peripheral_Name", "Peripheral_Type", "Brand", "Model", "Serial_Number", "Associated_PC", "Status", "Date_Of_Purchase", "Warranty_Expiry_Date", "Maintenance_Due"};
        for (int i = 0; i < fields.length; i++) {
            String value = data.get(fields[i]);
            if (value == null || value.trim().isEmpty()) {
                sql.append("NULL");
            } else if (fields[i].contains("Date")) {
                sql.append("TO_DATE('").append(value).append("', 'MM-DD-YYYY')");
            } else {
                sql.append("'").append(value.replace("'", "''")).append("'");
            }
            if (i < fields.length - 1) {
                sql.append(", ");
            }
        }
        sql.append(");");
        return sql.toString();
    }
}