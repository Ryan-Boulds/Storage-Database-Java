package view_inventorytab;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class ViewInventoryTab extends JPanel {
    private final JTable table;
    private final TableManager tableManager;

    public ViewInventoryTab() {
        setLayout(new BorderLayout());
        table = new JTable();
        tableManager = new TableManager(table);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        initialize();
    }

    private void initialize() {
        refreshDataAndTabs();
    }

    public void refreshDataAndTabs() {
        tableManager.refreshDataAndTabs();
    }

    public void updateTables(String searchTerm, String deviceType, String status) {
        refreshDataAndTabs();
        // TODO: Add filtering logic based on searchTerm, deviceType, and status if required
    }
}