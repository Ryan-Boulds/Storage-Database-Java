package view_inventory_tab;

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
    private final HashMap<String, String> device;
    private final String[] columnNames;
    private final Map<String, Integer> columnTypes;
    private final JComponent[] inputs;
    private final HashMap<String, String> originalValues;
    private final JFrame parent;
    private final TableManager tableManager;
    private final String primaryKeyColumn = "AssetName";

    public ModifyRowEntry(JFrame parent, HashMap<String, String> device, String deviceType, TableManager tableManager) {
        super(parent, "Modify Row Entry", true);
        this.parent = parent;
        this.device = new HashMap<>(device);
        this.originalValues = new HashMap<>(device);
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
            System.out.println("ModifyDialog: Column " + key + " SQL type: " + sqlType);

            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0;
            JPanel labelPanel = new JPanel(new BorderLayout());
            labelPanel.add(UIComponentUtils.createAlignedLabel(fieldName + ":"), BorderLayout.WEST);
            labelPanel.setPreferredSize(new java.awt.Dimension(maxLabelWidth, 30));
            panel.add(labelPanel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            if (null == sqlType) {
                JTextField textField = UIComponentUtils.createFormattedTextField();
                textField.setText(device.getOrDefault(key, ""));
                textField.setPreferredSize(new java.awt.Dimension(200, 30));
                if (fieldName.equals(primaryKeyColumn)) {
                    textField.setEditable(false);
                    textField.setBackground(Color.LIGHT_GRAY);
                }
                input = textField;
            } else switch (sqlType) {
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
                case Types.NUMERIC:
                    JTextField doubleField = UIComponentUtils.createFormattedTextField();
                    doubleField.setText(device.getOrDefault(key, ""));
                    doubleField.setPreferredSize(new java.awt.Dimension(200, 30));
                    input = doubleField;
                    break;
                case Types.BIT:
                case Types.BOOLEAN:
                    JCheckBox checkBox = new JCheckBox();
                    checkBox.setSelected(Boolean.parseBoolean(device.getOrDefault(key, "false")));
                    checkBox.setPreferredSize(new java.awt.Dimension(200, 30));
                    input = checkBox;
                    break;
                default:
                    JTextField textField = UIComponentUtils.createFormattedTextField();
                    textField.setText(device.getOrDefault(key, ""));
                    textField.setPreferredSize(new java.awt.Dimension(200, 30));
                    if (fieldName.equals(primaryKeyColumn)) {
                        textField.setEditable(false);
                        textField.setBackground(Color.LIGHT_GRAY);
                    }   input = textField;
                    break;
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
        cancelButton.addActionListener(e -> cancelAction());
        buttonPanel.add(cancelButton);

        JButton deleteButton = new JButton("Delete Entry");
        deleteButton.addActionListener(e -> deleteAction());
        buttonPanel.add(deleteButton);

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
            JOptionPane.showMessageDialog(this, "Error: Editing is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("ModifyDialog: Attempted to edit in Inventory table, which is not allowed");
            return;
        }

        HashMap<String, String> updatedDevice = new HashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            String key = columnNames[i];
            String value;
            if (inputs[i] instanceof JTextField) {
                value = ((JTextField) inputs[i]).getText();
            } else if (inputs[i] instanceof JPanel) {
                value = UIComponentUtils.getDateFromPicker((JPanel) inputs[i]);
            } else if (inputs[i] instanceof JCheckBox) {
                value = String.valueOf(((JCheckBox) inputs[i]).isSelected());
            } else {
                value = "";
            }
            updatedDevice.put(key, value);
        }

        if (updatedDevice.equals(originalValues)) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "No changes detected. Do you want to close the dialog?",
                "No Changes",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                this.dispose();
            }
            return;
        }

        String primaryKey = device.get(primaryKeyColumn);
        String error = DataUtils.validateDevice(updatedDevice, primaryKey);
        if (error != null) {
            JOptionPane.showMessageDialog(this, "Error: " + error, "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (primaryKey != null) {
                DatabaseUtils.deleteDevice(tableName, primaryKey);
                DatabaseUtils.saveDevice(tableName, updatedDevice);
                JOptionPane.showMessageDialog(this, "Device updated successfully");
                this.dispose();
                SwingUtilities.invokeLater(() -> {
                    if (tableManager != null) {
                        System.out.println("Refreshing table after modify for " + primaryKey + " in table '" + tableName + "'");
                        tableManager.refreshDataAndTabs();
                    } else {
                        System.err.println("Error: TableManager is null during refresh");
                    }
                });
            } else {
                JOptionPane.showMessageDialog(this, "Error: " + primaryKeyColumn + " not found", "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating device: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("ModifyDialog: SQLException in table '" + tableName + "': " + e.getMessage());
        }
    }

    private void cancelAction() {
        boolean hasChanges = false;
        for (int i = 0; i < columnNames.length; i++) {
            String key = columnNames[i];
            String currentValue;
            if (inputs[i] instanceof JTextField) {
                currentValue = ((JTextField) inputs[i]).getText();
            } else if (inputs[i] instanceof JPanel) {
                currentValue = UIComponentUtils.getDateFromPicker((JPanel) inputs[i]);
            } else if (inputs[i] instanceof JCheckBox) {
                currentValue = String.valueOf(((JCheckBox) inputs[i]).isSelected());
            } else {
                currentValue = "";
            }
            String originalValue = originalValues.getOrDefault(key, "");
            if (!currentValue.equals(originalValue)) {
                hasChanges = true;
                break;
            }
        }

        if (hasChanges) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel? Any changes made will be lost.",
                "Confirm Cancel",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        this.dispose();
    }

    private void deleteAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Deletion is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("ModifyDialog: Attempted to delete in Inventory table, which is not allowed");
            return;
        }

        String primaryKey = device.get(primaryKeyColumn);
        if (primaryKey != null) {
            JOptionPane optionPane = new JOptionPane(
                "Are you sure you want to delete device with " + primaryKeyColumn + ": " + primaryKey + "?",
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION
            );
            JDialog confirmDialog = optionPane.createDialog(parent, "Confirm Delete");
            confirmDialog.setLocationRelativeTo(parent);
            confirmDialog.setVisible(true);
            Integer confirm = (Integer) optionPane.getValue();
            if (confirm != null && confirm == JOptionPane.YES_OPTION) {
                try {
                    System.out.println("Deleting device with " + primaryKeyColumn + ": " + primaryKey + " from table '" + tableName + "'");
                    DatabaseUtils.deleteDevice(tableName, primaryKey);
                    JOptionPane.showMessageDialog(parent, "Device deleted successfully");
                    this.dispose();
                    SwingUtilities.invokeLater(() -> {
                        if (tableManager != null) {
                            System.out.println("Refreshing table after delete for " + primaryKey + " in table '" + tableName + "'");
                            tableManager.refreshDataAndTabs();
                        } else {
                            System.err.println("Error: TableManager is null during refresh");
                        }
                    });
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(parent, "Error deleting device: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("ModifyDialog: SQLException in table '" + tableName + "': " + e.getMessage());
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Error: " + primaryKeyColumn + " not found", "Delete Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addColumnAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Adding columns is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("ModifyDialog: Attempted to add column in Inventory table, which is not allowed");
            return;
        }

        String newColumnName = JOptionPane.showInputDialog(this, "Enter new column name:");
        if (newColumnName != null && !newColumnName.trim().isEmpty()) {
            newColumnName = newColumnName.trim();
            try (Connection conn = DatabaseUtils.getConnection()) {
                String sql = "ALTER TABLE " + tableName + " ADD " + newColumnName + " VARCHAR(255)";
                conn.createStatement().executeUpdate(sql);
                JOptionPane.showMessageDialog(this, "Column added successfully");
                SwingUtilities.invokeLater(() -> {
                    tableManager.setTableName(tableName);
                    tableManager.refreshDataAndTabs();
                });
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("ModifyDialog: SQLException adding column to table '" + tableName + "': " + e.getMessage());
            }
        }
    }

    private void deleteColumnAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Deleting columns is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("ModifyDialog: Attempted to delete column in Inventory table, which is not allowed");
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
                    String sql = "ALTER TABLE " + tableName + " DROP COLUMN " + columnToDelete;
                    conn.createStatement().executeUpdate(sql);
                    JOptionPane.showMessageDialog(this, "Column '" + columnToDelete + "' deleted successfully");
                    SwingUtilities.invokeLater(() -> {
                        tableManager.setTableName(tableName);
                        tableManager.refreshDataAndTabs();
                    });
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error deleting column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("ModifyDialog: SQLException deleting column in table '" + tableName + "': " + e.getMessage());
                }
            }
        } else if (columnToDelete != null && columnToDelete.equals(primaryKeyColumn)) {
            JOptionPane.showMessageDialog(this, "Error: Cannot delete the primary key column '" + primaryKeyColumn + "'", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("ModifyDialog: Attempted to delete primary key column '" + primaryKeyColumn + "' in table '" + tableName + "'");
        }
    }

    private void renameColumnAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Renaming columns is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("ModifyDialog: Attempted to rename column in Inventory table, which is not allowed");
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
                    String sql = "ALTER TABLE " + tableName + " RENAME COLUMN " + oldColumnName + " TO " + newColumnName;
                    conn.createStatement().executeUpdate(sql);
                    JOptionPane.showMessageDialog(this, "Column renamed successfully");
                    SwingUtilities.invokeLater(() -> {
                        tableManager.setTableName(tableName);
                        tableManager.refreshDataAndTabs();
                    });
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error renaming column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("ModifyDialog: SQLException renaming column in table '" + tableName + "': " + e.getMessage());
                }
            }
        }
    }

    public static void showModifyDialog(JFrame parent, HashMap<String, String> device, TableManager tableManager) {
        ModifyRowEntry dialog = new ModifyRowEntry(parent, device, null, tableManager);
        dialog.setVisible(true);
    }
}