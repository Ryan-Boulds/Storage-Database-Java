package view_inventorytab;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import utils.FileUtils;
import utils.InventoryData;
import utils.UIComponentUtils;

public class TableManager {
    private final Map<String, DefaultTableModel> tableModels = new HashMap<>();
    private final Map<String, JTable> tables = new HashMap<>();
    private final JTabbedPane tabbedPane = new JTabbedPane();

    public TableManager() {
        // No method reference needed; PopupHandler is stateless
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public void refreshDataAndTabs() {
        try {
            FileUtils.loadDevices();
            System.out.println("[DEBUG] After refreshData, InventoryData.getDevices(): " + InventoryData.getDevices());
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to load devices: " + e.getMessage());
            return;
        }

        // Clear existing tabs
        tabbedPane.removeAll();
        tableModels.clear();
        tables.clear();

        // Determine unique device types
        Set<String> uniqueTypes = new HashSet<>();
        for (HashMap<String, String> device : InventoryData.getDevices()) {
            String type = device.getOrDefault("Device_Type", "Other");
            uniqueTypes.add(type);
        }

        // Determine dynamic columns with data
        Set<String> allKeys = new HashSet<>();
        for (HashMap<String, String> device : InventoryData.getDevices()) {
            for (String key : device.keySet()) {
                String normalizedKey = key.replace("/", "_");
                allKeys.add(normalizedKey);
            }
        }
        Set<String> activeColumns = new HashSet<>();
        for (String key : allKeys) {
            for (HashMap<String, String> device : InventoryData.getDevices()) {
                String value = device.getOrDefault(key, "").trim();
                if (!value.isEmpty()) {
                    activeColumns.add(key);
                    break;
                }
                String originalKey = key.contains("_") ? key.replace("_", "/") : key.replace("/", "_");
                value = device.getOrDefault(originalKey, "").trim();
                if (!value.isEmpty()) {
                    activeColumns.add(originalKey);
                    break;
                }
            }
        }
        String[] columns = activeColumns.stream().map(key -> key.replace("_", " ")).toArray(String[]::new);

        // Create a tab for each unique device type
        for (String type : uniqueTypes) {
            DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            JTable table = new JTable(tableModel);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (int i = 0; i < columns.length; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(100);
            }
            JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(table);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            scrollPane.getVerticalScrollBar().setUnitIncrement(20);
            scrollPane.getVerticalScrollBar().setBlockIncrement(60);
            scrollPane.setDoubleBuffered(true);

            tableModels.put(type, tableModel);
            tables.put(type, table);
            tabbedPane.addTab(type, scrollPane);
            PopupHandler.addTablePopup(table, tabbedPane);
        }

        updateTables("", "All", "All");
    }

    public void updateTables(String searchText, String statusFilter, String deptFilter) {
        for (String type : tableModels.keySet()) {
            DefaultTableModel tableModel = tableModels.get(type);
            tableModel.setRowCount(0);
            ArrayList<HashMap<String, String>> filteredDevices = new ArrayList<>();
            for (HashMap<String, String> device : InventoryData.getDevices()) {
                String deviceName = device.getOrDefault("Device_Name", "");
                String deviceType = device.getOrDefault("Device_Type", "Other");
                String serial = device.getOrDefault("Serial_Number", "");
                String status = device.getOrDefault("Status", "");
                String dept = device.getOrDefault("Department", "");

                if ((searchText == null || searchText.isEmpty() || deviceName.toLowerCase().contains(searchText.toLowerCase()) || serial.toLowerCase().contains(searchText.toLowerCase())) &&
                    (statusFilter.equals("All") || status.equals(statusFilter)) &&
                    (deptFilter.equals("All") || dept.equals(deptFilter)) &&
                    (type.equals("Other") || deviceType.equals(type))) {
                    filteredDevices.add(device);
                }
            }
            for (HashMap<String, String> device : filteredDevices) {
                System.out.println("[DEBUG] Adding device to table (" + type + "): " + device);
                Object[] rowData = new Object[tableModel.getColumnCount()];
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    String columnName = tableModel.getColumnName(i).replace(" ", "_");
                    rowData[i] = device.getOrDefault(columnName, "");
                }
                tableModel.addRow(rowData);
            }
        }
    }
}