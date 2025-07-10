package view_inventorytab;

import java.awt.BorderLayout;

import javax.swing.JPanel;

public final class ViewInventoryTab extends JPanel {
    private final TableManager tableManager;
    private final FilterPanel filterPanel;

    public ViewInventoryTab() {
        setLayout(new BorderLayout(10, 10));

        // Initialize table manager for handling tables and tabs
        tableManager = new TableManager();
        add(tableManager.getTabbedPane(), BorderLayout.CENTER);

        // Initialize filter panel
        filterPanel = new FilterPanel(this::updateTables, this::refreshDataAndTabs);
        add(filterPanel.getPanel(), BorderLayout.NORTH);

        // Initial data load and table update
        refreshDataAndTabs();
    }

    public void refreshDataAndTabs() {
        tableManager.refreshDataAndTabs();
        updateTables("", "All", "All");
    }

    public void updateTables(String searchText, String statusFilter, String deptFilter) {
        tableManager.updateTables(searchText, statusFilter, deptFilter);
    }
}