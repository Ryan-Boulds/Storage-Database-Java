package view_inventory_tab;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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
import view_inventory_tab.Add_And_Edit_Entries.AddRowEntry;

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
    private final JButton importDataButton;
    private final JButton licenseKeyTrackerButton;
    private final TriConsumer<String, String, String> filterAction;
    private final Runnable refreshAction;
    private final TableManager tableManager;
    private final ViewInventoryTab parentTab;
    private static final Logger LOGGER = Logger.getLogger(FilterPanel.class.getName());

    public FilterPanel(TriConsumer<String, String, String> filterAction, Runnable refreshAction, TableManager tableManager, TableListPanel tableListPanel, ViewInventoryTab parentTab) {
        this.filterAction = filterAction;
        this.refreshAction = refreshAction;
        this.tableManager = tableManager;
        this.parentTab = parentTab;
        this.filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.searchLabel = UIComponentUtils.createAlignedLabel("Search:");
        this.searchField = UIComponentUtils.createFormattedTextField();
        this.statusLabel = UIComponentUtils.createAlignedLabel("Status:");
        this.statusFilter = new JComboBox<>(new String[]{"All", "Active", "Inactive"});
        this.deptLabel = UIComponentUtils.createAlignedLabel("Department:");
        this.deptFilter = new JComboBox<>(new String[]{"All"});
        this.refreshButton = new JButton("Refresh");
        this.addRowButton = new JButton("Add Row");
        this.addColumnButton = new JButton("Add Column");
        this.deleteColumnButton = new JButton("Delete Column");
        this.importDataButton = new JButton("Import Data");
        this.licenseKeyTrackerButton = new JButton("License Key Tracker");
        initializeUI();
        checkDepartmentColumn();
    }

    private void initializeUI() {
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

        statusFilter.addActionListener(e -> applyFilter());
        deptFilter.addActionListener(e -> applyFilter());

        refreshButton.addActionListener(e -> {
            filterAction.accept("", "All", "All");
            refreshAction.run();
            LOGGER.log(Level.INFO, "Refresh button clicked for table '{0}'", tableManager.getTableName());
        });

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

        addColumnButton.addActionListener(e -> addColumnAction());
        deleteColumnButton.addActionListener(e -> deleteColumnAction());

        importDataButton.addActionListener(e -> {
            parentTab.showImportDataTab();
            LOGGER.log(Level.INFO, "Navigated to ImportDataTab from FilterPanel");
        });

        licenseKeyTrackerButton.addActionListener(e -> {
            parentTab.showLicenseKeyTracker();
            LOGGER.log(Level.INFO, "Navigated to LicenseKeyTracker from FilterPanel");
        });

        filterPanel.add(searchLabel);
        filterPanel.add(searchField);
        filterPanel.add(statusLabel);
        filterPanel.add(statusFilter);
        if (hasDepartmentColumn) {
            filterPanel.add(deptLabel);
            filterPanel.add(deptFilter);
        }
        filterPanel.add(refreshButton);
        filterPanel.add(addRowButton);
        filterPanel.add(addColumnButton);
        filterPanel.add(deleteColumnButton);
        filterPanel.add(importDataButton);
        filterPanel.add(licenseKeyTrackerButton);
    }

    private void checkDepartmentColumn() {
        String tableName = tableManager.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            hasDepartmentColumn = false;
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, tableName, "Department")) {
                hasDepartmentColumn = rs.next();
                if (hasDepartmentColumn) {
                    updateDepartmentFilter();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking Department column for table '{0}': {1}", new Object[]{tableName, e.getMessage()});
        }
    }

    private void updateDepartmentFilter() {
        String tableName = tableManager.getTableName();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(new String[]{"All"});
        try (Connection conn = DatabaseUtils.getConnection()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DISTINCT Department FROM [" + tableName + "] WHERE Department IS NOT NULL")) {
                while (rs.next()) {
                    model.addElement(rs.getString("Department"));
                }
            }
            deptFilter.setModel(model);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating department filter for table '{0}': {1}", new Object[]{tableName, e.getMessage()});
        }
    }

    private void applyFilter() {
        String searchText = searchField.getText();
        String status = (String) statusFilter.getSelectedItem();
        String dept = hasDepartmentColumn ? (String) deptFilter.getSelectedItem() : "All";
        filterAction.accept(searchText, status, dept);
        LOGGER.log(Level.INFO, "Applied filter: search='{0}', status='{1}', dept='{2}'", new Object[]{searchText, status, dept});
    }

    private void addColumnAction() {
        String tableName = tableManager.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(filterPanel, "Please select a valid table first", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.WARNING, "Attempted to add column without selecting a valid table");
            return;
        }
        String newColumnName = JOptionPane.showInputDialog(filterPanel, "Enter new column name:");
        if (newColumnName == null || newColumnName.trim().isEmpty()) {
            return;
        }
        final String finalColumnName = newColumnName.trim().replace(" ", "_");
        try (Connection conn = DatabaseUtils.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                String sql = "ALTER TABLE [" + tableName + "] ADD [" + finalColumnName + "] VARCHAR(255)";
                stmt.executeUpdate(sql);
                LOGGER.log(Level.INFO, "Added column '{0}' to table '{1}'", new Object[]{finalColumnName, tableName});
            }
            JOptionPane.showMessageDialog(filterPanel, "Column '" + finalColumnName + "' added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            SwingUtilities.invokeLater(() -> {
                tableManager.initializeColumns();
                tableManager.refreshDataAndTabs();
                checkDepartmentColumn();
                LOGGER.log(Level.INFO, "Refreshed table '{0}' after adding column '{1}'", new Object[]{tableName, finalColumnName});
            });
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(filterPanel, "Error adding column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        if (columns == null || columns.length == 0) {
            JOptionPane.showMessageDialog(filterPanel, "No columns available to delete", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.WARNING, "No columns available to delete in table '{0}'", tableName);
            return;
        }
        String columnToDelete = (String) JOptionPane.showInputDialog(
            filterPanel,
            "Select column to delete:",
            "Delete Column",
            JOptionPane.QUESTION_MESSAGE,
            null,
            columns,
            columns[0]
        );
        if (columnToDelete == null || columnToDelete.equals("AssetName")) {
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection()) {
            String tempTableName = "Temp_" + UUID.randomUUID().toString().replace("-", "");
            List<String> remainingColumns = new ArrayList<>(Arrays.asList(columns));
            remainingColumns.remove(columnToDelete);

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
                tableManager.initializeColumns();
                tableManager.refreshDataAndTabs();
                checkDepartmentColumn();
                LOGGER.log(Level.INFO, "deleteColumnAction: Deleted column '{0}' from table '{1}' and refreshed UI", new Object[]{columnToDelete, tableName});
            });
        } catch (SQLException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("FeatureNotSupportedException")) {
                errorMessage = "This version of UCanAccess does not support dropping columns directly. Please contact the administrator or update the database driver.";
            }
            JOptionPane.showMessageDialog(filterPanel, "Error deleting column: " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "SQLException deleting column '{0}' in table '{1}': {2}", new Object[]{columnToDelete, tableName, errorMessage});
        }
    }

    public ViewInventoryTab getViewInventoryTab() {
        return parentTab;
    }
}