package view_software_list_tab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
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

import utils.DataUtils;
import utils.DatabaseUtils;
import utils.UIComponentUtils;

public class ModifyRowEntry extends JDialog {
    private final String[] columnNames;
    private final Map<String, Integer> columnTypes;
    private final JComponent[] inputs;
    private final TableManager tableManager;
    private final String primaryKeyColumn = "AssetName";
    private static final Logger LOGGER = Logger.getLogger(ModifyRowEntry.class.getName());

    public ModifyRowEntry(JFrame parent, HashMap<String, String> device, String deviceType, TableManager tableManager) {
        super(parent, "Modify Row Entry", true);
        this.tableManager = tableManager;
        this.columnNames = tableManager.getColumns();
        this.columnTypes = tableManager.getColumnTypes();

        setLayout(new BorderLayout());
        setSize(600, 800);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        this.inputs = new JComponent[columnNames.length];

        int maxLabelWidth = 0;
        FontMetrics fm = panel.getFontMetrics(panel.getFont());
        for (String fieldName : columnNames) {
            maxLabelWidth = Math.max(maxLabelWidth, fm.stringWidth(fieldName + ":"));
        }
        maxLabelWidth += 20;

        for (int i = 0; i < columnNames.length; i++) {
            String fieldName = columnNames[i];
            JComponent input;
            String key = fieldName;
            Integer sqlType = columnTypes.getOrDefault(key, Types.VARCHAR);
            LOGGER.log(Level.INFO, "ModifyDialog: Column {0} SQL type: {1}", new Object[]{key, sqlType});

            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0;
            JPanel labelPanel = new JPanel(new BorderLayout());
            labelPanel.add(UIComponentUtils.createAlignedLabel(fieldName + ":"), BorderLayout.WEST);
            labelPanel.setPreferredSize(new java.awt.Dimension(maxLabelWidth, 30));
            panel.add(labelPanel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            if (sqlType == null) {
                JTextField textField = UIComponentUtils.createFormattedTextField();
                textField.setText(device.getOrDefault(key, ""));
                textField.setPreferredSize(new java.awt.Dimension(200, 30));
                if (fieldName.equals(primaryKeyColumn)) {
                    textField.setEditable(false);
                    textField.setBackground(Color.LIGHT_GRAY);
                }
                input = textField;
            } else {
                switch (sqlType) {
                    case Types.DATE:
                    case Types.TIMESTAMP:
                        JPanel datePicker = UIComponentUtils.createFormattedDatePicker();
                        JTextField dateField = (JTextField) datePicker.getComponent(0);
                        dateField.setText(device.getOrDefault(key, ""));
                        dateField.setPreferredSize(new java.awt.Dimension(200, 30));
                        input = datePicker;
                        break;
                    case Types.DOUBLE:
                    case Types.FLOAT:
                    case Types.DECIMAL:
                        JTextField numericField = UIComponentUtils.createFormattedTextField();
                        numericField.setText(device.getOrDefault(key, ""));
                        numericField.setPreferredSize(new java.awt.Dimension(200, 30));
                        input = numericField;
                        break;
                    case Types.BIT:
                    case Types.BOOLEAN:
                        JCheckBox checkBox = new JCheckBox();
                        checkBox.setSelected(Boolean.parseBoolean(device.getOrDefault(key, "false")));
                        input = checkBox;
                        break;
                    default:
                        JTextField textField = UIComponentUtils.createFormattedTextField();
                        textField.setText(device.getOrDefault(key, ""));
                        textField.setPreferredSize(new java.awt.Dimension(200, 30));
                        if (fieldName.equals(primaryKeyColumn)) {
                            textField.setEditable(false);
                            textField.setBackground(Color.LIGHT_GRAY);
                        }
                        input = textField;
                        break;
                }
            }
            inputs[i] = input;
            panel.add(input, gbc);
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane, BorderLayout.CENTER);

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

    private void saveAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Modifying rows in the Inventory table is not allowed", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.severe("ModifyDialog: Attempted to modify row in Inventory table, which is not allowed");
            return;
        }

        HashMap<String, String> updatedDevice = new HashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            String columnName = columnNames[i];
            JComponent input = inputs[i];
            String value;
            if (input instanceof JCheckBox) {
                value = ((JCheckBox) input).isSelected() ? "true" : "false";
            } else if (input instanceof JPanel && ((JPanel) input).getComponent(0) instanceof JTextField) {
                value = ((JTextField) ((JPanel) input).getComponent(0)).getText().trim();
            } else {
                value = ((JTextField) input).getText().trim();
            }
            updatedDevice.put(columnName, value);
        }

