package view_inventorytab;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import utils.DataUtils;
import utils.DatabaseUtils;
import utils.DefaultColumns;
import utils.UIComponentUtils;

public class ModifyDialog {
    private final JDialog dialog;
    private final HashMap<String, String> device;
    private final String[] columnNames = DefaultColumns.getInventoryColumns();
    private final JComponent[] inputs;
    private final HashMap<String, String> originalValues;

    public ModifyDialog(HashMap<String, String> device, String deviceType) {
        this.device = device;
        this.originalValues = new HashMap<>(device);

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.inputs = new JComponent[columnNames.length];

        Map<String, String> columnDefs = DefaultColumns.getInventoryColumnDefinitions();
        for (int i = 0; i < columnNames.length; i++) {
            String fieldName = columnNames[i];
            JComponent input;
            String key = fieldName;
            String type = columnDefs.getOrDefault(key, "TEXT");
            switch (type) {
                case "DATE":
                    JPanel datePicker = UIComponentUtils.createFormattedDatePicker();
                    JTextField dateField = (JTextField) datePicker.getComponent(0);
                    dateField.setText(device.getOrDefault(key, ""));
                    input = datePicker;
                    break;
                case "DOUBLE":
                    JTextField doubleField = UIComponentUtils.createFormattedTextField();
                    doubleField.setText(device.getOrDefault(key, ""));
                    input = doubleField;
                    break;
                default:
                    JTextField textField = UIComponentUtils.createFormattedTextField();
                    textField.setText(device.getOrDefault(key, ""));
                    if (fieldName.equals("AssetName")) {
                        textField.setEditable(false);
                    }
                    input = textField;
                    break;
            }
            panel.add(UIComponentUtils.createAlignedLabel(fieldName + ":"));
            panel.add(input);
            inputs[i] = input;
        }

        JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        dialog = new JDialog();
        dialog.setTitle("Modify Device");
        dialog.setModal(true);
        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setSize(500, Math.min(600, 50 + 40 * columnNames.length));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(null);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = UIComponentUtils.createFormattedButton("Save");
        JButton cancelButton = UIComponentUtils.createFormattedButton("Cancel");

        saveButton.addActionListener(e -> saveAction());
        cancelButton.addActionListener(e -> cancelAction());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void showDialog() {
        dialog.setVisible(true);
    }

    public static void showModifyDialog(JFrame parent, HashMap<String, String> device) {
        String deviceType = device.getOrDefault("Device_Type", "Unknown");
        ModifyDialog modifyDialog = new ModifyDialog(device, deviceType);
        modifyDialog.dialog.setLocationRelativeTo(parent);
        modifyDialog.showDialog();
    }

    private void saveAction() {
        HashMap<String, String> updatedDevice = new HashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            String key = columnNames[i];
            String value;
            if (inputs[i] instanceof JTextField) {
                value = ((JTextField) inputs[i]).getText();
            } else {
                value = UIComponentUtils.getDateFromPicker((JPanel) inputs[i]);
            }
            updatedDevice.put(key, value);
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

        String assetName = device.get("AssetName");
        String error = DataUtils.validateDevice(updatedDevice, assetName);
        if (error != null) {
            JOptionPane.showMessageDialog(dialog, "Error: " + error, "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (assetName != null) {
                DatabaseUtils.deleteDevice(assetName);
                DatabaseUtils.saveDevice(updatedDevice);
                JOptionPane.showMessageDialog(dialog, "Device updated successfully");
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Error: Asset Name not found", "Update Error", JOptionPane.ERROR_MESSAGE);
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
            } else {
                currentValue = UIComponentUtils.getDateFromPicker((JPanel) inputs[i]);
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
}