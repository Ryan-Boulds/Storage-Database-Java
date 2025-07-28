package view_inventorytab;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
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
        table = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Only "Edit" column is editable
            }
        };
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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

        // Add mouse listener for selection and popup trigger
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseEvent(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseEvent(e);
            }
            private void handleMouseEvent(MouseEvent e) {
                int columnIndex = table.columnAtPoint(e.getPoint());
                int rowIndex = table.rowAtPoint(e.getPoint());
                if (rowIndex < 0 || columnIndex < 0) return;

                // Handle right-click for popup
                if (e.isPopupTrigger()) {
                    if (!table.isRowSelected(rowIndex)) {
                        table.setRowSelectionInterval(rowIndex, rowIndex);
                        table.setColumnSelectionInterval(columnIndex, columnIndex);
                    }
                    return;
                }

                // Handle left-click for selection
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (e.isControlDown()) {
                        // Ctrl + click: toggle selection (same column only)
                        if (table.getSelectedColumn() != columnIndex && table.getSelectedColumnCount() > 0) {
                            return; // Restrict to same column
                        }
                        if (table.isCellSelected(rowIndex, columnIndex)) {
                            table.removeRowSelectionInterval(rowIndex, rowIndex);
                        } else {
                            table.addRowSelectionInterval(rowIndex, rowIndex);
                            table.addColumnSelectionInterval(columnIndex, columnIndex);
                        }
                    } else if (e.isShiftDown()) {
                        // Shift + click: select range (same column only)
                        if (table.getSelectedColumn() != columnIndex && table.getSelectedColumnCount() > 0) {
                            return; // Restrict to same column
                        }
                        int anchorRow = table.getSelectionModel().getAnchorSelectionIndex();
                        if (anchorRow >= 0) {
                            int start = Math.min(anchorRow, rowIndex);
                            int end = Math.max(anchorRow, rowIndex);
                            table.setRowSelectionInterval(start, end);
                            table.setColumnSelectionInterval(columnIndex, columnIndex);
                        }
                    } else {
                        // Single click: select single cell
                        table.setRowSelectionInterval(rowIndex, rowIndex);
                        table.setColumnSelectionInterval(columnIndex, columnIndex);
                    }
                }
            }
        });

        // Add sorting on column header click
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    int columnIndex = table.getTableHeader().columnAtPoint(e.getPoint());
                    if (columnIndex >= 0) {
                        tableManager.sortTable(columnIndex);
                    }
                }
            }
        });

        // Add popup menu
        PopupHandler.addTablePopup(table, null, tableManager);

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
    }
}