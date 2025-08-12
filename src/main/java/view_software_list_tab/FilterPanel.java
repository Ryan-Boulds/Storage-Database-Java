package view_software_list_tab;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import utils.DatabaseUtils;
import utils.UIComponentUtils;

public class FilterPanel {
    private final JPanel filterPanel;
    private final JTextField searchField;
    private final JComboBox<String> statusFilter;
    private final JComboBox<String> deptFilter;
    private boolean hasStatusColumn;
    private boolean hasDepartmentColumn;
    private final JLabel searchLabel;
    private final JLabel statusLabel;
    private final JLabel deptLabel;
    private final JButton refreshButton;
    private final TriConsumer<String, String, String> filterAction;

    public FilterPanel(TriConsumer<String, String, String> filterAction, Runnable refreshAction) {
        this.filterAction = filterAction;
        filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchLabel = UIComponentUtils.createAlignedLabel("Search:");
        searchField = UIComponentUtils.createFormattedTextField();
        searchField.setPreferredSize(new Dimension(200, 30));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        statusLabel = UIComponentUtils.createAlignedLabel("Status:");
        statusFilter = UIComponentUtils.createFormattedComboBox(new String[]{"All", "Deployed", "In Storage", "Needs Repair"});
        statusFilter.setPreferredSize(new Dimension(100, 30));
        statusFilter.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyFilter();
            }
        });

        deptLabel = UIComponentUtils.createAlignedLabel("Department:");
        deptFilter = UIComponentUtils.createFormattedComboBox(new String[]{"All"});
        deptFilter.setPreferredSize(new Dimension(100, 30));
        deptFilter.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyFilter();
            }
        });

        refreshButton = UIComponentUtils.createFormattedButton("Refresh");
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            deptFilter.setSelectedIndex(0);
            refreshAction.run();
        });

        // Initial layout without specific table filters
        filterPanel.add(searchLabel);
        filterPanel.add(searchField);
        filterPanel.add(refreshButton);
    }

    private void applyFilter() {
        filterAction.accept(getSearchText(), getStatusFilter(), getDeptFilter());
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

    public void setTableName(String tableName) {
        hasStatusColumn = checkColumnExists(tableName, "Status");
        hasDepartmentColumn = checkColumnExists(tableName, "Department");

        filterPanel.removeAll();
        filterPanel.add(searchLabel);
        filterPanel.add(searchField);
        if (hasStatusColumn) {
            filterPanel.add(statusLabel);
            filterPanel.add(statusFilter);
        }
        if (hasDepartmentColumn) {
            filterPanel.add(deptLabel);
            filterPanel.add(deptFilter);
            updateDepartmentFilter(tableName);
        }
        filterPanel.add(refreshButton);
        filterPanel.revalidate();
        filterPanel.repaint();
    }

    private boolean checkColumnExists(String tableName, String columnName) {
        if (tableName == null) return false;
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " WHERE 1=0")) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                if (metaData.getColumnName(i).equals(columnName)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("FilterPanel: Error checking column " + columnName + " in table " + tableName + ": " + e.getMessage());
        }
        return false;
    }

    private void updateDepartmentFilter(String tableName) {
        if (tableName == null) return;
        ArrayList<String> departments = new ArrayList<>();
        departments.add("All");
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT Department FROM " + tableName + " WHERE Department IS NOT NULL")) {
            while (rs.next()) {
                String dept = rs.getString("Department");
                if (dept != null && !dept.isEmpty() && !departments.contains(dept)) {
                    departments.add(dept);
                }
            }
        } catch (SQLException e) {
            System.err.println("FilterPanel: Error fetching departments from table " + tableName + ": " + e.getMessage());
        }
        deptFilter.setModel(new DefaultComboBoxModel<>(departments.toArray(new String[0])));
    }
}