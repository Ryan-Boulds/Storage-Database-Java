package view_software_list_tab;

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
import view_software_list_tab.license_key_tracker.LicenseKeyTracker;
import view_software_list_tab.view_software_details.DeviceDetailsPanel;

public class ViewSoftwareListTab extends JPanel {
    private final JTable table;
    protected final TableManager tableManager;
    private final JPanel mainPanel;
    private JComponent currentView;
    private final JScrollPane tableListScrollPane;
    private final JList<String> tableList;
    private final JSplitPane mainSplitPane;
    private final ListSelectionListener originalTableListListener;
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

        // Initialize table list and scroll pane
        tableList = new JList<>(new DefaultListModel<>());
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableListScrollPane = new JScrollPane(tableList);

        // Initialize split pane for left (software list) and right (main panel)
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        mainSplitPane.setDividerLocation(200);
        mainSplitPane.setResizeWeight(0.3);

        // Set up left panel with software list and buttons
        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
        JPanel topLeftPanel = new JPanel();
        topLeftPanel.setLayout(new BoxLayout(topLeftPanel, BoxLayout.Y_AXIS));
        topLeftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel softwareListLabel = new JLabel("software list:");
        JButton addNewTableButton = new JButton("Add New Table");
        topLeftPanel.add(softwareListLabel);
        topLeftPanel.add(addNewTableButton);
        leftPanel.add(topLeftPanel, BorderLayout.NORTH);
        leftPanel.add(tableListScrollPane, BorderLayout.CENTER);

        // Add action listener for Add New Table button
        addNewTableButton.addActionListener(e -> addNewTable());

        JButton licenseKeyTrackerButton = new JButton("License Key Tracker");

        // Add action listener for License Key Tracker button
        licenseKeyTrackerButton.addActionListener(e -> showLicenseKeyTracker());

        // Set up main panel with table
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(mainPanel);
        currentView = mainSplitPane;
        add(currentView, BorderLayout.CENTER);

        // Initialize table list
        updateTableList();

        // Set up filter panel after all fields are initialized to avoid leaking 'this'
        FilterPanel filterPanel = new FilterPanel(
                (search, status, dept) -> updateTables(search),
                this::refreshDataAndTabs,
                tableManager
        );
        filterPanel.getPanel().add(licenseKeyTrackerButton, 0);
        mainPanel.add(filterPanel.getPanel(), BorderLayout.NORTH);

        // Initialize listeners
        originalTableListListener = e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = tableList.getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    tableManager.setTableName(selectedTable);
                    refreshDataAndTabs();
                }
            }
        };
        tableList.addListSelectionListener(originalTableListListener);

        // Attach popup menu handler
        PopupHandler.addTablePopup(table, this);
    }

    private void updateTableList() {
        DefaultListModel<String> model = (DefaultListModel<String>) tableList.getModel();
        model.clear();
        List<String> tables = getIncludedSoftwareTables();
        if (tables.isEmpty()) {
            model.addElement("No tables available");
        } else {
            for (String table : tables) {
                model.addElement(table);
            }
        }
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
        String searchText = searchTerm.toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        if (sorter != null) {
            javax.swing.RowFilter<DefaultTableModel, Integer> filter = null;
            if (!searchText.isEmpty()) {
                filter = new javax.swing.RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        for (int i = 1; i < entry.getModel().getColumnCount(); i++) {
                            Object value = entry.getValue(i);
                            if (value != null && value.toString().toLowerCase().contains(searchText)) {
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

    private void showLicenseKeyTracker() {
        String tableName = tableManager.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a valid table first", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.WARNING, "Attempted to open LicenseKeyTracker without selecting a valid table");
            return;
        }
        remove(currentView);
        currentView = new LicenseKeyTracker(this, tableManager);
        add(currentView, BorderLayout.CENTER);
        revalidate();
        repaint();
        LOGGER.log(Level.INFO, "Opened LicenseKeyTracker for table '{0}'", tableName);
    }

    private void addNewTable() {
        String newTableName = JOptionPane.showInputDialog(this, "Enter new table name:");
        if (newTableName == null || newTableName.trim().isEmpty()) {
            return;
        }
        newTableName = newTableName.trim();

        try (Connection conn = DatabaseUtils.getConnection()) {
            if (tableExists(newTableName, conn)) {
                JOptionPane.showMessageDialog(this, "Table already exists", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, "Attempted to create existing table '{0}'", newTableName);
                return;
            }

            // Create table
            String createSql = "CREATE TABLE [" + newTableName + "] (AssetName VARCHAR(255) PRIMARY KEY)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createSql);
                LOGGER.log(Level.INFO, "Created new table '{0}'", newTableName);
            }

            // Add to Settings
            int nextId = getNextSettingsId(conn);
            String insertSql = "INSERT INTO Settings (ID, SoftwareTables) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, nextId);
                ps.setString(2, newTableName);
                ps.executeUpdate();
                LOGGER.log(Level.INFO, "Added table '{0}' to Settings with ID {1}", new Object[]{newTableName, nextId});
            }

            updateTableList();
            tableList.setSelectedValue(newTableName, true);
            tableManager.setTableName(newTableName);
            refreshDataAndTabs();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error creating table: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "SQLException creating table '{0}': {1}", new Object[]{newTableName, e.getMessage()});
        }
    }

    public TableManager getTableManager() {
        return tableManager;
    }

    public JComponent getCurrentView() {
        return currentView;
    }

    public void setCurrentView(JComponent currentView) {
        this.currentView = currentView;
    }
}