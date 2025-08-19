package view_inventory_tab;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import utils.DatabaseUtils;
import utils.UIComponentUtils;

public class AddRowEntry extends JDialog {
    private String[] columnNames;
    private final Map<String, Integer> columnTypes;
    private JComponent[] inputs;
    private final TableManager tableManager;
    private final JPanel inputPanel;
    private final JScrollPane scrollPane;
    private static final Logger LOGGER = Logger.getLogger(AddRowEntry.class.getName());

    public AddRowEntry(JFrame parent, TableManager tableManager) {
        super(parent, "Add Row Entry", true);
        this.tableManager = tableManager;
        this.columnNames = tableManager.getColumns();
        this.columnTypes = tableManager.getColumnTypes();

        setLayout(new BorderLayout());
        setSize(600, 800);
        setLocationRelativeTo(parent);

        inputPanel = new JPanel(new GridBagLayout());
        scrollPane = new JScrollPane(inputPanel);
        add(scrollPane, BorderLayout.CENTER);

        refreshInputFields();

        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveAction());
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        JButton addColumnButton = new JButton("Add Column");
        addColumnButton.addActionListener(e -> addColumnAction());
        buttonPanel.add(addColumnButton);

        JButton deleteColumnButton = new JButton("Delete Column");
        deleteColumnButton.addActionListener(e -> deleteColumnAction());
        buttonPanel.add(deleteColumnButton);

