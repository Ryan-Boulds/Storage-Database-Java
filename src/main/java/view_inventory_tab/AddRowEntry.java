package view_inventory_tab;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

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

import utils.DataUtils;
import utils.DatabaseUtils;
import utils.SQLGenerator;
import utils.UIComponentUtils;

public class AddRowEntry extends JDialog {
    private final HashMap<String, String> device;
    private String[] columnNames;
    private final Map<String, Integer> columnTypes;
    private JComponent[] inputs;
    private final TableManager tableManager;
    private final JPanel inputPanel;
    private final JScrollPane scrollPane;
    private static final Logger LOGGER = Logger.getLogger(AddRowEntry.class.getName());

    public AddRowEntry(JFrame parent, TableManager tableManager) {
        super(parent, "Add Row Entry", true);
        this.device = new HashMap<>();
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
        inputPanel.removeAll();
        columnNames = tableManager.getColumns();
        inputs = new JComponent[columnNames.length];

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
            int sqlType = columnTypes.getOrDefault(columnName, Types.VARCHAR);
            System.out.println("AddDialog: Column " + columnName + " SQL type: " + sqlType);

            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            inputPanel.add(UIComponentUtils.createAlignedLabel(columnName), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            if (sqlType == Types.BIT || sqlType == Types.BOOLEAN) {
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(device.getOrDefault(columnName, "false").equalsIgnoreCase("true"));
                inputs[i] = checkBox;
            } else {
                JTextField textField = new JTextField(20);
                textField.setText(device.getOrDefault(columnName, ""));
                inputs[i] = textField;
            }
            inputPanel.add(inputs[i], gbc);
        }

        inputPanel.revalidate();
        inputPanel.repaint();
    }

    private void saveAction() {
        String tableName = tableManager.getTableName();
        Map<String, String> data = new HashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            String value;
            if (inputs[i] instanceof JCheckBox) {
                value = ((JCheckBox) inputs[i]).isSelected() ? "true" : "false";
            } else {
                value = ((JTextField) inputs[i]).getText();
            }
            data.put(columnNames[i], value);
        }

