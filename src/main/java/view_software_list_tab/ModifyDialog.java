package view_software_list_tab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
                }
                input = textField;
            }
            panel.add(input, gbc);
            inputs[i] = input;
        }

        JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        dialog = new JDialog(parent, "Modify Device", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setSize(800, Math.min(600, 100 + 40 * columnNames.length));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(parent);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel rightPanel = new JPanel(new BorderLayout());

        JButton saveButton = UIComponentUtils.createFormattedButton("Save");
        JButton cancelButton = UIComponentUtils.createFormattedButton("Cancel");
        JButton deleteButton = new JButton("Delete Device");
        deleteButton.setBackground(Color.RED);
        deleteButton.setForeground(Color.WHITE);

        saveButton.addActionListener(e -> saveAction());
        cancelButton.addActionListener(e -> cancelAction());
        deleteButton.addActionListener(e -> deleteAction());

        leftPanel.add(saveButton, BorderLayout.WEST);
        rightPanel.add(cancelButton, BorderLayout.EAST);
        rightPanel.add(deleteButton, BorderLayout.WEST);
        buttonPanel.add(leftPanel, BorderLayout.WEST);
        buttonPanel.add(rightPanel, BorderLayout.EAST);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void showDialog() {
        dialog.setVisible(true);
    }

    public static void showModifyDialog(JFrame parent, HashMap<String, String> device, TableManager tableManager) {
        String deviceType = device.getOrDefault("Device_Type", "Unknown");
        ModifyDialog modifyDialog = new ModifyDialog(parent, device, deviceType, tableManager);
        modifyDialog.showDialog();
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
            System.out.println("ModifyDialog: Saving column " + key + " = " + value);
        }

        boolean hasChanges = false;
        for (String key : updatedDevice.keySet()) {
            String original = originalValues.getOrDefault(key, "");
            String updated = updatedDevice.getOrDefault(key, "");
            if (!original.equals(updated)) {
                hasChanges = true;
                break;
            }
        }

        if (hasChanges) {
            int confirm = JOptionPane.showConfirmDialog(
                dialog,
                "Are you sure you want to save?",
                "Confirm Save",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) {
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