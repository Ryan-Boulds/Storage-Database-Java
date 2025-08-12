package view_software_list_tab;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import utils.DatabaseUtils;
import utils.TablesNotIncludedList;
import view_software_list_tab.license_key_tracker.LicenseKeyTracker;
import view_software_list_tab.view_software_details.DeviceDetailsPanel;

@SuppressWarnings("this-escape")
public class ViewSoftwareListTab extends JPanel {
    private final JTable table;
    private final TableManager tableManager;
    private final JPanel mainPanel;
    private JPanel currentView;
    private JScrollPane tableListScrollPane;
    private JList<String> tableList;
    private JSplitPane mainSplitPane; // Store the main split pane

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

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        mainSplitPane.setDividerLocation(200);

        tableListScrollPane = createTableListScrollPane();
        mainSplitPane.setLeftComponent(tableListScrollPane);
        @SuppressWarnings("unchecked")
        JList<String> newTableList = (JList<String>) tableListScrollPane.getViewport().getView();
        tableList = newTableList;

        JPanel tablePanel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        JButton licenseKeyButton = new JButton("License Key Tracker");
        licenseKeyButton.addActionListener(e -> showLicenseKeyTracker());
        buttonPanel.add(licenseKeyButton);

        JButton addColumnButton = new JButton("Add Column");
        addColumnButton.addActionListener(e -> {
            String tableName = tableManager.getTableName();
            if ("Inventory".equals(tableName)) {
                JOptionPane.showMessageDialog(this, "Error: Adding columns is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("ViewSoftwareListTab: Attempted to add column in Inventory table, which is not allowed");
                return;
            }

            String newColumnName = JOptionPane.showInputDialog(this, "Enter new column name:");
            if (newColumnName != null && !newColumnName.trim().isEmpty()) {
                newColumnName = newColumnName.trim();
                String[] columns = tableManager.getColumns();
                for (String column : columns) {
                    if (column.equalsIgnoreCase(newColumnName)) {
                        JOptionPane.showMessageDialog(this, "Error: Column '" + newColumnName + "' already exists (case-insensitive)", "Error", JOptionPane.ERROR_MESSAGE);
                        System.err.println("ViewSoftwareListTab: Attempted to add existing column '" + newColumnName + "' to table '" + tableName + "'");
                        return;
                    }
                }
                try (Connection conn = DatabaseUtils.getConnection()) {
                    String sql = "ALTER TABLE " + tableName + " ADD " + newColumnName + " VARCHAR(255)";
                    conn.createStatement().executeUpdate(sql);
                    JOptionPane.showMessageDialog(this, "Column added successfully");
                    SwingUtilities.invokeLater(() -> {
                        tableManager.setTableName(tableName);
                        tableManager.refreshDataAndTabs();
                    });
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error adding column: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("ViewSoftwareListTab: SQLException adding column to table '" + tableName + "': " + ex.getMessage());
                }
            }
        });
        buttonPanel.add(addColumnButton);

        JButton deleteColumnButton = new JButton("Delete Column");
        deleteColumnButton.addActionListener(e -> {
            String tableName = tableManager.getTableName();
            if ("Inventory".equals(tableName)) {
                JOptionPane.showMessageDialog(this, "Error: Deleting columns is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("ViewSoftwareListTab: Attempted to delete column in Inventory table, which is not allowed");
                return;
            }

            String[] columnNames = tableManager.getColumns();
            String columnToDelete = (String) JOptionPane.showInputDialog(
                this,
                "Select column to delete:",
                "Delete Column",
                JOptionPane.PLAIN_MESSAGE,
                null,
                columnNames,
                columnNames[0]
            );
            if (columnToDelete != null && !columnToDelete.equals("AssetName")) {
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete the column '" + columnToDelete + "'? This will remove all data in this column.",
                    "Confirm Delete Column",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    try (Connection conn = DatabaseUtils.getConnection()) {
                        String sql = "ALTER TABLE " + tableName + " DROP COLUMN " + columnToDelete;
                        conn.createStatement().executeUpdate(sql);
                        JOptionPane.showMessageDialog(this, "Column '" + columnToDelete + "' deleted successfully");
                        SwingUtilities.invokeLater(() -> {
                            tableManager.setTableName(tableName);
                            tableManager.refreshDataAndTabs();
                        });
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Error deleting column: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        System.err.println("ViewSoftwareListTab: SQLException deleting column in table '" + tableName + "': " + ex.getMessage());
                    }
                }
            } else if (columnToDelete != null && columnToDelete.equals("AssetName")) {
                JOptionPane.showMessageDialog(this, "Error: Cannot delete the primary key column 'AssetName'", "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("ViewSoftwareListTab: Attempted to delete primary key column 'AssetName' in table '" + tableName + "'");
            }
        });
        buttonPanel.add(deleteColumnButton);

        topPanel.add(buttonPanel, BorderLayout.WEST);
        topPanel.add(filterPanel.getPanel(), BorderLayout.CENTER);
        tablePanel.add(topPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        mainSplitPane.setRightComponent(tablePanel);

        mainPanel.add(mainSplitPane, BorderLayout.CENTER);
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

    public void showLicenseKeyTracker() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: License Key Tracker is not available for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("ViewSoftwareListTab: Attempted to access License Key Tracker for Inventory table");
            return;
        }

        // Create a new split pane for the LicenseKeyTracker view
        JSplitPane trackerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        trackerSplitPane.setDividerLocation(200);
        trackerSplitPane.setLeftComponent(tableListScrollPane);
        LicenseKeyTracker tracker = new LicenseKeyTracker(this, tableManager);
        trackerSplitPane.setRightComponent(tracker);

        remove(currentView);
        currentView = new JPanel(new BorderLayout());
        currentView.add(trackerSplitPane, BorderLayout.CENTER);
        add(currentView, BorderLayout.CENTER);

        // Update table list selection listener to call updateTable on LicenseKeyTracker
        tableList.clearSelection();
        tableList.setSelectedValue(tableName, true);
        // Remove existing listeners to avoid duplicates
        for (ListSelectionListener listener : tableList.getListSelectionListeners()) {
            tableList.removeListSelectionListener(listener);
        }
        tableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = tableList.getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    tracker.updateTable(selectedTable);
                }
            }
        });

        revalidate();
        repaint();
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

        JList<String> newTableList = new JList<>(listModel);
        newTableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        newTableList.setFixedCellWidth(180);
        newTableList.setFixedCellHeight(25);

        newTableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = newTableList.getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    updateTableView(selectedTable);
                }
            }
        });

        if (!listModel.isEmpty() && !listModel.getElementAt(0).startsWith("Error") && !listModel.getElementAt(0).equals("No tables available")) {
            newTableList.setSelectedIndex(0);
            // Explicitly set the initial table in TableManager
            String initialTable = newTableList.getSelectedValue();
            if (initialTable != null) {
                tableManager.setTableName(initialTable);
                tableManager.refreshDataAndTabs();
            }
        }

        JScrollPane scrollPane = new JScrollPane(newTableList);
        tableListScrollPane = scrollPane;
        tableList = newTableList;
        return scrollPane;
    }

    private void updateTableView(String tableName) {
        tableManager.setTableName(tableName);
        refreshDataAndTabs();
    }

    private void initialize() {
        System.out.println("ViewSoftwareListTab: Initializing table");
        // Moved table initialization to createTableListScrollPane to ensure selection triggers data load
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
        // Ensure the table list is visible
        mainSplitPane.setLeftComponent(tableListScrollPane);
        // Reselect the current table to trigger data refresh
        String currentTable = tableManager.getTableName();
        if (currentTable != null) {
            tableList.clearSelection();
            tableList.setSelectedValue(currentTable, true);
        }
        refreshDataAndTabs();
        revalidate();
        repaint();
    }
}