package view_software_list_tab;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import utils.DatabaseUtils;
import utils.TablesNotIncludedList;
import view_software_list_tab.license_key_tracker.LicenseKeyTracker;
import view_software_list_tab.view_software_details.DeviceDetailsPanel;

public class ViewSoftwareListTab extends JPanel {
    private final JTable table;
    private final TableManager tableManager;
    private final JPanel mainPanel;
    private JPanel currentView;
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

        FilterPanel filterPanel = new FilterPanel(
                (search, status, dept) -> updateTables(search),
                this::refreshDataAndTabs
        );

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        splitPane.setDividerLocation(200);

        JScrollPane listScrollPane = createTableListScrollPane();
        splitPane.setLeftComponent(listScrollPane);

        JPanel tablePanel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new BorderLayout());
        JButton licenseKeyButton = new JButton("License Key Tracker");
        licenseKeyButton.addActionListener(e -> {
            showLicenseKeyTracker();
        });
        topPanel.add(licenseKeyButton, BorderLayout.WEST);
        topPanel.add(filterPanel.getPanel(), BorderLayout.CENTER);
        tablePanel.add(topPanel, BorderLayout.NORTH);
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
                if (rowIndex < 0 || columnIndex < 0) {
                    return;
                }

                if (e.isPopupTrigger()) {
                    if (!table.isRowSelected(rowIndex)) {
                        table.setRowSelectionInterval(rowIndex, rowIndex);
                        table.setColumnSelectionInterval(columnIndex, columnIndex);
                    }
                    return;
                }

                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (columnIndex == 0) {
                        table.changeSelection(rowIndex, columnIndex, false, false);
                        if (table.editCellAt(rowIndex, columnIndex)) {
                            Component editor = table.getEditorComponent();
                            editor.requestFocusInWindow();
                        }
                    } else {
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

    private void showLicenseKeyTracker() {
        LicenseKeyTracker tracker = new LicenseKeyTracker((JFrame) SwingUtilities.getWindowAncestor(this), tableManager);
        tracker.setVisible(true);
    }

    private JScrollPane createTableListScrollPane() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        try {
            List<String> tableNames = DatabaseUtils.getTableNames();
            List<String> excluded = TablesNotIncludedList.getExcludedTablesForSoftwareImporter();
            List<String> validTables = new ArrayList<>();
            for (String table : tableNames) {
                if (!excluded.contains(table) && !table.equals("Inventory")) {
                    validTables.add(table);
                }
            }
            validTables.sort(String::compareToIgnoreCase);
            for (String table : validTables) {
                listModel.addElement(table);
            }
            if (listModel.isEmpty()) {
                listModel.addElement("No tables available");
            }
        } catch (SQLException e) {
            System.err.println("ViewSoftwareListTab: Error fetching table names: " + e.getMessage());
            listModel.addElement("Error: " + e.getMessage());
        }

        JList<String> localTableList = new JList<>(listModel);
        localTableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        localTableList.setFixedCellWidth(180);
        localTableList.setFixedCellHeight(25);

        localTableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = localTableList.getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    updateTableView(selectedTable);
                }
            }
        });

        if (!listModel.isEmpty() && !listModel.getElementAt(0).startsWith("Error") && !listModel.getElementAt(0).equals("No tables available")) {
            localTableList.setSelectedIndex(0);
        }

        return new JScrollPane(localTableList);
    }

    private void updateTableView(String tableName) {
        tableManager.setTableName(tableName);
        refreshDataAndTabs();
    }

    private void initialize() {
        System.out.println("ViewSoftwareListTab: Initializing table");
        refreshDataAndTabs();
    }

    public void refreshDataAndTabs() {
        tableManager.refreshDataAndTabs();
    }

    @SuppressWarnings("unchecked")
    public void updateTables(String searchTerm) {
        refreshDataAndTabs();
        String text = searchTerm.toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        if (sorter != null) {
            javax.swing.RowFilter<DefaultTableModel, Integer> filter = null;
            if (!text.isEmpty()) {
                filter = new javax.swing.RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        for (int i = 1; i < entry.getModel().getColumnCount(); i++) {
                            Object value = entry.getValue(i);
                            if (value != null && value.toString().toLowerCase().contains(text)) {
                                return true;
                            }
                        }
                        return false;
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