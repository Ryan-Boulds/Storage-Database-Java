package view_software_list_tab;

import java.awt.BorderLayout;
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
    private JSplitPane mainSplitPane;
    private ListSelectionListener originalTableListListener;

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
        JPanel topPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        JButton licenseKeyButton = new JButton("License Key Tracker");
        licenseKeyButton.addActionListener(e -> showLicenseKeyTracker());

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
            String[] ANSWERS = { "Yes", "No" };
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
                int confirm = JOptionPane.showOptionDialog(
                    this,
                    "Are you sure you want to delete the column '" + columnToDelete + "'? This will remove all data in this column.",
                    "Confirm Delete Column",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    ANSWERS,
                    ANSWERS[1]
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
            } else if (columnToDelete != null) {
                JOptionPane.showMessageDialog(this, "Cannot delete primary key column 'AssetName'", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(deleteColumnButton);

        JButton addRowButton = new JButton("Add New Entry");
        addRowButton.addActionListener(e -> {
            String tableName = tableManager.getTableName();
            if ("Inventory".equals(tableName)) {
                JOptionPane.showMessageDialog(this, "Error: Adding rows is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("ViewSoftwareListTab: Attempted to add row in Inventory table, which is not allowed");
                return;
            }
            AddRowEntry.showAddDialog((javax.swing.JFrame) SwingUtilities.getWindowAncestor(this), tableManager);
        });
        buttonPanel.add(addRowButton);

        topPanel.add(licenseKeyButton);
        topPanel.add(filterPanel.getPanel());
        topPanel.add(buttonPanel);
        tablePanel.add(topPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        mainSplitPane.setRightComponent(tablePanel);
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);
        currentView = mainPanel;
        add(currentView, BorderLayout.CENTER);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int row = table.rowAtPoint(e.getPoint());
                    int column = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && column > 0) {
                        int primaryKeyColumnIndex = -1;
                        for (int i = 0; i < table.getColumnCount(); i++) {
                            if ("AssetName".equals(table.getColumnName(i))) {
                                primaryKeyColumnIndex = i;
                                break;
                            }
                        }
                        if (primaryKeyColumnIndex != -1) {
                            String assetName = (String) table.getValueAt(row, primaryKeyColumnIndex);
                            if (assetName != null && !assetName.trim().isEmpty()) {
                                showDeviceDetails(assetName);
                            }
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
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: License Key Tracker is not available for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("ViewSoftwareListTab: Attempted to access License Key Tracker for Inventory table");
            return;
        }

        JSplitPane trackerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        trackerSplitPane.setDividerLocation(200);
        trackerSplitPane.setLeftComponent(tableListScrollPane);
        LicenseKeyTracker tracker = new LicenseKeyTracker(this, tableManager);
        trackerSplitPane.setRightComponent(tracker);

        remove(currentView);
        currentView = new JPanel(new BorderLayout());
        currentView.add(trackerSplitPane, BorderLayout.CENTER);
        add(currentView, BorderLayout.CENTER);

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

        tableList.clearSelection();
        tableList.setSelectedValue(tableName, true);

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

        JList<String> localTableList = new JList<>(listModel);
        localTableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        localTableList.setFixedCellWidth(180);
        localTableList.setFixedCellHeight(25);

        originalTableListListener = e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = localTableList.getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    updateTableView(selectedTable);
                }
            }
        };
        localTableList.addListSelectionListener(originalTableListListener);

        if (!listModel.isEmpty() && !listModel.getElementAt(0).startsWith("Error") && !listModel.getElementAt(0).equals("No tables available")) {
            localTableList.setSelectedIndex(0);
            String initialTable = localTableList.getSelectedValue();
            if (initialTable != null) {
                tableManager.setTableName(initialTable);
                tableManager.refreshDataAndTabs();
            }
        }

        JScrollPane scrollPane = new JScrollPane(localTableList);
        tableListScrollPane = scrollPane;
        tableList = localTableList;
        return scrollPane;
    }

    private void updateTableView(String tableName) {
        tableManager.setTableName(tableName);
        refreshDataAndTabs();
    }

    private void initialize() {
        System.out.println("ViewSoftwareListTab: Initializing table");
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
        mainSplitPane.setLeftComponent(tableListScrollPane);

        for (ListSelectionListener listener : tableList.getListSelectionListeners()) {
            tableList.removeListSelectionListener(listener);
        }
        tableList.addListSelectionListener(originalTableListListener);

        String currentTable = tableManager.getTableName();
        if (currentTable != null) {
            tableList.clearSelection();
            tableList.setSelectedValue(currentTable, true);
            tableManager.setTableName(currentTable);
            refreshDataAndTabs();
        }

        revalidate();
        repaint();
    }
}