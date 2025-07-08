import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import utils.DataUtils;
import utils.FileUtils;
import utils.InventoryData;
import utils.UIComponentUtils;

public final class ViewInventoryTab extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;

    public ViewInventoryTab() {
        setLayout(new BorderLayout(10, 10));
        refreshData();

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = UIComponentUtils.createFormattedTextField();
        searchField.setPreferredSize(new Dimension(200, 30));
        JComboBox<String> typeFilter = UIComponentUtils.createFormattedComboBox(new String[]{"All", "Computer", "Printer", "Router", "Switch"});
        JComboBox<String> statusFilter = UIComponentUtils.createFormattedComboBox(new String[]{"All", "Deployed", "In Storage", "Needs Repair"});
        JComboBox<String> deptFilter = UIComponentUtils.createFormattedComboBox(new String[]{"All"});
        JButton filterButton = UIComponentUtils.createFormattedButton("Filter");
        JButton refreshButton = UIComponentUtils.createFormattedButton("Refresh");

        filterPanel.add(UIComponentUtils.createAlignedLabel("Search:"));
        filterPanel.add(searchField);
        filterPanel.add(UIComponentUtils.createAlignedLabel("Device Type:"));
        filterPanel.add(typeFilter);
        filterPanel.add(UIComponentUtils.createAlignedLabel("Status:"));
        filterPanel.add(statusFilter);
        filterPanel.add(UIComponentUtils.createAlignedLabel("Department:"));
        filterPanel.add(deptFilter);
        filterPanel.add(filterButton);
        filterPanel.add(refreshButton);

        String[] columns = {"Device Name", "Device Type", "Brand", "Model", "Serial Number", "Status", "Department", "Warranty Expiry", "Network Address", "Purchase Cost", "Vendor", "OS Version", "Assigned User", "Building Location", "Room/Desk", "Specification", "Added Memory", "Added Storage", "Last Maintenance", "Maintenance Due", "Memory (RAM)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);
        table.getColumnModel().getColumn(7).setPreferredWidth(100);
        table.getColumnModel().getColumn(8).setPreferredWidth(100);
        table.getColumnModel().getColumn(9).setPreferredWidth(80);
        table.getColumnModel().getColumn(10).setPreferredWidth(80);
        table.getColumnModel().getColumn(11).setPreferredWidth(80);
        table.getColumnModel().getColumn(12).setPreferredWidth(100);
        table.getColumnModel().getColumn(13).setPreferredWidth(100);
        table.getColumnModel().getColumn(14).setPreferredWidth(80);
        table.getColumnModel().getColumn(15).setPreferredWidth(100);
        table.getColumnModel().getColumn(16).setPreferredWidth(80);
        table.getColumnModel().getColumn(17).setPreferredWidth(80);
        table.getColumnModel().getColumn(18).setPreferredWidth(100);
        table.getColumnModel().getColumn(19).setPreferredWidth(100);
        table.getColumnModel().getColumn(20).setPreferredWidth(100);

        JScrollPane tableScrollPane = UIComponentUtils.createScrollableContentPanel(table);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        tableScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        tableScrollPane.getVerticalScrollBar().setBlockIncrement(60);
        tableScrollPane.setDoubleBuffered(true);

        updateTable(null, "All", "All", "All");

        filterButton.addActionListener(e -> {
            String searchText = searchField.getText().toLowerCase();
            String type = (String) typeFilter.getSelectedItem();
            String status = (String) statusFilter.getSelectedItem();
            String dept = (String) deptFilter.getSelectedItem();
            updateTable(searchText.isEmpty() ? null : searchText, type, status, dept);
        });

        refreshButton.addActionListener(e -> {
            refreshData();
            updateTable(null, "All", "All", "All");
        });

        ArrayList<String> departments = new ArrayList<>();
        departments.add("All");
        for (HashMap<String, String> device : InventoryData.getDevices()) {
            String dept = device.getOrDefault("Department", "");
            if (!dept.isEmpty() && !departments.contains(dept)) {
                departments.add(dept);
            }
        }
        deptFilter.setModel(new DefaultComboBoxModel<>(departments.toArray(new String[departments.size()])));

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem modifyItem = new JMenuItem("Change/Modify");
        JMenuItem removeItem = new JMenuItem("Remove Entry");
        popupMenu.add(modifyItem);
        popupMenu.add(removeItem);

        modifyItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                HashMap<String, String> device = new HashMap<>();
                for (int col = 0; col < table.getColumnCount(); col++) {
                    device.put(table.getColumnName(col).replace(" ", "_"), (String) table.getValueAt(row, col));
                }
                showModifyDialog(device);
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
                        refreshData();
                        updateTable(null, "All", "All", "All");
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

        table.setAutoCreateRowSorter(true);

        add(filterPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    public void refreshData() {
        FileUtils.loadDevices();
        System.out.println("[DEBUG] After refreshData, InventoryData.getDevices(): " + InventoryData.getDevices());
    }

    public void updateTable(String searchText, String typeFilter, String statusFilter, String deptFilter) {
        tableModel.setRowCount(0);
        ArrayList<HashMap<String, String>> filteredDevices = new ArrayList<>();
        for (HashMap<String, String> device : InventoryData.getDevices()) {
            String deviceName = device.getOrDefault("Device_Name", "");
            String deviceType = device.getOrDefault("Device_Type", "");
            String serial = device.getOrDefault("Serial_Number", "");
            String status = device.getOrDefault("Status", "");
            String dept = device.getOrDefault("Department", "");

            if ((searchText == null || deviceName.toLowerCase().contains(searchText) || serial.toLowerCase().contains(searchText)) &&
                (typeFilter.equals("All") || deviceType.equals(typeFilter)) &&
                (statusFilter.equals("All") || status.equals(statusFilter)) &&
                (deptFilter.equals("All") || dept.equals(deptFilter))) {
                filteredDevices.add(device);
            }
        }
        for (HashMap<String, String> device : filteredDevices) {
            System.out.println("[DEBUG] Adding device to table: " + device);
            tableModel.addRow(new Object[]{
                device.getOrDefault("Device_Name", ""),
                device.getOrDefault("Device_Type", ""),
                device.getOrDefault("Brand", ""),
                device.getOrDefault("Model", ""),
                device.getOrDefault("Serial_Number", ""),
                device.getOrDefault("Status", ""),
                device.getOrDefault("Department", ""),
                device.getOrDefault("Warranty_Expiry_Date", device.getOrDefault("Warranty_Expiry", "")),
                device.getOrDefault("Network_Address", ""),
                device.getOrDefault("Purchase_Cost", ""),
                device.getOrDefault("Vendor", ""),
                device.getOrDefault("OS_Version", ""),
                device.getOrDefault("Assigned_User", ""),
                device.getOrDefault("Building_Location", ""),
                device.getOrDefault("Room_Desk", ""),
                device.getOrDefault("Specification", ""),
                device.getOrDefault("Added_Memory", ""),
                device.getOrDefault("Added_Storage", ""),
                device.getOrDefault("Last_Maintenance", ""),
                device.getOrDefault("Maintenance_Due", ""),
                device.getOrDefault("Memory_RAM", "")
            });
        }
    }

    private void showModifyDialog(HashMap<String, String> device) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        String deviceType = device.getOrDefault("Device_Type", "");
        String[] computerFields = {
            "Device Name", "Device Type", "Brand", "Model", "Serial Number", "Processor Type",
            "Storage Capacity", "Network Address", "Department", "Purchase Cost", "Vendor",
            "OS Version", "Assigned User", "Building Location", "Room/Desk", "Specification",
            "Added Memory", "Added Storage", "Last Maintenance", "Maintenance Due", "Memory (RAM)",
            "Status", "Warranty Expiry", "Date of Purchase"
        };
        String[] otherFields = {
            "Device Name", "Device Type", "Brand", "Model", "Serial Number", "Network Address",
            "Specification", "Department", "Building Location", "Room/Desk", "Status",
            "Purchase Cost", "Vendor", "Warranty Expiry", "Date of Purchase"
        };
        String[] columnNames = deviceType.equals("Computer") ? computerFields : otherFields;

        JComponent[] inputs = new JComponent[columnNames.length];
        HashMap<String, String> originalValues = new HashMap<>(device); // Store original values for change detection
        for (int i = 0; i < columnNames.length; i++) {
            String fieldName = columnNames[i];
            JComponent input;
            if (fieldName.equals("Status")) {
                JComboBox<String> combo = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
                combo.setSelectedItem(device.getOrDefault("Status", "Deployed"));
                input = combo;
            } else if (fieldName.equals("Added Memory") || fieldName.equals("Added Storage")) {
                JComboBox<String> combo = UIComponentUtils.createFormattedComboBox(new String[]{"TRUE", "FALSE", "null"});
                combo.setSelectedItem(device.getOrDefault(fieldName.replace(" ", "_"), "null"));
                input = combo;
            } else if (fieldName.equals("Warranty Expiry") || fieldName.equals("Last Maintenance") ||
                       fieldName.equals("Maintenance Due") || fieldName.equals("Date of Purchase")) {
                JPanel datePicker = UIComponentUtils.createFormattedDatePicker();
                JTextField dateField = (JTextField) datePicker.getComponent(0);
                String dateValue = device.getOrDefault(fieldName.replace(" ", "_") + "_Date", device.getOrDefault(fieldName.replace(" ", "_"), ""));
                dateField.setText(dateValue);
                input = datePicker;
            } else {
                JTextField field = UIComponentUtils.createFormattedTextField();
                field.setText(device.getOrDefault(fieldName.replace(" ", "_"), ""));
                if (fieldName.equals("Serial Number") || fieldName.equals("Device Type")) {
                    field.setEditable(false); // Make Serial Number and Device Type non-editable
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

        JDialog dialog = new JDialog((Frame) null, "Modify Device", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setSize(500, deviceType.equals("Computer") ? 600 : 450);
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = UIComponentUtils.createFormattedButton("Save");
        JButton cancelButton = UIComponentUtils.createFormattedButton("Cancel");
        saveButton.addActionListener(e -> {
            HashMap<String, String> updatedDevice = new HashMap<>();
            for (int i = 0; i < columnNames.length; i++) {
                String key = columnNames[i].replace(" ", "_");
                if (columnNames[i].equals("Warranty Expiry") || columnNames[i].equals("Last Maintenance") ||
                    columnNames[i].equals("Maintenance Due") || columnNames[i].equals("Date of Purchase")) {
                    key += "_Date";
                }
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

            // Log the full updated device
            System.out.println("[DEBUG] Updated device before validation: " + updatedDevice);

            // Check for changes
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

            // Show save confirmation
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
            refreshData();
            updateTable(null, "All", "All", "All");
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> {
            // Check for changes
            boolean hasChanges = false;
            for (int i = 0; i < columnNames.length; i++) {
                String key = columnNames[i].replace(" ", "_");
                if (columnNames[i].equals("Warranty Expiry") || columnNames[i].equals("Last Maintenance") ||
                    columnNames[i].equals("Maintenance Due") || columnNames[i].equals("Date of Purchase")) {
                    key += "_Date";
                }
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