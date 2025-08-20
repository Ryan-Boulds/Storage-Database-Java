package view_software_list_tab;

import java.awt.BorderLayout;
import java.awt.Font;
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

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import view_software_list_tab.view_software_details.DeviceDetailsPanel;

public class ViewSoftwareListTab extends JPanel {
    private final JTable table;
    protected final TableManager tableManager;
    private final JPanel mainPanel;
    private JComponent currentView;
    private JScrollPane tableListScrollPane;
    private JList<String> tableList;
    private final JSplitPane mainSplitPane;
    private ListSelectionListener originalTableListListener;
    private final JPanel leftPanel;
    private static final Logger LOGGER = Logger.getLogger(ViewSoftwareListTab.class.getName());

    public ViewSoftwareListTab() {
        setLayout(new BorderLayout());

        // Initialize main panel for filter and table
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

        // Initialize split pane for left (software list) and right (main panel)
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        mainSplitPane.setDividerLocation(200);
        mainSplitPane.setResizeWeight(0.3);

        // Set up left panel with software list and button
        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
        JPanel topLeftPanel = new JPanel();
        topLeftPanel.setLayout(new BoxLayout(topLeftPanel, BoxLayout.Y_AXIS));
        topLeftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel softwareListLabel = new JLabel("Inventory list:");
        JButton addNewTableButton = new JButton("Add New Table");
        topLeftPanel.add(softwareListLabel);
        topLeftPanel.add(addNewTableButton);
        leftPanel.add(topLeftPanel, BorderLayout.NORTH);
        tableListScrollPane = createTableListScrollPane();
        leftPanel.add(tableListScrollPane, BorderLayout.CENTER);

        // Set up filter panel after left panel to avoid leaking 'this'
        FilterPanel filterPanel = new FilterPanel(
                (search, status, dept) -> updateTables(search),
                this::refreshDataAndTabs,
                tableManager
        );

        // Set up main panel with filter at top and table below
        mainPanel.add(filterPanel.getPanel(), BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Set split pane components
        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(mainPanel);

        // Add action listener for new table button after filter panel initialization
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
                        try (PreparedStatement insertPs = conn.prepareStatement("INSERT INTO Settings (ID, SoftwareTables) VALUES (?, ?)")) {
                            insertPs.setInt(1, nextId);
                            insertPs.setString(2, newTableName);
                            insertPs.executeUpdate();
                        }
                        refreshTableList();
                        tableList.setSelectedValue(newTableName, true);
                        tableManager.setTableName(newTableName);
                        filterPanel.setTableName(newTableName);
                        refreshDataAndTabs();
                    } catch (SQLException ex) {
                        LOGGER.log(Level.SEVERE, "Error adding new table to Settings: {0}", ex.getMessage());
                        JOptionPane.showMessageDialog(this, "Error adding new table: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error creating new table '{0}': {1}", new Object[]{newTableName, ex.getMessage()});
                    JOptionPane.showMessageDialog(this, "Error creating new table: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        currentView = mainSplitPane;
        add(currentView, BorderLayout.CENTER);

        PopupHandler.addTablePopup(table, this);
    }

    private JScrollPane createTableListScrollPane() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        List<String> tables = getIncludedSoftwareTables();
        for (String table : tables) {
            if (!isExcludedTable(table)) {
                listModel.addElement(table);
            }
        }
        JList<String> softwareList = new JList<>(listModel);
        tableList = softwareList;
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableList.setFixedCellWidth(180);
        tableList.setFixedCellHeight(25);
        tableList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        originalTableListListener = e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = tableList.getSelectedValue();
                tableManager.setTableName(selectedTable);
                refreshDataAndTabs();
            }
        };
        tableList.addListSelectionListener(originalTableListListener);
        tableListScrollPane = new JScrollPane(softwareList);
        return tableListScrollPane;
    }

    private void refreshTableList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        List<String> tables = getIncludedSoftwareTables();
        for (String table : tables) {
            if (!isExcludedTable(table)) {
                listModel.addElement(table);
            }
        }
        tableList.setModel(listModel);
        tableList.revalidate();
        tableList.repaint();
    }

    private boolean isExcludedTable(String tableName) {
        return tableName == null || tableName.equalsIgnoreCase("Settings") ||
               tableName.equalsIgnoreCase("LicenseKeyRules") ||
               tableName.equalsIgnoreCase("Templates");
    }

    private List<String> getIncludedSoftwareTables() {
        List<String> tables = new ArrayList<>();
        try (Connection conn = DatabaseUtils.getConnection()) {
            if (!tableExists("Settings", conn)) {
                createSettingsTable(conn);
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DISTINCT SoftwareTables FROM Settings WHERE SoftwareTables IS NOT NULL")) {
                while (rs.next()) {
                    String tableName = rs.getString("SoftwareTables");
                    if (tableName != null && !tableName.trim().isEmpty()) {
                        tables.add(tableName);
                    }
                }
            }
            LOGGER.log(Level.INFO, "getIncludedSoftwareTables: Fetched tables: {0}", tables);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching included software tables: {0}", e.getMessage());
        }
        System.out.println("ViewSoftwareListTab: Fetched tables: " + tables);
        return tables;
    }

    private boolean tableExists(String tableName, Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void createSettingsTable(Connection conn) throws SQLException {
        String createSql = "CREATE TABLE Settings (ID INTEGER PRIMARY KEY, SoftwareTables VARCHAR(255))";
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
        currentView = mainSplitPane;
        add(currentView, BorderLayout.CENTER);

        for (ListSelectionListener listener : tableList.getListSelectionListeners()) {
            tableList.removeListSelectionListener(listener);
        }
        tableList.addListSelectionListener(originalTableListListener);

        String currentTable = tableManager.getTableName();
        if (currentTable != null) {
            tableList.setSelectedValue(currentTable, true);
            tableManager.setTableName(currentTable);
            refreshDataAndTabs();
        } else {
            tableList.clearSelection();
            tableManager.setTableName(null);
            refreshDataAndTabs();
        }

        revalidate();
        repaint();
    }

    public TableManager getTableManager() {
        return tableManager;
    }
}