        String validationResult = DataUtils.validateDevice(data, tableName);
        if (validationResult != null) {
            JOptionPane.showMessageDialog(this, validationResult, "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = SQLGenerator.generateInsertSQL(tableName, data);
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < columnNames.length; i++) {
                String value = data.get(columnNames[i]);
                int sqlType = columnTypes.getOrDefault(columnNames[i], Types.VARCHAR);
                if (value == null || value.trim().isEmpty()) {
                    ps.setNull(i + 1, sqlType);
                } else if (sqlType == Types.BIT || sqlType == Types.BOOLEAN) {
                    ps.setBoolean(i + 1, Boolean.parseBoolean(value));
                } else {
                    ps.setString(i + 1, value);
                }
            }
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Row added successfully");
            SwingUtilities.invokeLater(() -> {
                tableManager.refreshDataAndTabs();
                dispose();
            });
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving entry to table {0}: {1}", new Object[]{tableName, e.getMessage()});
            JOptionPane.showMessageDialog(this, "Error saving entry: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addColumnAction() {
        String tableName = tableManager.getTableName();
        String newColumnName = JOptionPane.showInputDialog(this, "Enter new column name:");
        if (newColumnName != null && !newColumnName.trim().isEmpty()) {
            newColumnName = newColumnName.trim();
            for (String column : columnNames) {
                if (column.equalsIgnoreCase(newColumnName)) {
                    JOptionPane.showMessageDialog(this, "Error: Column '" + newColumnName + "' already exists (case-insensitive)", "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("AddDialog: Attempted to add existing column '" + newColumnName + "' to table '" + tableName + "'");
                    return;
                }
            }
            try (Connection conn = DatabaseUtils.getConnection()) {
                String sql = "ALTER TABLE [" + tableName + "] ADD [" + newColumnName + "] VARCHAR(255)";
                conn.createStatement().executeUpdate(sql);
                JOptionPane.showMessageDialog(this, "Column added successfully");
                SwingUtilities.invokeLater(() -> {
                    tableManager.setTableName(tableName);
                    tableManager.refreshDataAndTabs();
                    refreshInputFields();
                });
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("AddDialog: SQLException adding column to table '" + tableName + "': " + e.getMessage());
            }
        }
    }

    private void deleteColumnAction() {
        String tableName = tableManager.getTableName();
        String[] columns = tableManager.getColumns();
        if (columns == null || columns.length == 0) {
            JOptionPane.showMessageDialog(this, "Error: No columns available to delete", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: No columns available for table '" + tableName + "'");
            return;
        }

        String columnToDelete = (String) JOptionPane.showInputDialog(
            this,
            "Select column to delete:",
            "Delete Column",
            JOptionPane.PLAIN_MESSAGE,
            null,
            columns,
            columns[0]
        );
        if (columnToDelete == null) {
            return; // User canceled
        }
        if (columnToDelete.equals("AssetName")) {
            JOptionPane.showMessageDialog(this, "Error: Cannot delete primary key column 'AssetName'", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: Attempted to delete primary key column 'AssetName' in table '" + tableName + "'");
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
                String[] newColumns = new String[columns.length - 1];
                int index = 0;
                for (String col : columns) {
                    if (!col.equals(columnToDelete)) {
                        newColumns[index++] = col;
                    }
                }
                if (newColumns.length == 0) {
                    JOptionPane.showMessageDialog(this, "Error: Cannot delete the last column in the table", "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("AddDialog: Attempted to delete last column '" + columnToDelete + "' in table '" + tableName + "'");
                    return;
                }

                String tempTableName = tableName + "_temp";
                StringBuilder createSql = new StringBuilder("CREATE TABLE [" + tempTableName + "] (");
                for (int i = 0; i < newColumns.length; i++) {
                    createSql.append("[").append(newColumns[i]).append("] VARCHAR(255)");
                    if (newColumns[i].equals("AssetName")) {
                        createSql.append(" PRIMARY KEY");
                    }
                    if (i < newColumns.length - 1) {
                        createSql.append(", ");
                    }
                }
                createSql.append(")");
                conn.createStatement().executeUpdate(createSql.toString());

                StringBuilder selectColumns = new StringBuilder();
                for (int i = 0; i < newColumns.length; i++) {
                    selectColumns.append("[").append(newColumns[i]).append("]");
                    if (i < newColumns.length - 1) {
                        selectColumns.append(", ");
                    }
                }
                String insertSql = "INSERT INTO [" + tempTableName + "] (" + selectColumns + ") SELECT " + selectColumns + " FROM [" + tableName + "]";
                conn.createStatement().executeUpdate(insertSql);

                conn.createStatement().executeUpdate("DROP TABLE [" + tableName + "]");

                String renameSql = "ALTER TABLE [" + tempTableName + "] RENAME TO [" + tableName + "]";
                conn.createStatement().executeUpdate(renameSql);

                JOptionPane.showMessageDialog(this, "Column deleted successfully");
                SwingUtilities.invokeLater(() -> {
                    tableManager.setTableName(tableName);
                    tableManager.refreshDataAndTabs();
                    refreshInputFields();
                });
            } catch (SQLException e) {
                String errorMessage = e.getMessage();
                if (errorMessage.contains("FeatureNotSupportedException")) {
                    errorMessage = "This version of UCanAccess does not support dropping columns directly. Please contact the administrator or update the database driver.";
                }
                JOptionPane.showMessageDialog(this, "Error deleting column: " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("AddDialog: SQLException deleting column '" + columnToDelete + "' in table '" + tableName + "': " + e.getMessage());
            }
        }
    }

    private void renameColumnAction() {
        String tableName = tableManager.getTableName();
        String[] columns = tableManager.getColumns();
        if (columns == null || columns.length == 0) {
            JOptionPane.showMessageDialog(this, "Error: No columns available to rename", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: No columns available for table '" + tableName + "'");
            return;
        }

        String oldColumnName = (String) JOptionPane.showInputDialog(
            this,
            "Select column to rename:",
            "Rename Column",
            JOptionPane.PLAIN_MESSAGE,
            null,
            columns,
            columns[0]
        );
        if (oldColumnName == null) {
            return; // User canceled
        }
        if (oldColumnName.equals("AssetName")) {
            JOptionPane.showMessageDialog(this, "Error: Cannot rename primary key column 'AssetName'", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: Attempted to rename primary key column 'AssetName' in table '" + tableName + "'");
            return;
        }

        String newColumnName = JOptionPane.showInputDialog(this, "Enter new column name:");
        if (newColumnName != null && !newColumnName.trim().isEmpty()) {
            newColumnName = newColumnName.trim();
            for (String column : columns) {
                if (column.equalsIgnoreCase(newColumnName)) {
                    JOptionPane.showMessageDialog(this, "Error: Column '" + newColumnName + "' already exists (case-insensitive)", "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("AddDialog: Attempted to rename to existing column '" + newColumnName + "' in table '" + tableName + "'");
                    return;
                }
            }

            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to rename the column '" + oldColumnName + "' to '" + newColumnName + "'?",
                "Confirm Rename Column",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseUtils.getConnection()) {
                    String[] newColumns = new String[columns.length];
                    for (int i = 0; i < columns.length; i++) {
                        if (columns[i].equals(oldColumnName)) {
                            newColumns[i] = newColumnName;
                        } else {
                            newColumns[i] = columns[i];
                        }
                    }

                    String tempTableName = tableName + "_temp";
                    StringBuilder createSql = new StringBuilder("CREATE TABLE [" + tempTableName + "] (");
                    for (int i = 0; i < newColumns.length; i++) {
                        createSql.append("[").append(newColumns[i]).append("] VARCHAR(255)");
                        if (newColumns[i].equals("AssetName")) {
                            createSql.append(" PRIMARY KEY");
                        }
                        if (i < newColumns.length - 1) {
                            createSql.append(", ");
                        }
                    }
                    createSql.append(")");
                    conn.createStatement().executeUpdate(createSql.toString());

                    StringBuilder selectColumns = new StringBuilder();
                    for (int i = 0; i < columns.length; i++) {
                        selectColumns.append("[").append(columns[i]).append("]");
                        if (i < columns.length - 1) {
                            selectColumns.append(", ");
                        }
                    }
                    StringBuilder insertColumns = new StringBuilder();
                    for (int i = 0; i < newColumns.length; i++) {
                        insertColumns.append("[").append(newColumns[i]).append("]");
                        if (i < newColumns.length - 1) {
                            insertColumns.append(", ");
                        }
                    }
                    String insertSql = "INSERT INTO [" + tempTableName + "] (" + insertColumns + ") SELECT " + selectColumns + " FROM [" + tableName + "]";
                    conn.createStatement().executeUpdate(insertSql);

                    conn.createStatement().executeUpdate("DROP TABLE [" + tableName + "]");

                    String renameSql = "ALTER TABLE [" + tempTableName + "] RENAME TO [" + tableName + "]";
                    conn.createStatement().executeUpdate(renameSql);

                    JOptionPane.showMessageDialog(this, "Column renamed successfully");
                    SwingUtilities.invokeLater(() -> {
                        tableManager.setTableName(tableName);
                        tableManager.refreshDataAndTabs();
                        refreshInputFields();
                    });
                } catch (SQLException e) {
                    String errorMessage = e.getMessage();
                    if (errorMessage.contains("FeatureNotSupportedException")) {
                        errorMessage = "This version of UCanAccess does not support renaming columns directly. Please contact the administrator or update the database driver.";
                    }
                    JOptionPane.showMessageDialog(this, "Error renaming column: " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("AddDialog: SQLException renaming column '" + oldColumnName + "' to '" + newColumnName + "' in table '" + tableName + "': " + e.getMessage());
                }
            }
        }
    }

    public static void showAddDialog(JFrame parent, TableManager tableManager) {
        AddRowEntry dialog = new AddRowEntry(parent, tableManager);
        dialog.setVisible(true);
    }
}