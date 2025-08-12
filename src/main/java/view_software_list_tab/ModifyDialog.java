package view_software_list_tab;

import java.awt.BorderLayout;
import java.awt.Color;
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

public class ModifyDialog {
    private final JDialog dialog;
    private final HashMap<String, String> device;
    private final String[] columnNames;
    private final Map<String, Integer> columnTypes;
    private final JComponent[] inputs;
    private final HashMap<String, String> originalValues;
    private final JFrame parent;
    private final TableManager tableManager;
    private final String primaryKeyColumn = "AssetName";

    public ModifyDialog(JFrame parent, HashMap<String, String> device, String deviceType, TableManager tableManager) {
        this.parent = parent;
        this.device = new HashMap<>(device);
        this.originalValues = new HashMap<>(device);
        this.tableManager = tableManager;
        this.columnNames = tableManager.getColumns();
        this.columnTypes = tableManager.getColumnTypes();

        dialog = new JDialog(parent, "Modify Device", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(600, 800);
        dialog.setLocationRelativeTo(parent);

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
            if (sqlType == Types.DATE || sqlType == Types.TIMESTAMP || 
                key.equals("Warranty_Expiry_Date") || key.equals("Last_Maintenance") || 
                key.equals("Maintenance_Due") || key.equals("Date_Of_Purchase")) {
                JPanel datePicker = UIComponentUtils.createFormattedDatePicker();
                JTextField dateField = (JTextField) datePicker.getComponent(0);
                dateField.setText(device.getOrDefault(key, ""));
                dateField.setPreferredSize(new java.awt.Dimension(200, 30));
                input = datePicker;
            } else if (sqlType == Types.DOUBLE || sqlType == Types.FLOAT || 
                       sqlType == Types.DECIMAL || sqlType == Types.NUMERIC || 
                       key.equals("Purchase_Cost")) {
                JTextField doubleField = UIComponentUtils.createFormattedTextField();
                doubleField.setText(device.getOrDefault(key, ""));
                doubleField.setPreferredSize(new java.awt.Dimension(200, 30));
                input = doubleField;
            } else if (sqlType == Types.BIT || sqlType == Types.BOOLEAN || 
                       key.equals("Added_Memory")) {
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(Boolean.parseBoolean(device.getOrDefault(key, "false")));
                checkBox.setPreferredSize(new java.awt.Dimension(200, 30));
                input = checkBox;
            } else {
                JTextField textField = UIComponentUtils.createFormattedTextField();
                textField.setText(device.getOrDefault(key, ""));
                textField.setPreferredSize(new java.awt.Dimension(200, 30));
                if (fieldName.equals(primaryKeyColumn)) {
                    textField.setEditable(false);
                    textField.setBackground(Color.LIGHT_GRAY);
                }
                input = textField;
            }
            inputs[i] = input;
            panel.add(input, gbc);
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveAction());
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancelAction());
        buttonPanel.add(cancelButton);

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteAction());
        buttonPanel.add(deleteButton);

        JButton addColumnButton = new JButton("Add Column");
        addColumnButton.addActionListener(e -> addColumnAction());
        buttonPanel.add(addColumnButton);

        JButton renameColumnButton = new JButton("Rename Column");
        renameColumnButton.addActionListener(e -> renameColumnAction());
        buttonPanel.add(renameColumnButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    public static void showModifyDialog(JFrame parent, HashMap<String, String> device, TableManager tableManager) {
        new ModifyDialog(parent, device, "Inventory", tableManager);
    }

    private void addColumnAction() {
        String columnName = JOptionPane.showInputDialog(dialog, "Enter new column name:");
        if (columnName != null && !columnName.trim().isEmpty()) {
            String tableName = tableManager.getTableName();
            String sql = "ALTER TABLE [" + tableName + "] ADD COLUMN [" + columnName + "] VARCHAR(255)";
            try (Connection conn = DatabaseUtils.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(dialog, "Column added successfully");
                dialog.dispose();
                SwingUtilities.invokeLater(() -> {
                    tableManager.setTableName(tableName);
                    tableManager.refreshDataAndTabs();
                });
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Error adding column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void renameColumnAction() {
        String oldName = (String) JOptionPane.showInputDialog(dialog, "Select column to rename:", "Rename Column", JOptionPane.PLAIN_MESSAGE, null, columnNames, columnNames[0]);
        if (oldName != null) {
            if (oldName.equals(primaryKeyColumn)) {
                JOptionPane.showMessageDialog(dialog, "Cannot rename primary key column", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String newName = JOptionPane.showInputDialog(dialog, "Enter new name for " + oldName + ":");
            if (newName != null && !newName.trim().isEmpty()) {
                String tableName = tableManager.getTableName();
                String sql = "ALTER TABLE [" + tableName + "] RENAME COLUMN [" + oldName + "] TO [" + newName + "]";
                try (Connection conn = DatabaseUtils.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(dialog, "Column renamed successfully");
                    dialog.dispose();
                    SwingUtilities.invokeLater(() -> {
                        tableManager.setTableName(tableName);
                        tableManager.refreshDataAndTabs();
                    });
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(dialog, "Error renaming column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void saveAction() {
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
                dialog,
                "No changes detected. Do you want to close the dialog?",
                "No Changes",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                dialog.dispose();
                return;
            }
        } else {
            dialog.dispose();
            return;
        }

        String primaryKey = device.get(primaryKeyColumn);
        String error = DataUtils.validateDevice(updatedDevice, primaryKey);
        if (error != null) {
            JOptionPane.showMessageDialog(dialog, "Error: " + error, "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (primaryKey != null) {
                DatabaseUtils.deleteDevice("Inventory", primaryKey);
                DatabaseUtils.saveDevice("Inventory", updatedDevice);
                JOptionPane.showMessageDialog(dialog, "Device updated successfully");
                dialog.dispose();
                SwingUtilities.invokeLater(() -> {
                    if (tableManager != null) {
                        System.out.println("Refreshing table after modify for " + primaryKey);
                        tableManager.refreshDataAndTabs();
                    } else {
                        System.err.println("Error: TableManager is null during refresh");
                    }
                });
            } else {
                JOptionPane.showMessageDialog(dialog, "Error: " + primaryKeyColumn + " not found", "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(dialog, "Error updating device: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
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
                dialog,
                "Are you sure you want to cancel? Any changes made will be lost.",
                "Confirm Cancel",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dialog.dispose();
    }

    private void deleteAction() {
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
                    System.out.println("Deleting device with " + primaryKeyColumn + ": " + primaryKey);
                    DatabaseUtils.deleteDevice("Inventory", primaryKey);
                    JOptionPane.showMessageDialog(parent, "Device deleted successfully");
                    dialog.dispose();
                    SwingUtilities.invokeLater(() -> {
                        if (tableManager != null) {
                            System.out.println("Refreshing table after delete for " + primaryKey);
                            tableManager.refreshDataAndTabs();
                        } else {
                            System.err.println("Error: TableManager is null during refresh");
                        }
                    });
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(parent, "Error deleting device: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(dialog, "Error: " + primaryKeyColumn + " not found", "Delete Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}