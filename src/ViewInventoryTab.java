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
        popupMenu.add(modifyItem);

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
            tableModel.addRow(new Object[]{
                device.getOrDefault("Device_Name", ""),
                device.getOrDefault("Device_Type", ""),
                device.getOrDefault("Brand", ""),
                device.getOrDefault("Model", ""),
                device.getOrDefault("Serial_Number", ""),
                device.getOrDefault("Status", ""),
                device.getOrDefault("Department", ""),
                device.getOrDefault("Warranty_Expiry_Date", ""),
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

        JTextField[] fields = new JTextField[21];
        String[] columnNames = {"Device Name", "Device Type", "Brand", "Model", "Serial Number", "Status", "Department", "Warranty Expiry", "Network Address", "Purchase Cost", "Vendor", "OS Version", "Assigned User", "Building Location", "Room/Desk", "Specification", "Added Memory", "Added Storage", "Last Maintenance", "Maintenance Due", "Memory (RAM)"};
        for (int i = 0; i < columnNames.length; i++) {
            JTextField field = UIComponentUtils.createFormattedTextField();
            field.setText(device.getOrDefault(columnNames[i].replace(" ", "_"), ""));
            panel.add(UIComponentUtils.createAlignedLabel(columnNames[i] + ":"));
            panel.add(field);
            fields[i] = field;
        }

        JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JDialog dialog = new JDialog((Frame) null, "Modify Device", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setSize(400, 300);
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = UIComponentUtils.createFormattedButton("OK");
        JButton cancelButton = UIComponentUtils.createFormattedButton("Cancel");
        okButton.addActionListener(e -> {
            HashMap<String, String> updatedDevice = new HashMap<>();
            for (int i = 0; i < columnNames.length; i++) {
                updatedDevice.put(columnNames[i].replace(" ", "_"), fields[i].getText());
            }
            String error = DataUtils.validateDevice(updatedDevice);
            if (error != null) {
                JOptionPane.showMessageDialog(dialog, "Error: " + error, "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (int i = 0; i < InventoryData.getDevices().size(); i++) {
                HashMap<String, String> d = InventoryData.getDevices().get(i);
                if (d.get("Serial_Number").equals(device.get("Serial_Number"))) {
                    InventoryData.getDevices().set(i, updatedDevice);
                    break;
                }
            }
            FileUtils.saveDevices();
            refreshData();
            updateTable(null, "All", "All", "All");
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}