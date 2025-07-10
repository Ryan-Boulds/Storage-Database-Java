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
    public FilterPanel(TriConsumer<String, String, String> filterAction, Runnable refreshAction) {
        filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = UIComponentUtils.createFormattedTextField();
        searchField.setPreferredSize(new Dimension(200, 30));
        statusFilter = UIComponentUtils.createFormattedComboBox(new String[]{"All", "Deployed", "In Storage", "Needs Repair"});
        statusFilter.setPreferredSize(new Dimension(100, 30));
        deptFilter = UIComponentUtils.createFormattedComboBox(new String[]{"All"});
        deptFilter.setPreferredSize(new Dimension(100, 30));
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

        // Update department filter
        updateDepartmentFilter();

        filterButton.addActionListener(e -> filterAction.accept(getSearchText(), getStatusFilter(), getDeptFilter()));
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            deptFilter.setSelectedIndex(0);
            refreshAction.run();
        });
    }

    public JPanel getPanel() {
        return filterPanel;
    }

    public String getSearchText() {
        return searchField.getText().toLowerCase();
    }

    public String getStatusFilter() {
        return (String) statusFilter.getSelectedItem();
    }

    public String getDeptFilter() {
        return (String) deptFilter.getSelectedItem();
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
        deptFilter.setModel(new DefaultComboBoxModel<>(departments.toArray(new String[0])));
    }
}