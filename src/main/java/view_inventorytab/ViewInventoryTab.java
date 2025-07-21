package view_inventorytab;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import utils.UIComponentUtils;

public class ViewInventoryTab extends JPanel {
    private final JTable table;
    private final TableManager tableManager;
    private final JTextField searchField;

    public ViewInventoryTab() {
        setLayout(new BorderLayout());

        // Initialize table and manager
        table = new JTable();
        tableManager = new TableManager(table);
        JScrollPane scrollPane = new JScrollPane(table);

        // Add search bar
        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        searchField = UIComponentUtils.createFormattedTextField();
        searchField.setPreferredSize(new java.awt.Dimension(200, 30));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            private void filterTable() {
                String text = searchField.getText().toLowerCase();
                TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
                if (sorter != null) {
                    sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + text));
                }
            }
        });
        searchPanel.add(UIComponentUtils.createAlignedLabel("Search:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        // Add sorting on column header click
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 1 && e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    int columnIndex = table.columnAtPoint(e.getPoint());
                    if (columnIndex >= 0) {
                        tableManager.sortTable(columnIndex);
                    }
                }
            }
        });

        add(searchPanel, BorderLayout.NORTH);
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