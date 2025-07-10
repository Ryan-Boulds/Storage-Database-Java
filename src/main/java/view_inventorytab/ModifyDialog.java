package view_inventorytab;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import utils.DataUtils;
import utils.FileUtils;
import utils.UIComponentUtils;

public class ModifyDialog {
    private final JDialog dialog;
    private final HashMap<String, String> device;
    private final java.util.List<String> columnNames;
    private final JComponent[] inputs;
    private final HashMap<String, String> originalValues;

    public ModifyDialog(HashMap<String, String> device, String deviceType) {
        this.device = device;
        this.originalValues = new HashMap<>(device);

        // Define default categories
        java.util.List<String> defaultCategories = Arrays.asList(
            "Device Name", "Device Type", "Serial Number", "Status", "Department", "Added Memory",
            "Added Storage", "Purchase Date", "Warranty Expiry", "Purchase Cost", "Vendor", "Assigned User",
            "OS Version", "Room", "Desk", "Maintenance Due", "Storage Capacity", "Memory Space",
            "Warranty Guarantee", "Acquisition Cost", "Supplier", "Operating System", "Specification Details",
            "Purchase Expense", "Storage Expansion", "Brand", "Manufacturer", "Building Location",
            "Site", "Division", "Team", "Unit", "Test", "Trial", "Experiment", "Evaluation", "Check",
            "Review", "Assessment"
        );

        // Determine dynamic fields
        this.columnNames = new ArrayList<>();
        Set<String> allKeys = new HashSet<>(device.keySet());
        allKeys.addAll(defaultCategories.stream().map(s -> s.replace(" ", "_")).collect(Collectors.toSet()));
        for (String key : allKeys) {
            columnNames.add(key.replace("_", " "));
        }
        Collections.sort(columnNames);

        // Create dialog
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        this.inputs = new JComponent[columnNames.size()];

        for (int i = 0; i < columnNames.size(); i++) {
            String fieldName = columnNames.get(i);
            JComponent input;
            String key = fieldName.replace(" ", "_");
            if (fieldName.equals("Status")) {
                JComboBox<String> combo = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
                combo.setSelectedItem(device.getOrDefault(key, "Deployed"));
                input = combo;
            } else if (fieldName.equals("Added Memory") || fieldName.equals("Added Storage")) {
                JComboBox<String> combo = UIComponentUtils.createFormattedComboBox(new String[]{"TRUE", "FALSE", "null"});
                combo.setSelectedItem(device.getOrDefault(key, "null"));
                input = combo;
            } else if (fieldName.contains("Date")) {
                JPanel datePicker = UIComponentUtils.createFormattedDatePicker();
                JTextField dateField = (JTextField) datePicker.getComponent(0);
                dateField.setText(device.getOrDefault(key, ""));
                input = datePicker;
            } else {
                JTextField field = UIComponentUtils.createFormattedTextField();
                field.setText(device.getOrDefault(key, ""));
                if (fieldName.equals("Serial Number") || fieldName.equals("Device Type")) {
                    field.setEditable(false);
                }
                input = field;
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
        dialog.setSize(500, Math.min(600, 50 + 40 * columnNames.size()));
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

    private void saveAction() {
        HashMap<String, String> updatedDevice = new HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            String key = columnNames.get(i).replace(" ", "_");
            String value;
            if (inputs[i] instanceof JTextField) {
                value = ((JTextField) inputs[i]).getText();
            } else if (inputs[i] instanceof JComboBox) {
                value = (String) ((JComboBox<?>) inputs[i]).getSelectedItem();
            } else {
                JTextField dateField = (JTextField) ((JPanel) inputs[i]).getComponent(0);
                value = dateField.getText();
                System.out.println("[DEBUG] Saving " + key + ": " + value);
            }
            updatedDevice.put(key, value);
        }

        System.out.println("[DEBUG] Updated device before validation: " + updatedDevice);

        boolean hasChanges = false;
        for (String key : updatedDevice.keySet()) {
            String original = originalValues.getOrDefault(key, "");
            String updated = updatedDevice.getOrDefault(key, "");
            if (!original.equals(updated)) {
                hasChanges = true;
                System.out.println("[DEBUG] Change detected in " + key + ": " + original + " -> " + updated);
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
        }

        String error = DataUtils.validateDevice(updatedDevice, device.get("Serial_Number"));
        if (error != null) {
            JOptionPane.showMessageDialog(dialog, "Error: " + error, "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean updated = false;
        for (int i = 0; i < utils.InventoryData.getDevices().size(); i++) {
            HashMap<String, String> d = utils.InventoryData.getDevices().get(i);
            if (d.get("Serial_Number").equals(device.get("Serial_Number"))) {
                utils.InventoryData.getDevices().set(i, updatedDevice);
                updated = true;
                System.out.println("[DEBUG] Updated device in InventoryData: " + updatedDevice);
                break;
            }
        }
        if (!updated) {
            JOptionPane.showMessageDialog(dialog, "Error: Device not found for update", "Update Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        FileUtils.saveDevices();
        System.out.println("[DEBUG] After saveDevices, InventoryData.getDevices(): " + utils.InventoryData.getDevices());
        dialog.dispose();
    }

    private void cancelAction() {
        boolean hasChanges = false;
        for (int i = 0; i < columnNames.size(); i++) {
            String key = columnNames.get(i).replace(" ", "_");
            String currentValue;
            if (inputs[i] instanceof JTextField) {
                currentValue = ((JTextField) inputs[i]).getText();
            } else if (inputs[i] instanceof JComboBox) {
                currentValue = (String) ((JComboBox<?>) inputs[i]).getSelectedItem();
            } else {
                currentValue = ((JTextField) ((JPanel) inputs[i]).getComponent(0)).getText();
            }
            String originalValue = originalValues.getOrDefault(key, "");
            if (!currentValue.equals(originalValue)) {
                hasChanges = true;
                System.out.println("[DEBUG] Cancel change detected in " + key + ": " + originalValue + " -> " + currentValue);
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