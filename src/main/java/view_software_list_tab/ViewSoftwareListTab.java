package view_software_list_tab;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import utils.DatabaseUtils;
import utils.TablesNotIncludedList;
import utils.UIComponentUtils;
import view_software_list_tab.view_software_details.DeviceDetailsPanel;

public class ViewSoftwareListTab extends JPanel {
    private final JTable table;
    private final TableManager tableManager;
    private final JTextField searchField;
    private final JPanel mainPanel;
    private JPanel currentView;
    private FilterPanel filterPanel;
    private JList<String> tableList;

    public ViewSoftwareListTab() {
        setLayout(new BorderLayout());

        mainPanel = new JPanel(new BorderLayout());
        table = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tableManager = new TableManager(table);
        JScrollPane scrollPane = new JScrollPane(table);

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

        filterPanel = new FilterPanel(
            (search, status, dept) -> updateTables(search, status, dept),
            this::refreshDataAndTabs
        );
        searchPanel.add(filterPanel.getPanel(), BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        splitPane.setDividerLocation(200);

        JScrollPane listScrollPane = createTableListScrollPane();
        splitPane.setLeftComponent(listScrollPane);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(searchPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        splitPane.setRightComponent(tablePanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        currentView = mainPanel;
        add(currentView, BorderLayout.CENTER);

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

                if (e.isPopupTrigger()) {
                    if (!table.isRowSelected(rowIndex)) {
                        table.setRowSelectionInterval(rowIndex, rowIndex);
                        table.setColumnSelectionInterval(columnIndex, columnIndex);
                    }
                    return;
                }

                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (e.isControlDown()) {
                        if (table.getSelectedColumn() != columnIndex && table.getSelectedColumnCount() > 0) {
                            return;
                        }
                        if (table.isCellSelected(rowIndex, columnIndex)) {
                            table.removeRowSelectionInterval(rowIndex, rowIndex);
                        } else {
                            table.addRowSelectionInterval(rowIndex, rowIndex);
                            table.addColumnSelectionInterval(columnIndex, columnIndex);
                        }
                    } else if (e.isShiftDown()) {
                        if (table.getSelectedColumn() != columnIndex && table.getSelectedColumnCount() > 0) {
                            return;
                        }
                        int anchorRow = table.getSelectionModel().getAnchorSelectionIndex();
                        if (anchorRow >= 0) {
                            int start = Math.min(anchorRow, rowIndex);
                            int end = Math.max(anchorRow, rowIndex);
                            table.setRowSelectionInterval(start, end);
                            table.setColumnSelectionInterval(columnIndex, columnIndex);
                        }
                    } else {
                        table.setRowSelectionInterval(rowIndex, rowIndex);
                        table.setColumnSelectionInterval(columnIndex, columnIndex);
                    }
                }
            }
        });

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

        PopupHandler.addTablePopup(table, this, tableManager);
        initialize();
    }

    private JScrollPane createTableListScrollPane() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        try {
            List<String> tableNames = DatabaseUtils.getTableNames();
            List<String> excluded = TablesNotIncludedList.getExcludedTablesForSoftwareImporter();
            for (String table : tableNames) {
                if (!excluded.contains(table)) {
                    listModel.addElement(table);
                }
            }
            if (listModel.isEmpty()) {
                listModel.addElement("No tables available");
            }
        } catch (SQLException e) {
            System.err.println("ViewSoftwareListTab: Error fetching table names: " + e.getMessage());
            listModel.addElement("Error: " + e.getMessage());
        }

        tableList = new JList<>(listModel);
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableList.setFixedCellWidth(180);
        tableList.setFixedCellHeight(25);

        tableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = tableList.getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    updateTableView(selectedTable);
                }
            }
        });

        // Initial selection
        tableList.setSelectedValue("Inventory", true);

        return new JScrollPane(tableList);
    }

    private void updateTableView(String tableName) {
        tableManager.setTableName(tableName);
        mainPanel.remove(filterPanel.getPanel());
        filterPanel = new FilterPanel(
            (search, status, dept) -> updateTables(search, status, dept),
            this::refreshDataAndTabs
        );
        mainPanel.add(filterPanel.getPanel(), BorderLayout.SOUTH);
        mainPanel.revalidate();
        mainPanel.repaint();
        refreshDataAndTabs();
    }

    private void initialize() {
        System.out.println("ViewSoftwareListTab: Initializing table");
        refreshDataAndTabs();
    }

    public void refreshDataAndTabs() {
        tableManager.refreshDataAndTabs();
    }

    public void updateTables(String searchTerm, String status, String dept) {
        refreshDataAndTabs();
        String text = searchTerm.toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        if (sorter != null) {
            javax.swing.RowFilter<DefaultTableModel, Integer> filter = null;
            if (!text.isEmpty() || !status.equals("All") || !dept.equals("All")) {
                filter = new javax.swing.RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        boolean matchesSearch = text.isEmpty();
                        boolean matchesStatus = status.equals("All");
                        boolean matchesDept = dept.equals("All");

                        if (!text.isEmpty()) {
                            for (int i = 1; i < entry.getModel().getColumnCount(); i++) {
                                Object value = entry.getValue(i);
                                if (value != null && value.toString().toLowerCase().contains(text)) {
                                    matchesSearch = true;
                                    break;
                                }
                            }
                        }

                        int statusIndex = getColumnIndex("Status");
                        if (!status.equals("All") && statusIndex != -1) {
                            Object value = entry.getValue(statusIndex);
                            matchesStatus = value != null && value.toString().equals(status);
                        }

                        int deptIndex = getColumnIndex("Department");
                        if (!dept.equals("All") && deptIndex != -1) {
                            Object value = entry.getValue(deptIndex);
                            matchesDept = value != null && value.toString().equals(dept);
                        }

                        return matchesSearch && matchesStatus && matchesDept;
                    }

                    private int getColumnIndex(String columnName) {
                        for (int i = 0; i < table.getColumnCount(); i++) {
                            if (table.getColumnName(i).equals(columnName)) {
                                return i;
                            }
                        }
                        return -1;
                    }
                };
            }
            sorter.setRowFilter(filter);
        }
    }

    public void showDeviceDetails(String assetName) {
        remove(currentView);
        currentView = new DeviceDetailsPanel(assetName, this);
        add(currentView, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showMainView() {
        remove(currentView);
        currentView = mainPanel;
        add(currentView, BorderLayout.CENTER);
        refreshDataAndTabs();
        revalidate();
        repaint();
    }
}