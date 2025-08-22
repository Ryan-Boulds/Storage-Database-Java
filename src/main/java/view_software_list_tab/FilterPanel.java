package view_software_list_tab;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import utils.DatabaseUtils;
import utils.UIComponentUtils;
import view_software_list_tab.Add_And_Edit_Entries.AddRowEntry;

public class FilterPanel {

    final JPanel filterPanel;
    private final JTextField searchField;
    private final JComboBox<String> statusFilter;
    private final JComboBox<String> deptFilter;
    private boolean hasDepartmentColumn;
    private final JLabel searchLabel;
    private final JLabel statusLabel;
    private final JLabel deptLabel;
    private final JButton refreshButton;
    private final JButton addRowButton;
    private final JButton addColumnButton;
    private final JButton deleteColumnButton;
    private final TriConsumer<String, String, String> filterAction;
    private final TableManager tableManager;
    private final TableListPanel tableListPanel; // Added to access tableListPanel directly
    private static final Logger LOGGER = Logger.getLogger(FilterPanel.class.getName());

    public FilterPanel(TriConsumer<String, String, String> filterAction, Runnable refreshAction, TableManager tableManager, TableListPanel tableListPanel) {
        this.filterAction = filterAction;
        this.tableManager = tableManager;
        this.tableListPanel = tableListPanel;
        filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchLabel = UIComponentUtils.createAlignedLabel("Search:");
        searchField = UIComponentUtils.createFormattedTextField();
        searchField.setPreferredSize(new Dimension(200, 30));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        statusLabel = UIComponentUtils.createAlignedLabel("Status:");
        statusFilter = UIComponentUtils.createFormattedComboBox(new String[]{"All", "Deployed", "In Storage", "Needs Repair"});
        statusFilter.setPreferredSize(new Dimension(100, 30));
        statusFilter.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyFilter();
            }
        });

        deptLabel = UIComponentUtils.createAlignedLabel("Department:");
        deptFilter = UIComponentUtils.createFormattedComboBox(new String[]{"All"});
        deptFilter.setPreferredSize(new Dimension(100, 30));
        deptFilter.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyFilter();
            }
        });

        refreshButton = UIComponentUtils.createFormattedButton("Refresh");
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            deptFilter.setSelectedIndex(0);
            String tableName = tableManager.getTableName();
            if (tableName == null || tableName.isEmpty()) {
                DefaultListModel<String> model = (DefaultListModel<String>) tableListPanel.getTableList().getModel();
                if (!model.isEmpty() && !model.getElementAt(0).equals("No tables available")) {
                    tableName = model.getElementAt(0);
                    tableListPanel.getTableList().setSelectedValue(tableName, true);
                    tableManager.setTableName(tableName);
                    tableManager.initializeColumns();
                    LOGGER.log(Level.INFO, "Refresh: Selected first table '{0}'", tableName);
                }
            } else {
                tableManager.setTableName(tableName);
                tableManager.initializeColumns();
            }
            refreshAction.run();
        });

        addRowButton = UIComponentUtils.createFormattedButton("Add Row/Entry");
        addRowButton.addActionListener(e -> {
            String tableName = tableManager.getTableName();
            if (tableName != null && !tableName.isEmpty()) {
                AddRowEntry.showAddDialog((javax.swing.JFrame) SwingUtilities.getWindowAncestor(filterPanel), tableManager);
                LOGGER.log(Level.INFO, "Opened AddRowEntry dialog for table '{0}'", tableName);
            } else {
                JOptionPane.showMessageDialog(filterPanel, "Please select a valid table first", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, "Attempted to add row without selecting a valid table");
            }
        });

        addColumnButton = UIComponentUtils.createFormattedButton("Add Column");
        addColumnButton.addActionListener(e -> addColumnAction());

        deleteColumnButton = UIComponentUtils.createFormattedButton("Delete Column");
        deleteColumnButton.addActionListener(e -> deleteColumnAction());

        filterPanel.add(searchLabel);
        filterPanel.add(searchField);
        filterPanel.add(statusLabel);
        filterPanel.add(statusFilter);
        filterPanel.add(deptLabel);
        filterPanel.add(deptFilter);
        filterPanel.add(refreshButton);
        filterPanel.add(addRowButton);
        filterPanel.add(addColumnButton);
        filterPanel.add(deleteColumnButton);

        initializeFilters();
    }

    private void initializeFilters() {
        try (Connection conn = DatabaseUtils.getConnection()) {
            if (conn == null) {
                LOGGER.log(Level.SEVERE, "Failed to establish database connection for initializing filters");
                return;
            }
            String tableName = tableManager.getTableName();
            if (tableName == null) {
                LOGGER.log(Level.WARNING, "No table selected for initializing filters");
                return;
            }
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM [" + tableName + "] WHERE 1=0")) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                hasDepartmentColumn = false;

                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    columns.add(columnName);
                    if (columnName.equalsIgnoreCase("Department")) {
                        hasDepartmentColumn = true;
                    }
                }
                LOGGER.log(Level.INFO, "Table '{0}' columns: {1}", new Object[]{tableName, String.join(", ", columns)});

                if (hasDepartmentColumn) {
                    try (ResultSet deptRs = stmt.executeQuery("SELECT DISTINCT Department FROM [" + tableName + "] WHERE Department IS NOT NULL")) {
                        List<String> departments = new ArrayList<>();
                        departments.add("All");
                        while (deptRs.next()) {
                            String dept = deptRs.getString("Department");
                            if (dept != null && !dept.trim().isEmpty()) {
                                departments.add(dept);
                            }
                        }
                        deptFilter.setModel(new DefaultComboBoxModel<>(departments.toArray(new String[0])));
                        LOGGER.log(Level.INFO, "Loaded {0} departments for table '{1}'", new Object[]{departments.size() - 1, tableName});
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initializing filters for table '{0}': {1}", new Object[]{tableManager.getTableName(), e.getMessage()});
        }
    }

    private void applyFilter() {
        String searchText = searchField.getText();
        String statusText = (String) statusFilter.getSelectedItem();
        String deptText = (String) deptFilter.getSelectedItem();
        filterAction.accept(searchText, statusText, deptText);
    }

    public JPanel getPanel() {
        return filterPanel;
    }

    private void addColumnAction() {
        String tableName = tableManager.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(filterPanel, "Please select a valid table first", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.WARNING, "Attempted to add column without selecting a valid table");
            return;
        }

        String columnName = JOptionPane.showInputDialog(filterPanel, "Enter new column name:", "Add Column", JOptionPane.PLAIN_MESSAGE);
        if (columnName == null || columnName.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "No column name provided for table '{0}'", tableName);
            return;
        }
        final String finalColumnName = columnName.trim();

        try (Connection conn = DatabaseUtils.getConnection()) {
            if (conn == null) {
                LOGGER.log(Level.SEVERE, "Failed to establish database connection for adding column to '{0}'", tableName);
                JOptionPane.showMessageDialog(filterPanel, "Failed to connect to the database", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, tableName, finalColumnName)) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(filterPanel, "Column '" + finalColumnName + "' already exists", "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.log(Level.WARNING, "Attempted to add existing column '{0}' to table '{1}'", new Object[]{finalColumnName, tableName});
                    return;
                }
            }
            String sql = "ALTER TABLE [" + tableName + "] ADD [" + finalColumnName + "] VARCHAR(255)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                JOptionPane.showMessageDialog(filterPanel, "Column '" + finalColumnName + "' added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                SwingUtilities.invokeLater(() -> {
                    tableManager.setTableName(tableName);
                    tableManager.refreshDataAndTabs();
                    initializeFilters();
                    LOGGER.log(Level.INFO, "addColumnAction: Added column '{0}' to table '{1}' and refreshed UI", new Object[]{finalColumnName, tableName});
                });
            }
        } catch (SQLException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("FeatureNotSupportedException")) {
                errorMessage = "This version of UCanAccess does not support adding columns directly. Please contact the administrator or update the database driver.";
            }
            JOptionPane.showMessageDialog(filterPanel, "Error adding column: " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "SQLException adding column '{0}' to table '{1}': {2}", new Object[]{finalColumnName, tableName, e.getMessage()});
        }
    }

    private void deleteColumnAction() {
        String tableName = tableManager.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(filterPanel, "Please select a valid table first", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.WARNING, "Attempted to delete column without selecting a valid table");
            return;
        }

        String[] columns = tableManager.getColumns();
        String columnToDelete = (String) JOptionPane.showInputDialog(filterPanel, "Select column to delete:", "Delete Column",
                JOptionPane.PLAIN_MESSAGE, null, Arrays.stream(columns).filter(c -> !c.equalsIgnoreCase("AssetName")).toArray(), null);

        if (columnToDelete == null) {
            LOGGER.log(Level.WARNING, "No column selected for deletion in table '{0}'", tableName);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            if (conn == null) {
                LOGGER.log(Level.SEVERE, "Failed to establish database connection for deleting column from '{0}'", tableName);
                JOptionPane.showMessageDialog(filterPanel, "Failed to connect to the database", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<String> remainingColumns = new ArrayList<>();
            for (String col : columns) {
                if (!col.equals(columnToDelete)) {
                    remainingColumns.add(col);
                }
            }
            if (remainingColumns.size() <= 1) {
                JOptionPane.showMessageDialog(filterPanel, "Cannot delete the last column in the table", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.SEVERE, "Attempted to delete last column '{0}' in table '{1}'", new Object[]{columnToDelete, tableName});
                return;
            }

            String tempTableName = tableName + "_temp_" + UUID.randomUUID().toString().replace("-", "");
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, null, new String[]{"TABLE"})) {
                while (rs.next()) {
                    String existingTable = rs.getString("TABLE_NAME");
                    if (existingTable.startsWith(tableName + "_temp")) {
                        try {
                            conn.createStatement().executeUpdate("DROP TABLE [" + existingTable + "]");
                            LOGGER.log(Level.INFO, "Dropped existing temporary table '{0}'", existingTable);
                        } catch (SQLException ex) {
                            LOGGER.log(Level.WARNING, "Failed to drop existing temporary table '{0}': {1}", new Object[]{existingTable, ex.getMessage()});
                        }
                    }
                }
            }

            StringBuilder createSql = new StringBuilder("CREATE TABLE [" + tempTableName + "] (");
            for (int i = 0; i < remainingColumns.size(); i++) {
                createSql.append("[").append(remainingColumns.get(i)).append("] VARCHAR(255)");
                if (remainingColumns.get(i).equals("AssetName")) {
                    createSql.append(" PRIMARY KEY");
                }
                if (i < remainingColumns.size() - 1) {
                    createSql.append(", ");
                }
            }
            createSql.append(")");
            conn.createStatement().executeUpdate(createSql.toString());

            StringBuilder selectColumns = new StringBuilder();
            for (int i = 0; i < remainingColumns.size(); i++) {
                selectColumns.append("[").append(remainingColumns.get(i)).append("]");
                if (i < remainingColumns.size() - 1) {
                    selectColumns.append(", ");
                }
            }
            String insertSql = "INSERT INTO [" + tempTableName + "] (" + selectColumns + ") SELECT " + selectColumns + " FROM [" + tableName + "]";
            conn.createStatement().executeUpdate(insertSql);

            conn.createStatement().executeUpdate("DROP TABLE [" + tableName + "]");

            String renameSql = "ALTER TABLE [" + tempTableName + "] RENAME TO [" + tableName + "]";
            conn.createStatement().executeUpdate(renameSql);

            JOptionPane.showMessageDialog(filterPanel, "Column '" + columnToDelete + "' deleted successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            SwingUtilities.invokeLater(() -> {
                tableManager.setTableName(tableName);
                tableManager.refreshDataAndTabs();
                LOGGER.log(Level.INFO, "deleteColumnAction: Deleted column '{0}' from table '{1}' and refreshed UI", new Object[]{columnToDelete, tableName});
            });
        } catch (SQLException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("FeatureNotSupportedException")) {
                errorMessage = "This version of UCanAccess does not support dropping columns directly. Please contact the administrator or update the database driver.";
            }
            JOptionPane.showMessageDialog(filterPanel, "Error deleting column: " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "SQLException deleting column '{0}' in table '{1}': {2}", new Object[]{columnToDelete, tableName, e.getMessage()});
        }
    }
}