        String validationError = DataUtils.validateData(updatedDevice, columnTypes);
        if (validationError != null) {
            JOptionPane.showMessageDialog(this, validationError, "Validation Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "ModifyDialog: Validation error: {0}", validationError);
            return;
        }

        try {
            DatabaseUtils.updateDevice(tableName, updatedDevice);
            JOptionPane.showMessageDialog(this, "Row updated successfully");
            SwingUtilities.invokeLater(() -> {
                tableManager.refreshDataAndTabs();
                dispose();
            });
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating row: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "ModifyDialog: Error updating row in table ''{0}'': {1}", new Object[]{tableName, e.getMessage()});
        }
    }

    private void addColumnAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Adding columns to the Inventory table is not allowed", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.severe("ModifyDialog: Attempted to add column to Inventory table, which is not allowed");
            return;
        }

        String newColumnName = JOptionPane.showInputDialog(this, "Enter new column name:");
        if (newColumnName != null && !newColumnName.trim().isEmpty()) {
            newColumnName = newColumnName.trim();
            try (Connection conn = DatabaseUtils.getConnection()) {
                String sql = "ALTER TABLE [" + tableName + "] ADD COLUMN [" + newColumnName + "] VARCHAR(255)";
                conn.createStatement().executeUpdate(sql);
                JOptionPane.showMessageDialog(this, "Column added successfully");
                SwingUtilities.invokeLater(() -> {
                    tableManager.setTableName(tableName);
                    tableManager.refreshDataAndTabs();
                });
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.SEVERE, "ModifyDialog: SQLException adding column in table ''{0}'': {1}", new Object[]{tableName, e.getMessage()});
            }
        }
    }

    private void deleteColumnAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Deleting columns from the Inventory table is not allowed", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.severe("ModifyDialog: Attempted to delete column from Inventory table, which is not allowed");
            return;
        }

        String columnToDelete = (String) JOptionPane.showInputDialog(
            this,
            "Select column to delete:",
            "Delete Column",
            JOptionPane.PLAIN_MESSAGE,
            null,
            columnNames,
            columnNames[0]
        );
        if (columnToDelete != null && !columnToDelete.equals(primaryKeyColumn)) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the column '" + columnToDelete + "'? This will remove all data in this column.",
                "Confirm Delete Column",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseUtils.getConnection()) {
                    String sql = "ALTER TABLE [" + tableName + "] DROP COLUMN [" + columnToDelete + "]";
                    conn.createStatement().executeUpdate(sql);
                    JOptionPane.showMessageDialog(this, "Column '" + columnToDelete + "' deleted successfully");
                    SwingUtilities.invokeLater(() -> {
                        tableManager.setTableName(tableName);
                        tableManager.refreshDataAndTabs();
                    });
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error deleting column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.log(Level.SEVERE, "ModifyDialog: SQLException deleting column ''{0}'' in table ''{1}'': {2}", new Object[]{columnToDelete, tableName, e.getMessage()});
                }
            }
        } else if (columnToDelete != null && columnToDelete.equals(primaryKeyColumn)) {
            JOptionPane.showMessageDialog(this, "Error: Cannot delete the primary key column '" + primaryKeyColumn + "'", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE,"ModifyDialog: Attempted to delete primary key column '" + primaryKeyColumn + "'' in table ''{0}''", tableName);
        }
    }

    private void renameColumnAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Renaming columns is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.severe("ModifyDialog: Attempted to rename column in Inventory table, which is not allowed");
            return;
        }

        String oldColumnName = (String) JOptionPane.showInputDialog(
            this,
            "Select column to rename:",
            "Rename Column",
            JOptionPane.PLAIN_MESSAGE,
            null,
            columnNames,
            columnNames[0]
        );
        if (oldColumnName != null && !oldColumnName.equals(primaryKeyColumn)) {
            String newColumnName = JOptionPane.showInputDialog(this, "Enter new column name for " + oldColumnName + ":");
            if (newColumnName != null && !newColumnName.trim().isEmpty()) {
                newColumnName = newColumnName.trim();
                try (Connection conn = DatabaseUtils.getConnection()) {
                    String sql = "ALTER TABLE [" + tableName + "] RENAME COLUMN [" + oldColumnName + "] TO [" + newColumnName + "]";
                    conn.createStatement().executeUpdate(sql);
                    JOptionPane.showMessageDialog(this, "Column renamed successfully");
                    SwingUtilities.invokeLater(() -> {
                        tableManager.setTableName(tableName);
                        tableManager.refreshDataAndTabs();
                    });
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error renaming column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.log(Level.SEVERE, "ModifyDialog: SQLException renaming column ''{0}'' to ''{1}'' in table ''{2}'': {3}", new Object[]{oldColumnName, newColumnName, tableName, e.getMessage()});
                }
            }
        } else if (oldColumnName != null && oldColumnName.equals(primaryKeyColumn)) {
            JOptionPane.showMessageDialog(this, "Error: Cannot rename the primary key column '" + primaryKeyColumn + "'", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE,"ModifyDialog: Attempted to rename primary key column '" + primaryKeyColumn + "'' in table ''{0}''", tableName);
        }
    }

    public static void showModifyDialog(JFrame parent, HashMap<String, String> device, TableManager tableManager) {
        ModifyRowEntry dialog = new ModifyRowEntry(parent, device, null, tableManager);
        dialog.setVisible(true);
    }
}