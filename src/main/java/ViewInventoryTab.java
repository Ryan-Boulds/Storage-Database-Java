import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import utils.DataUtils;
import utils.FileUtils;
import utils.InventoryData;
import utils.UIComponentUtils;

public final class ViewInventoryTab extends JPanel {
    private final Map<String, DefaultTableModel> tableModels = new HashMap<>();
    private final Map<String, JTable> tables = new HashMap<>();
    private final JTabbedPane tabbedPane = new JTabbedPane();

    public ViewInventoryTab() {
        setLayout(new BorderLayout(10, 10));
        refreshDataAndTabs();

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = UIComponentUtils.createFormattedTextField();
        searchField.setPreferredSize(new Dimension(200, 30));
        JComboBox<String> statusFilter = UIComponentUtils.createFormattedComboBox(new String[]{"All", "Deployed", "In Storage", "Needs Repair"});
        statusFilter.setPreferredSize(new Dimension(100, 30)); // Skinnier width
        JComboBox<String> deptFilter = UIComponentUtils.createFormattedComboBox(new String[]{"All"});
        deptFilter.setPreferredSize(new Dimension(100, 30)); // Skinnier width
        JButton filterButton = UIComponentUtils.createFormattedButton("Filter");
        JButton refreshButton = UIComponentUtils.createFormattedButton("Refresh");

        filterPanel.add(UIComponentUtils.createAlignedLabel("Search:"));
        filterPanel.add(searchField);
        filterPanel.add(UIComponentUtils.createAlignedLabel("Status:"));
        filterPanel.add(statusFilter);
        filterPanel.add(UIComponentUtils.createAlignedLabel("Department:"));
        filterPanel.add(deptFilter);
        filterPanel.add(filterButton);
        filterPanel.add(refreshButton);

        add(filterPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        // Update department filter
        ArrayList<String> departments = new ArrayList<>();
        departments.add("All");
        for (HashMap<String, String> device : InventoryData.getDevices()) {
            String dept = device.getOrDefault("Department", "");
            if (!dept.isEmpty() && !departments.contains(dept)) {
                departments.add(dept);
            }
        }
        deptFilter.setModel(new DefaultComboBoxModel<>(departments.toArray(new String[0])));

        filterButton.addActionListener(e -> {
            String searchText = searchField.getText().toLowerCase();
            String status = (String) statusFilter.getSelectedItem();
            String dept = (String) deptFilter.getSelectedItem();
            updateTables(searchText, status, dept);
        });

        refreshButton.addActionListener(e -> {
            refreshDataAndTabs();
            updateTables("", "All", "All");
        });

        // Add right-click popup for all tables
        for (String type : tables.keySet()) {
            JTable table = tables.get(type);
            addTablePopup(table);
        }
    }

    public void refreshDataAndTabs() {
        FileUtils.loadDevices();
        System.out.println("[DEBUG] After refreshData, InventoryData.getDevices(): " + InventoryData.getDevices());

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
                // Normalize keys by replacing special characters (e.g., "/" with "_")
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
                // Check normalized version if original key has special characters
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
                table.getColumnModel().getColumn(i).setPreferredWidth(100); // Uniform width for dynamic columns
            }
            JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(table);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            scrollPane.getVerticalScrollBar().setUnitIncrement(20);
            scrollPane.getVerticalScrollBar().setBlockIncrement(60);
            scrollPane.setDoubleBuffered(true);

            tableModels.put(type, tableModel);
            tables.put(type, table);
            tabbedPane.addTab(type, scrollPane);
            addTablePopup(table);
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

                if ((searchText == null || searchText.isEmpty() || deviceName.toLowerCase().contains(searchText) || serial.toLowerCase().contains(searchText)) &&
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

    private void addTablePopup(JTable table) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem modifyItem = new JMenuItem("Change/Modify");
        JMenuItem removeItem = new JMenuItem("Remove Entry");
        popupMenu.add(modifyItem);
        popupMenu.add(removeItem);

        modifyItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String type = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
                HashMap<String, String> device = new HashMap<>();
                for (int col = 0; col < table.getColumnCount(); col++) {
                    device.put(table.getColumnName(col).replace(" ", "_"), (String) table.getValueAt(row, col));
                }
                showModifyDialog(device, type);
            }
        });

        removeItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String serialNumber = (String) table.getValueAt(row, table.getColumnModel().getColumnIndex("Serial Number"));
                String deviceName = (String) table.getValueAt(row, table.getColumnModel().getColumnIndex("Device Name"));
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to remove device '" + deviceName + "' (Serial: " + serialNumber + ")?",
                    "Confirm Removal",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    boolean removed = false;
                    for (int i = 0; i < InventoryData.getDevices().size(); i++) {
                        HashMap<String, String> device = InventoryData.getDevices().get(i);
                        if (device.get("Serial_Number").equals(serialNumber)) {
                            InventoryData.getDevices().remove(i);
                            removed = true;
                            break;
                        }
                    }
                    if (removed) {
                        FileUtils.saveDevices();
                        refreshDataAndTabs();
                        updateTables("", "All", "All");
                        JOptionPane.showMessageDialog(this, "Device removed successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Error: Device not found", "Removal Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        popupMenu.show(table, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void showModifyDialog(HashMap<String, String> device, String deviceType) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Define default categories for editing
        List<String> defaultCategories = Arrays.asList(
            "Device Name", "Device Type", "Serial Number", "Status", "Department", "Added Memory",
            "Added Storage", "Purchase Date", "Warranty Expiry", "Purchase Cost", "Vendor", "Assigned User",
            "OS Version", "Room", "Desk", "Maintenance Due", "Storage Capacity", "Memory Space",
            "Warranty Guarantee", "Acquisition Cost", "Supplier", "Operating System", "Specification Details",
            "Purchase Expense", "Storage Expansion", "Brand", "Manufacturer", "Building Location",
            "Site", "Division", "Team", "Unit", "Test", "Trial", "Experiment", "Evaluation", "Check",
            "Review", "Assessment"
        );

        // Determine dynamic fields based on device data and default categories
        List<String> columnNames = new ArrayList<>();
        Set<String> allKeys = new HashSet<>(device.keySet());
        allKeys.addAll(defaultCategories.stream().map(s -> s.replace(" ", "_")).collect(Collectors.toSet()));
        for (String key : allKeys) {
            columnNames.add(key.replace("_", " "));
        }
        Collections.sort(columnNames); // Sort for consistent display

        JComponent[] inputs = new JComponent[columnNames.size()];
        HashMap<String, String> originalValues = new HashMap<>(device);
        for (int i = 0; i < columnNames.size(); i++) {
            String fieldName = columnNames.get(i);
            JComponent input;
            String key = fieldName.replace(" ", "_");
            if (fieldName.equals("Status")) {
                JComboBox<String> combo = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
                combo.setSelectedItem(device.getOrDefault(key, "Deployed"));
                input = combo;
            } else if (fieldName.equals("Added Memory") || fieldName.equals("Added Storage")) {
                JComboBox<String> combo = UIComponentUtils.createFormattedComboBox(new String[]{"TRUE", "FALSE", "null"});
                combo.setSelectedItem(device.getOrDefault(key, "null"));
                input = combo;
            } else if (fieldName.contains("Date")) {
                JPanel datePicker = UIComponentUtils.createFormattedDatePicker();
                JTextField dateField = (JTextField) datePicker.getComponent(0);
                dateField.setText(device.getOrDefault(key, ""));
                input = datePicker;
            } else {
                JTextField field = UIComponentUtils.createFormattedTextField();
                field.setText(device.getOrDefault(key, ""));
                if (fieldName.equals("Serial Number") || fieldName.equals("Device Type")) {
                    field.setEditable(false);
                }
                input = field;
            }
            panel.add(UIComponentUtils.createAlignedLabel(fieldName + ":"));
            panel.add(input);
            inputs[i] = input;
        }

        JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JDialog dialog = new JDialog();
        dialog.setTitle("Modify Device");
        dialog.setModal(true);
        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setSize(500, Math.min(600, 50 + 40 * columnNames.size()));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = UIComponentUtils.createFormattedButton("Save");
        JButton cancelButton = UIComponentUtils.createFormattedButton("Cancel");
        saveButton.addActionListener(e -> {
            HashMap<String, String> updatedDevice = new HashMap<>();
            for (int i = 0; i < columnNames.size(); i++) {
                String key = columnNames.get(i).replace(" ", "_");
                String value;
                if (inputs[i] instanceof JTextField) {
                    value = ((JTextField) inputs[i]).getText();
                } else if (inputs[i] instanceof JComboBox) {
                    value = (String) ((JComboBox<?>) inputs[i]).getSelectedItem();
                } else {
                    JTextField dateField = (JTextField) ((JPanel) inputs[i]).getComponent(0);
                    value = dateField.getText();
                    System.out.println("[DEBUG] Saving " + key + ": " + value);
                }
                updatedDevice.put(key, value);
            }

            System.out.println("[DEBUG] Updated device before validation: " + updatedDevice);

            boolean hasChanges = false;
            for (String key : updatedDevice.keySet()) {
                String original = originalValues.getOrDefault(key, "");
                String updated = updatedDevice.getOrDefault(key, "");
                if (!original.equals(updated)) {
                    hasChanges = true;
                    System.out.println("[DEBUG] Change detected in " + key + ": " + original + " -> " + updated);
                    break;
                }
            }

            if (hasChanges) {
                int confirm = JOptionPane.showConfirmDialog(
                    dialog,
                    "Are you sure you want to save?",
                    "Confirm Save",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            String error = DataUtils.validateDevice(updatedDevice, device.get("Serial_Number"));
            if (error != null) {
                JOptionPane.showMessageDialog(dialog, "Error: " + error, "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean updated = false;
            for (int i = 0; i < InventoryData.getDevices().size(); i++) {
                HashMap<String, String> d = InventoryData.getDevices().get(i);
                if (d.get("Serial_Number").equals(device.get("Serial_Number"))) {
                    InventoryData.getDevices().set(i, updatedDevice);
                    updated = true;
                    System.out.println("[DEBUG] Updated device in InventoryData: " + updatedDevice);
                    break;
                }
            }
            if (!updated) {
                JOptionPane.showMessageDialog(dialog, "Error: Device not found for update", "Update Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            FileUtils.saveDevices();
            System.out.println("[DEBUG] After saveDevices, InventoryData.getDevices(): " + InventoryData.getDevices());
            refreshDataAndTabs();
            updateTables("", "All", "All");
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> {
            boolean hasChanges = false;
            for (int i = 0; i < columnNames.size(); i++) {
                String key = columnNames.get(i).replace(" ", "_");
                String currentValue;
                if (inputs[i] instanceof JTextField) {
                    currentValue = ((JTextField) inputs[i]).getText();
                } else if (inputs[i] instanceof JComboBox) {
                    currentValue = (String) ((JComboBox<?>) inputs[i]).getSelectedItem();
                } else {
                    currentValue = ((JTextField) ((JPanel) inputs[i]).getComponent(0)).getText();
                }
                String originalValue = originalValues.getOrDefault(key, "");
                if (!currentValue.equals(originalValue)) {
                    hasChanges = true;
                    System.out.println("[DEBUG] Cancel change detected in " + key + ": " + originalValue + " -> " + currentValue);
                    break;
                }
            }

            if (hasChanges) {
                int confirm = JOptionPane.showConfirmDialog(
                    dialog,
                    "Are you sure you want to cancel? Any changes made will be lost.",
                    "Confirm Cancel",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            dialog.dispose();
        });
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}