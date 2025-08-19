package view_inventory_tab;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import utils.DatabaseUtils;
import view_inventory_tab.view_software_details.DeviceDetailsPanel;

public class ViewInventoryTab extends JPanel {
    private final JTable table;
    protected final TableManager tableManager;
    private final JPanel mainPanel;
    private JPanel currentView;
    private JScrollPane tableListScrollPane;
    private JList<String> tableList;
    private final JSplitPane mainSplitPane;
    private ListSelectionListener originalTableListListener;
    private final JPanel leftPanel;
    private static final Logger LOGGER = Logger.getLogger(ViewInventoryTab.class.getName());

    public ViewInventoryTab() {
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
                this::refreshDataAndTabs,
                tableManager
        );

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        mainSplitPane.setDividerLocation(200);

        tableListScrollPane = createTableListScrollPane();
        leftPanel = new JPanel(new BorderLayout());
        JPanel topLeftPanel = new JPanel();
        topLeftPanel.setLayout(new BoxLayout(topLeftPanel, BoxLayout.Y_AXIS));
        JLabel softwareListLabel = new JLabel("Software list:");
        JButton addNewTableButton = new JButton("Add New Table");
        addNewTableButton.addActionListener(e -> {
            String inputTableName = JOptionPane.showInputDialog(this, "Enter new table name:");
            if (inputTableName != null && !inputTableName.trim().isEmpty()) {
                final String newTableName = inputTableName.trim();
                if (newTableName.matches("\\d+")) {
                    JOptionPane.showMessageDialog(this, "Error: Table name cannot be purely numeric", "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.log(Level.SEVERE, "Attempted to create numeric table name '{0}'", newTableName);
                    return;
                }
                try (Connection conn = DatabaseUtils.getConnection()) {
                    List<String> existingTables = DatabaseUtils.getTableNames();
                    if (existingTables.stream().anyMatch(t -> t.equalsIgnoreCase(newTableName))) {
                        JOptionPane.showMessageDialog(this, "Error: Table '" + newTableName + "' already exists", "Error", JOptionPane.ERROR_MESSAGE);
                        LOGGER.log(Level.SEVERE, "Attempted to create existing table '{0}'", newTableName);
                        return;
                    }
                    String sql = "CREATE TABLE [" + newTableName + "] ([AssetName] VARCHAR(255) PRIMARY KEY)";
                    conn.createStatement().executeUpdate(sql);
                    try {
                        if (!tableExists("Settings", conn)) {
                            createSettingsTable(conn);
                        }
                        int nextId = getNextSettingsId(conn);
                        try (PreparedStatement insertPs = conn.prepareStatement("INSERT INTO Settings (ID, InventoryTables) VALUES (?, ?)")) {
                            insertPs.setInt(1, nextId);
                            insertPs.setString(2, newTableName);
                            insertPs.executeUpdate();
                        }
                        tableListScrollPane.setViewportView(createTableListScrollPane().getViewport());
                        tableManager.setTableName(newTableName);
                        filterPanel.setTableName(newTableName);
                        refreshDataAndTabs();
                        LOGGER.log(Level.INFO, "Created new table '{0}' and updated UI", newTableName);
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Error creating table: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        LOGGER.log(Level.SEVERE, "SQLException creating table '{0}': {1}", new Object[]{newTableName, ex.getMessage()});
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error creating table: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.log(Level.SEVERE, "SQLException creating table '{0}': {1}", new Object[]{newTableName, ex.getMessage()});
                }
            }
        });
        topLeftPanel.add(softwareListLabel);
        topLeftPanel.add(addNewTableButton);
        leftPanel.add(topLeftPanel, BorderLayout.NORTH);
        leftPanel.add(tableListScrollPane, BorderLayout.CENTER);
        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(mainPanel);
        mainPanel.add(filterPanel.getPanel(), BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        currentView = mainPanel;
        add(mainSplitPane, BorderLayout.CENTER);
        PopupHandler.addTablePopup(table, this);
    }

    private JScrollPane createTableListScrollPane() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String tableName : getIncludedInventoryTables()) {
            if (!isExcludedTable(tableName)) {
                listModel.addElement(tableName);
            }
        }
        tableList = new JList<>(listModel);
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        originalTableListListener = e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = tableList.getSelectedValue();
                if (selectedTable != null) {
                    tableManager.setTableName(selectedTable);
                    refreshDataAndTabs();
                }
            }
        };
        tableList.addListSelectionListener(originalTableListListener);
        return new JScrollPane(tableList);
    }

    private boolean isExcludedTable(String tableName) {
        return tableName.equalsIgnoreCase("Settings") ||
               tableName.equalsIgnoreCase("LicenseKeyRules") ||
               tableName.equalsIgnoreCase("Templates");
    }

    private List<String> getIncludedInventoryTables() {
        List<String> tables = new ArrayList<>();
        try (Connection conn = DatabaseUtils.getConnection()) {
            if (!tableExists("Settings", conn)) {
                createSettingsTable(conn);
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DISTINCT InventoryTables FROM Settings WHERE InventoryTables IS NOT NULL")) {
                while (rs.next()) {
                    String tableName = rs.getString("InventoryTables");
                    if (tableName != null && !tableName.trim().isEmpty()) {
                        tables.add(tableName);
                    }
                }
            }
            LOGGER.log(Level.INFO, "getIncludedInventoryTables: Fetched tables: {0}", tables);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching included inventory tables: {0}", e.getMessage());
        }
        System.out.println("ViewInventoryTab: Fetched tables: " + tables);
        return tables;
    }

    private boolean tableExists(String tableName, Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void createSettingsTable(Connection conn) throws SQLException {
        String createSql = "CREATE TABLE Settings (ID INTEGER PRIMARY KEY, InventoryTables VARCHAR(255))";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createSql);
        }
    }

    private int getNextSettingsId(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(ID) FROM Settings")) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
            return 1;
        }
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
        mainSplitPane.setLeftComponent(leftPanel);

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

    public TableManager getTableManager() {
        return tableManager;
    }
}