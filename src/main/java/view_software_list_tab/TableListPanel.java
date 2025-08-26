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
    private final DefaultListModel<String> tableListModel;
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
        tableListModel = new DefaultListModel<>();
        tableList = new JList<>(tableListModel);
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableListScrollPane = new JScrollPane(tableList);
        add(tableListScrollPane, BorderLayout.CENTER);

        // Add action listener for Add New Table button
        addNewTableButton.addActionListener(e -> addNewTable());

        // Ensure TableInformation table schema is up-to-date
        ensureTableInformationSchema();

        // Initialize table list
        updateTableList();
    }

    public final void updateTableList() {
        tableListModel.clear();
        try (Connection conn = DatabaseUtils.getConnection()) {
            List<String> tableNames = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT SoftwareTables FROM TableInformation")) {
                while (rs.next()) {
                    String tableName = rs.getString("SoftwareTables");
                    if (tableName != null && !tableName.trim().isEmpty()) {
                        tableNames.add(tableName);
                    }
                }
            }
            if (tableNames.isEmpty()) {
                tableListModel.addElement("No tables available");
            } else {
                tableNames.forEach(tableListModel::addElement);
            }
            tableList.clearSelection(); // Ensure no table is selected initially
            LOGGER.log(Level.INFO, "Updated table list with {0} tables", tableNames.size());
        } catch (SQLException e) {
            tableListModel.addElement("Error retrieving tables");
            LOGGER.log(Level.SEVERE, "Error updating table list: {0}", e.getMessage());
        }
    }

    private void ensureTableInformationSchema() {
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "TableInformation", null)) {
                if (!rs.next()) {
                    // Create table if it doesn't exist
                    try (Statement stmt = conn.createStatement()) {
                        String sql = "CREATE TABLE TableInformation (ID INTEGER PRIMARY KEY, SoftwareTables VARCHAR(255), RequiresLicenseKey BOOLEAN)";
                        stmt.executeUpdate(sql);
                        LOGGER.log(Level.INFO, "Created TableInformation table with BOOLEAN RequiresLicenseKey");
                    }
                } else {
                    // Check if RequiresLicenseKey column exists
                    try (ResultSet columns = meta.getColumns(null, null, "TableInformation", "RequiresLicenseKey")) {
                        if (!columns.next()) {
                            // Add RequiresLicenseKey column if missing
                            try (Statement stmt = conn.createStatement()) {
                                String sql = "ALTER TABLE TableInformation ADD RequiresLicenseKey BOOLEAN";
                                stmt.executeUpdate(sql);
                                LOGGER.log(Level.INFO, "Added RequiresLicenseKey BOOLEAN column to TableInformation");
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error ensuring TableInformation table schema: {0}", e.getMessage());
        }
    }

    private boolean tableExists(String tableName, Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private int getNextSettingsId(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(ID) FROM TableInformation")) {
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

            // Create table with default columns
            String createSql = "CREATE TABLE [" + newTableName + "] (AssetName VARCHAR(255) PRIMARY KEY, Status VARCHAR(50))";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createSql);
                LOGGER.log(Level.INFO, "Created new table '{0}'", newTableName);
            }

            // Add to TableInformation
            int nextId = getNextSettingsId(conn);
            String insertSql = "INSERT INTO TableInformation (ID, SoftwareTables, RequiresLicenseKey) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, nextId);
                ps.setString(2, newTableName);
                ps.setBoolean(3, true); // Use true/false for BOOLEAN type
                ps.executeUpdate();
                LOGGER.log(Level.INFO, "Added table '{0}' to TableInformation with ID {1}", new Object[]{newTableName, nextId});
            }

            updateTableList();
            viewSoftwareListTab.getTableManager().setTableName(newTableName);
            viewSoftwareListTab.refreshDataAndTabs();
            if (viewSoftwareListTab.getImportDataTab().getTableList() != null) {
                viewSoftwareListTab.getImportDataTab().getTableList().setSelectedValue(newTableName, true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error creating table: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "SQLException creating table '{0}': {1}", new Object[]{newTableName, e.getMessage()});
        }
    }

    public JList<String> getTableList() {
        return tableList;
    }
}