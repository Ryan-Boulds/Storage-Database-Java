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

public class FilterPanel {

    final JPanel filterPanel;
    private final JTextField searchField;
    private final JComboBox<String> statusFilter;
    private final JComboBox<String> deptFilter;
    private boolean hasStatusColumn;
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
    private static final Logger LOGGER = Logger.getLogger(FilterPanel.class.getName());

    public FilterPanel(TriConsumer<String, String, String> filterAction, Runnable refreshAction, TableManager tableManager) {
        this.filterAction = filterAction;
        this.tableManager = tableManager;
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
            tableManager.setTableName(tableManager.getTableName()); // Force schema reload
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
        filterPanel.add(refreshButton);
        filterPanel.add(addRowButton);
        filterPanel.add(addColumnButton);
        filterPanel.add(deleteColumnButton);
    }

    private void applyFilter() {
        filterAction.accept(getSearchText(), getStatusFilter(), getDeptFilter());
    }

    public JPanel getPanel() {
        return filterPanel;
    }

    public String getSearchText() {
        return searchField.getText().toLowerCase();
    }

    public String getStatusFilter() {
        return hasStatusColumn ? (String) statusFilter.getSelectedItem() : "All";
    }

    public String getDeptFilter() {
        return hasDepartmentColumn ? (String) deptFilter.getSelectedItem() : "All";
    }

    public void setTableName(String tableName) {
        hasStatusColumn = checkColumnExists(tableName, "Status");
        hasDepartmentColumn = checkColumnExists(tableName, "Department");

        filterPanel.removeAll();
        filterPanel.add(searchLabel);
        filterPanel.add(searchField);
        if (hasStatusColumn) {
            filterPanel.add(statusLabel);
            filterPanel.add(statusFilter);
        }
        if (hasDepartmentColumn) {
            filterPanel.add(deptLabel);
            filterPanel.add(deptFilter);
            updateDepartmentFilter(tableName);
        }
        filterPanel.add(refreshButton);
        filterPanel.add(addRowButton);
        filterPanel.add(addColumnButton);
        filterPanel.add(deleteColumnButton);
        filterPanel.revalidate();
        filterPanel.repaint();
    }

    private boolean checkColumnExists(String tableName, String columnName) {
        if (tableName == null || tableName.isEmpty()) {
            return false;
        }
        try (Connection conn = DatabaseUtils.getConnection(); ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM [" + tableName + "] WHERE 1=0")) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                if (metaData.getColumnName(i).equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking column '{0}' in table '{1}': {2}", new Object[]{columnName, tableName, e.getMessage()});
        }
        return false;
    }

    private void updateDepartmentFilter(String tableName) {
        try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT DISTINCT [Department] FROM [" + tableName + "] WHERE [Department] IS NOT NULL AND [Department] <> ''")) {
            List<String> departments = new ArrayList<>();
            departments.add("All");
            while (rs.next()) {
                String dept = rs.getString(1);
                if (dept != null && !dept.trim().isEmpty()) {
                    departments.add(dept);
                }
            }
            statusFilter.setModel(new DefaultComboBoxModel<>(new String[]{"All", "Deployed", "In Storage", "Needs Repair"}));
            deptFilter.setModel(new DefaultComboBoxModel<>(departments.toArray(new String[0])));
            LOGGER.log(Level.INFO, "Updated department filter for table '{0}' with values: {1}", new Object[]{tableName, String.join(", ", departments)});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating department filter for table '{0}': {1}", new Object[]{tableName, e.getMessage()});
        }
    }

    private void addColumnAction() {
        String tableName = tableManager.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(filterPanel, "Please select a valid table first", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.WARNING, "Attempted to add column without selecting a valid table");
            return;
        }
        String inputColumnName = JOptionPane.showInputDialog(filterPanel, "Enter new column name:");
        if (inputColumnName != null && !inputColumnName.trim().isEmpty()) {
            final String newColumnName = inputColumnName.trim(); // Use a new final variable
            if (Arrays.asList(tableManager.getColumns()).contains(newColumnName)) {
                JOptionPane.showMessageDialog(filterPanel, "Error: Column '" + newColumnName + "' already exists", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, "Attempted to add duplicate column '{0}' to table '{1}'", new Object[]{newColumnName, tableName});
                return;
            }
            try (Connection conn = DatabaseUtils.getConnection()) {
                String sql = "ALTER TABLE [" + tableName + "] ADD COLUMN [" + newColumnName + "] VARCHAR(255)";
                conn.createStatement().executeUpdate(sql);
                JOptionPane.showMessageDialog(filterPanel, "Column '" + newColumnName + "' added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                SwingUtilities.invokeLater(() -> {
                    tableManager.setTableName(tableName); // Force schema reload
                    tableManager.refreshDataAndTabs();
                    LOGGER.log(Level.INFO, "addColumnAction: Added column '{0}' to table '{1}' and refreshed UI", new Object[]{newColumnName, tableName});
                });
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(filterPanel, "Error adding column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.SEVERE, "SQLException adding column '{0}' to table '{1}': {2}", new Object[]{newColumnName, tableName, e.getMessage()});
            }
        }
    }

    private void deleteColumnAction() {
        String tableName = tableManager.getTableName();
        String[] columnNames = tableManager.getColumns();
        String columnToDelete = (String) JOptionPane.showInputDialog(
                filterPanel,
                "Select column to delete:",
                "Delete Column",
                JOptionPane.PLAIN_MESSAGE,
                null,
                columnNames,
                columnNames[0]
        );
        if (columnToDelete == null || columnToDelete.equals("AssetName")) {
            JOptionPane.showMessageDialog(filterPanel, "Error: Cannot delete the primary key column 'AssetName'", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Attempted to delete primary key column 'AssetName' in table '{0}'", tableName);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                filterPanel,
                "Are you sure you want to delete the column '" + columnToDelete + "'? This will remove all data in this column.",
                "Confirm Delete Column",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseUtils.getConnection()) {
                List<String> remainingColumns = new ArrayList<>();
                for (String col : columnNames) {
                    if (!col.equals(columnToDelete)) {
                        remainingColumns.add(col);
                    }
                }
                if (remainingColumns.isEmpty()) {
                    JOptionPane.showMessageDialog(filterPanel, "Error: Cannot delete the last column in the table", "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.log(Level.SEVERE, "Attempted to delete last column '{0}' in table '{1}'", new Object[]{columnToDelete, tableName});
                    return;
                }

                // Use a unique temporary table name
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
                    tableManager.setTableName(tableName); // Force schema reload
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
}