        JButton renameColumnButton = new JButton("Rename Column");
        renameColumnButton.addActionListener(e -> renameColumnAction());
        buttonPanel.add(renameColumnButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void refreshInputFields() {
        LOGGER.log(Level.INFO, "refreshInputFields: Starting refresh, current columns: {0}", String.join(", ", columnNames));
        columnNames = tableManager.getColumns(); // Refresh columns from TableManager
        inputs = new JComponent[columnNames.length];
        inputPanel.removeAll(); // Clear existing components

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        FontMetrics fm = inputPanel.getFontMetrics(inputPanel.getFont());
        int maxLabelWidth = 0;
        for (String columnName : columnNames) {
            maxLabelWidth = Math.max(maxLabelWidth, fm.stringWidth(columnName));
        }

        for (int i = 0; i < columnNames.length; i++) {
            String columnName = columnNames[i];
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.anchor = GridBagConstraints.EAST;
            inputPanel.add(UIComponentUtils.createAlignedLabel(columnName), gbc);

            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            Integer sqlType = columnTypes.get(columnName);
            if (sqlType != null && sqlType == Types.BIT) {
                inputs[i] = new JCheckBox();
            } else {
                inputs[i] = UIComponentUtils.createFormattedTextField();
                ((JTextField) inputs[i]).setColumns(20);
            }
            inputPanel.add(inputs[i], gbc);
        }

        // Force UI update
        inputPanel.revalidate();
        inputPanel.repaint();
        scrollPane.revalidate();
        scrollPane.repaint();
        SwingUtilities.invokeLater(() -> {
            inputPanel.revalidate();
            inputPanel.repaint();
            scrollPane.revalidate();
            scrollPane.repaint();
            LOGGER.log(Level.INFO, "refreshInputFields: Completed UI refresh, new columns: {0}", String.join(", ", columnNames));
        });
    }

    private void saveAction() {
        String tableName = tableManager.getTableName();
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            String columnName = columnNames[i];
            if (inputs[i] instanceof JTextField) {
                String text = ((JTextField) inputs[i]).getText().trim();
                values.put(columnName, text.isEmpty() ? null : text);
            } else if (inputs[i] instanceof JCheckBox) {
                values.put(columnName, ((JCheckBox) inputs[i]).isSelected() ? "true" : "false");
            }
        }

        String assetName = values.get("AssetName");
        if (assetName == null || assetName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Error: AssetName is required", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Attempted to save empty AssetName in table '{0}'", tableName);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            StringBuilder sql = new StringBuilder("INSERT INTO [" + tableName + "] (");
            StringBuilder placeholders = new StringBuilder();
            List<String> columns = new ArrayList<>();
            List<String> parameters = new ArrayList<>();
            for (String column : columnNames) {
                columns.add("[" + column + "]");
                placeholders.append("?");
                parameters.add(values.get(column));
                if (!column.equals(columnNames[columnNames.length - 1])) {
                    placeholders.append(", ");
                }
            }
            sql.append(String.join(", ", columns)).append(") VALUES (").append(placeholders).append(")");
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < parameters.size(); i++) {
                    ps.setString(i + 1, parameters.get(i));
                }
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Row added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            SwingUtilities.invokeLater(() -> {
                tableManager.refreshDataAndTabs();
                dispose();
            });
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding row: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "SQLException adding row to table '{0}': {1}", new Object[]{tableName, e.getMessage()});
        }
    }

    private void addColumnAction() {
        String inputColumnName = JOptionPane.showInputDialog(this, "Enter new column name:");
        if (inputColumnName != null && !inputColumnName.trim().isEmpty()) {
            final String newColumnName = inputColumnName.trim();
            String tableName = tableManager.getTableName();
            if (Arrays.asList(columnNames).contains(newColumnName)) {
                JOptionPane.showMessageDialog(this, "Error: Column '" + newColumnName + "' already exists", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.SEVERE, "Attempted to add duplicate column '{0}' to table '{1}'", new Object[]{newColumnName, tableName});
                return;
            }
            try (Connection conn = DatabaseUtils.getConnection()) {
                String sql = "ALTER TABLE [" + tableName + "] ADD COLUMN [" + newColumnName + "] VARCHAR(255)";
                conn.createStatement().executeUpdate(sql);
                JOptionPane.showMessageDialog(this, "Column '" + newColumnName + "' added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                SwingUtilities.invokeLater(() -> {
                    tableManager.setTableName(tableName); // Force schema reload
                    tableManager.refreshDataAndTabs();
                    refreshInputFields();
                    LOGGER.log(Level.INFO, "addColumnAction: Added column '{0}' to table '{1}' and refreshed UI", new Object[]{newColumnName, tableName});
                });
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.SEVERE, "SQLException adding column '{0}' to table '{1}': {2}", new Object[]{newColumnName, tableName, e.getMessage()});
            }
        }
    }

    private void deleteColumnAction() {
        String tableName = tableManager.getTableName();
        String columnToDelete = (String) JOptionPane.showInputDialog(
            this,
            "Select column to delete:",
            "Delete Column",
            JOptionPane.PLAIN_MESSAGE,
            null,
            columnNames,
            columnNames[0]
        );
        if (columnToDelete == null || columnToDelete.equals("AssetName")) {
            JOptionPane.showMessageDialog(this, "Error: Cannot delete the primary key column 'AssetName'", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Attempted to delete primary key column 'AssetName' in table '{0}'", tableName);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
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
                    JOptionPane.showMessageDialog(this, "Error: Cannot delete the last column in the table", "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.log(Level.SEVERE, "Attempted to delete last column '{0}' in table '{1}'", new Object[]{columnToDelete, tableName});
                    return;
                }

                String tempTableName = tableName + "_temp";
                // Drop temporary table if it exists
                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet rs = meta.getTables(null, null, tempTableName, new String[]{"TABLE"})) {
                    if (rs.next()) {
                        conn.createStatement().executeUpdate("DROP TABLE [" + tempTableName + "]");
                        LOGGER.log(Level.INFO, "Dropped existing temporary table '{0}'", tempTableName);
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

                JOptionPane.showMessageDialog(this, "Column '" + columnToDelete + "' deleted successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                SwingUtilities.invokeLater(() -> {
                    tableManager.setTableName(tableName); // Force schema reload
                    tableManager.refreshDataAndTabs();
                    refreshInputFields();
                    LOGGER.log(Level.INFO, "deleteColumnAction: Deleted column '{0}' from table '{1}' and refreshed UI", new Object[]{columnToDelete, tableName});
                });
            } catch (SQLException e) {
                String errorMessage = e.getMessage();
                if (errorMessage.contains("FeatureNotSupportedException")) {
                    errorMessage = "This version of UCanAccess does not support dropping columns directly. Please contact the administrator or update the database driver.";
                }
                JOptionPane.showMessageDialog(this, "Error deleting column: " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.SEVERE, "SQLException deleting column '{0}' in table '{1}': {2}", new Object[]{columnToDelete, tableName, e.getMessage()});
            }
        }
    }

    private void renameColumnAction() {
        String tableName = tableManager.getTableName();
        String oldColumnName = (String) JOptionPane.showInputDialog(
            this,
            "Select column to rename:",
            "Rename Column",
            JOptionPane.PLAIN_MESSAGE,
            null,
            columnNames,
            columnNames[0]
        );
        if (oldColumnName != null && !oldColumnName.equals("AssetName")) {
            String newColumnName = JOptionPane.showInputDialog(this, "Enter new column name:");
            if (newColumnName != null && !newColumnName.trim().isEmpty()) {
                final String finalNewColumnName = newColumnName.trim();
                try (Connection conn = DatabaseUtils.getConnection()) {
                    String sql = "ALTER TABLE [" + tableName + "] RENAME COLUMN [" + oldColumnName + "] TO [" + finalNewColumnName + "]";
                    conn.createStatement().executeUpdate(sql);
                    JOptionPane.showMessageDialog(this, "Column renamed successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                    SwingUtilities.invokeLater(() -> {
                        tableManager.setTableName(tableName); // Force schema reload
                        tableManager.refreshDataAndTabs();
                        refreshInputFields();
                        LOGGER.log(Level.INFO, "renameColumnAction: Renamed column '{0}' to '{1}' in table '{2}' and refreshed UI", new Object[]{oldColumnName, finalNewColumnName, tableName});
                    });
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error renaming column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.log(Level.SEVERE, "Error renaming column '{0}' in table '{1}': {2}", new Object[]{oldColumnName, tableName, e.getMessage()});
                }
            }
        } else if (oldColumnName != null) {
            JOptionPane.showMessageDialog(this, "Error: Cannot rename the primary key column 'AssetName'", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void showAddDialog(JFrame parent, TableManager tableManager) {
        AddRowEntry dialog = new AddRowEntry(parent, tableManager);
        dialog.setVisible(true);
    }
}