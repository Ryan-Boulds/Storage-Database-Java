package view_inventorytab;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import utils.InventoryData;

public class TableManager {
    private final JTable table;
    private final DefaultTableModel model;

    public TableManager(JTable table) {
        this.table = table;
        this.model = new DefaultTableModel();
        if (table != null) {
            table.setModel(model);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Disable auto-resize for fixed columns
            adjustColumnWidths(); // Set fixed column widths
        }
        initializeColumns();
    }

    private void initializeColumns() {
        if (model != null) {
            model.addColumn("Device Name");
            model.addColumn("Brand");
            model.addColumn("Model");
            model.addColumn("Serial Number");
            model.addColumn("Building Location");
            model.addColumn("Room/Desk");
            model.addColumn("Specification");
            model.addColumn("Processor Type");
            model.addColumn("Storage Capacity");
            model.addColumn("Network Address");
            model.addColumn("OS Version");
            model.addColumn("Department");
            model.addColumn("Added Memory");
            model.addColumn("Status");
            model.addColumn("Assigned User");
            model.addColumn("Warranty Expiry Date");
            model.addColumn("Last Maintenance");
            model.addColumn("Maintenance Due");
            model.addColumn("Date of Purchase");
            model.addColumn("Purchase Cost");
            model.addColumn("Vendor");
            model.addColumn("Memory (RAM)");
        }
    }

    private void adjustColumnWidths() {
        if (table != null && table.getColumnModel() != null) {
            DefaultTableColumnModel columnModel = (DefaultTableColumnModel) table.getColumnModel();
            String[] columnNames = {"Device Name", "Brand", "Model", "Serial Number", "Building Location", "Room/Desk",
                    "Specification", "Processor Type", "Storage Capacity", "Network Address", "OS Version", "Department",
                    "Added Memory", "Status", "Assigned User", "Warranty Expiry Date", "Last Maintenance",
                    "Maintenance Due", "Date of Purchase", "Purchase Cost", "Vendor", "Memory (RAM)"};
            int[] columnWidths = {150, 100, 100, 150, 120, 100, 120, 120, 100, 120, 100, 100, 100, 100, 120, 130, 130, 130, 130, 100, 100, 100};
            for (int i = 0; i < columnModel.getColumnCount() && i < columnNames.length; i++) {
                TableColumn column = columnModel.getColumn(i);
                column.setPreferredWidth(columnWidths[i]); // Set fixed width
                column.setMinWidth(columnWidths[i]); // Ensure minimum width
                column.setMaxWidth(columnWidths[i]); // Ensure maximum width matches preferred
            }
        }
    }

    public void refreshDataAndTabs() {
        if (table == null || model == null) {
            System.err.println("Table or model is null during refresh");
            return;
        }
        model.setRowCount(0); // Clear existing rows
        ArrayList<HashMap<String, String>> devices = InventoryData.getDevices();
        if (devices == null) {
            System.err.println("No devices retrieved from InventoryData");
            return;
        }
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        inputFormat.setLenient(false);
        for (Map<String, String> device : devices) {
            if (device == null) continue;
            Object[] row = new Object[22];
            row[0] = device.getOrDefault("Device_Name", "");
            row[1] = device.getOrDefault("Brand", "");
            row[2] = device.getOrDefault("Model", "");
            row[3] = device.getOrDefault("Serial_Number", "");
            row[4] = device.getOrDefault("Building_Location", "");
            row[5] = device.getOrDefault("Room_Desk", "");
            row[6] = device.getOrDefault("Specification", "");
            row[7] = device.getOrDefault("Processor_Type", "");
            row[8] = device.getOrDefault("Storage_Capacity", "");
            row[9] = device.getOrDefault("Network_Address", "");
            row[10] = device.getOrDefault("OS_Version", "");
            row[11] = device.getOrDefault("Department", "");
            row[12] = device.getOrDefault("Added_Memory", "");
            row[13] = device.getOrDefault("Status", "");
            row[14] = device.getOrDefault("Assigned_User", "");
            try {
                row[15] = device.get("Warranty_Expiry_Date") != null ? outputFormat.format(inputFormat.parse(device.get("Warranty_Expiry_Date"))) : "";
            } catch (Exception e) {
                row[15] = device.getOrDefault("Warranty_Expiry_Date", "");
            }
            try {
                row[16] = device.get("Last_Maintenance") != null ? outputFormat.format(inputFormat.parse(device.get("Last_Maintenance"))) : "";
            } catch (Exception e) {
                row[16] = device.getOrDefault("Last_Maintenance", "");
            }
            try {
                row[17] = device.get("Maintenance_Due") != null ? outputFormat.format(inputFormat.parse(device.get("Maintenance_Due"))) : "";
            } catch (Exception e) {
                row[17] = device.getOrDefault("Maintenance_Due", "");
            }
            try {
                row[18] = device.get("Date_Of_Purchase") != null ? outputFormat.format(inputFormat.parse(device.get("Date_Of_Purchase"))) : "";
            } catch (Exception e) {
                row[18] = device.getOrDefault("Date_Of_Purchase", "");
            }
            row[19] = device.getOrDefault("Purchase_Cost", "");
            row[20] = device.getOrDefault("Vendor", "");
            row[21] = device.getOrDefault("Memory_RAM", "");
            model.addRow(row);
        }
        adjustColumnWidths(); // Re-adjust after data load
    }
}