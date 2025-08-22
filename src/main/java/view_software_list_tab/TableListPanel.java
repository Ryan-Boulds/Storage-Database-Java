package view_software_list_tab;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList; // Correct import
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import utils.DatabaseUtils;

public class TableListPanel extends JPanel {
    private final JList<String> tableList;
    private final JScrollPane tableListScrollPane;
    private static final Logger LOGGER = Logger.getLogger(TableListPanel.class.getName());
    private final ViewSoftwareListTab viewSoftwareListTab;

    public TableListPanel(ViewSoftwareListTab viewSoftwareListTab) {
        this.viewSoftwareListTab = viewSoftwareListTab;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));

        // Initialize top panel with label and button
        JPanel topLeftPanel = new JPanel();
        topLeftPanel.setLayout(new BoxLayout(topLeftPanel, BoxLayout.Y_AXIS));
        topLeftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel softwareListLabel = new JLabel("Software List:");
        JButton addNewTableButton = new JButton("Add New Table");
        topLeftPanel.add(softwareListLabel);
        topLeftPanel.add(addNewTableButton);
        add(topLeftPanel, BorderLayout.NORTH);

        // Initialize table list and scroll pane
        tableList = new JList<>(new DefaultListModel<>());
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableListScrollPane = new JScrollPane(tableList);
        add(tableListScrollPane, BorderLayout.CENTER);

        // Add action listener for Add New Table button
        addNewTableButton.addActionListener(e -> addNewTable());

        // Initialize table list
        updateTableList();

        // Add selection listener
        tableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = tableList.getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    viewSoftwareListTab.getTableManager().setTableName(selectedTable);
                    if (viewSoftwareListTab.getImportDataTab() != null && viewSoftwareListTab.getImportDataTab().getTableSelector() != null) {
                        viewSoftwareListTab.getImportDataTab().getTableSelector().setSelectedItem(selectedTable);
                    }
                    viewSoftwareListTab.refreshDataAndTabs();
                }
            }
        });
    }

    public final void updateTableList() { // Marked as final to address hint
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
        System.out.println("TableListPanel: Fetched tables: " + tables);
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
            viewSoftwareListTab.getTableManager().setTableName(newTableName);
            viewSoftwareListTab.getImportDataTab().updateTableSelectorOptions();
            if (viewSoftwareListTab.getImportDataTab().getTableSelector() != null) {
                viewSoftwareListTab.getImportDataTab().getTableSelector().setSelectedItem(newTableName);
            }
            viewSoftwareListTab.refreshDataAndTabs();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error creating table: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "SQLException creating table '{0}': {1}", new Object[]{newTableName, e.getMessage()});
        }
    }

    public JList<String> getTableList() {
        return tableList;
    }
}