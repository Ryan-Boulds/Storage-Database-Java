package view_inventorytab;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import utils.InventoryData;
import utils.UIComponentUtils;

public class FilterPanel {
    private final JPanel filterPanel;
    private final JTextField searchField;
    private final JComboBox<String> statusFilter;
    private final JComboBox<String> deptFilter;
    private final boolean hasStatusColumn;
    private final boolean hasDepartmentColumn;

    public FilterPanel(TriConsumer<String, String, String> filterAction, Runnable refreshAction) {
        filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = UIComponentUtils.createFormattedTextField();
        searchField.setPreferredSize(new Dimension(200, 30));

        hasStatusColumn = checkColumnExists("Status");
        hasDepartmentColumn = checkColumnExists("Department");

        String[] statusOptions = hasStatusColumn ? new String[]{"All", "Deployed", "In Storage", "Needs Repair"} : new String[]{"All"};
        statusFilter = UIComponentUtils.createFormattedComboBox(statusOptions);
        statusFilter.setPreferredSize(new Dimension(100, 30));

        String[] deptOptions = hasDepartmentColumn ? new String[]{"All"} : new String[]{"All"};
        deptFilter = UIComponentUtils.createFormattedComboBox(deptOptions);
        deptFilter.setPreferredSize(new Dimension(100, 30));

        JButton filterButton = UIComponentUtils.createFormattedButton("Filter");
        JButton refreshButton = UIComponentUtils.createFormattedButton("Refresh");

        filterPanel.add(UIComponentUtils.createAlignedLabel("Search:"));
        filterPanel.add(searchField);
        if (hasStatusColumn) {
            filterPanel.add(UIComponentUtils.createAlignedLabel("Status:"));
            filterPanel.add(statusFilter);
        }
        if (hasDepartmentColumn) {
            filterPanel.add(UIComponentUtils.createAlignedLabel("Department:"));
            filterPanel.add(deptFilter);
        }
        filterPanel.add(filterButton);
        filterPanel.add(refreshButton);

        if (hasDepartmentColumn) {
            updateDepartmentFilter();
        }

        filterButton.addActionListener(e -> filterAction.accept(getSearchText(), getStatusFilter(), getDeptFilter()));
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            deptFilter.setSelectedIndex(0);
            refreshAction.run();
        });
    }

    private boolean checkColumnExists(String columnName) {
        ArrayList<HashMap<String, String>> devices = InventoryData.getDevices();
        if (devices != null && !devices.isEmpty()) {
            return devices.get(0).containsKey(columnName);
        }
        return false;
    }

    public JPanel getPanel() {
        return filterPanel;
    }

    public String getSearchText() {
        return searchField.getText().toLowerCase();
    }

    public String getStatusFilter() {
        return hasStatusColumn ? (String) statusFilter.getSelectedItem() : "All";
    }

    public String getDeptFilter() {
        return hasDepartmentColumn ? (String) deptFilter.getSelectedItem() : "All";
    }

    private void updateDepartmentFilter() {
        ArrayList<String> departments = new ArrayList<>();
        departments.add("All");
        for (HashMap<String, String> device : InventoryData.getDevices()) {
            String dept = device.getOrDefault("Department", "");
            if (!dept.isEmpty() && !departments.contains(dept)) {
                departments.add(dept);
            }
        }
        System.out.println("FilterPanel: Departments: " + departments);
        deptFilter.setModel(new DefaultComboBoxModel<>(departments.toArray(new String[0])));
    }
}