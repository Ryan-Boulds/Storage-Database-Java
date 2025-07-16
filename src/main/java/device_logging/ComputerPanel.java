package device_logging;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import utils.DataUtils;
import utils.InventoryData;
import utils.UIComponentUtils;

public class ComputerPanel extends JPanel {

    private final JTextField deviceNameField = UIComponentUtils.createFormattedTextField();
    private final JTextField brandField = UIComponentUtils.createFormattedTextField();
    private final JTextField modelField = UIComponentUtils.createFormattedTextField();
    private final JTextField serialNumberField = UIComponentUtils.createFormattedTextField();
    private final JTextField buildingLocationField = UIComponentUtils.createFormattedTextField();
    private final JTextField roomDeskField = UIComponentUtils.createFormattedTextField();
    private final JTextField specificationField = UIComponentUtils.createFormattedTextField();
    private final JTextField processorTypeField = UIComponentUtils.createFormattedTextField();
    private final JTextField storageCapacityField = UIComponentUtils.createFormattedTextField();
    private final JTextField networkAddressField = UIComponentUtils.createFormattedTextField();
    private final JTextField osVersionField = UIComponentUtils.createFormattedTextField();
    private final JTextField departmentField = UIComponentUtils.createFormattedTextField();
    private final JComboBox<String> addedMemoryField = UIComponentUtils.createFormattedComboBox(new String[]{"TRUE", "FALSE", "null"});
    private final JComboBox<String> statusField = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
    private final JTextField assignedUserField = UIComponentUtils.createFormattedTextField();
    private final JTextField warrantyExpiryField = UIComponentUtils.createFormattedTextField();
    private final JTextField lastMaintenanceField = UIComponentUtils.createFormattedTextField();
    private final JTextField maintenanceDueField = UIComponentUtils.createFormattedTextField();
    private final JTextField dateOfPurchaseField = UIComponentUtils.createFormattedTextField();
    private final JTextField purchaseCostField = UIComponentUtils.createFormattedTextField();
    private final JTextField vendorField = UIComponentUtils.createFormattedTextField();
    private final JTextField memoryRamField = UIComponentUtils.createFormattedTextField();
    private final JLabel statusLabel;

    public ComputerPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        setLayout(new GridLayout(0, 2, 5, 5));

        add(UIComponentUtils.createAlignedLabel("Device Name:"));
        add(deviceNameField);
        add(UIComponentUtils.createAlignedLabel("Brand:"));
        add(brandField);
        add(UIComponentUtils.createAlignedLabel("Model:"));
        add(modelField);
        add(UIComponentUtils.createAlignedLabel("Serial Number:"));
        add(serialNumberField);
        add(UIComponentUtils.createAlignedLabel("Building Location:"));
        add(buildingLocationField);
        add(UIComponentUtils.createAlignedLabel("Room/Desk:"));
        add(roomDeskField);
        add(UIComponentUtils.createAlignedLabel("Specification:"));
        add(specificationField);
        add(UIComponentUtils.createAlignedLabel("Processor Type:"));
        add(processorTypeField);
        add(UIComponentUtils.createAlignedLabel("Storage Capacity:"));
        add(storageCapacityField);
        add(UIComponentUtils.createAlignedLabel("Network Address:"));
        add(networkAddressField);
        add(UIComponentUtils.createAlignedLabel("OS Version:"));
        add(osVersionField);
        add(UIComponentUtils.createAlignedLabel("Department:"));
        add(departmentField);
        add(UIComponentUtils.createAlignedLabel("Added Memory:"));
        add(addedMemoryField);
        add(UIComponentUtils.createAlignedLabel("Status:"));
        add(statusField);
        add(UIComponentUtils.createAlignedLabel("Assigned User:"));
        add(assignedUserField);
        add(UIComponentUtils.createAlignedLabel("Warranty Expiry Date:"));
        add(warrantyExpiryField);
        add(UIComponentUtils.createAlignedLabel("Last Maintenance:"));
        add(lastMaintenanceField);
        add(UIComponentUtils.createAlignedLabel("Maintenance Due:"));
        add(maintenanceDueField);
        add(UIComponentUtils.createAlignedLabel("Date of Purchase:"));
        add(dateOfPurchaseField);
        add(UIComponentUtils.createAlignedLabel("Purchase Cost:"));
        add(purchaseCostField);
        add(UIComponentUtils.createAlignedLabel("Vendor:"));
        add(vendorField);
        add(UIComponentUtils.createAlignedLabel("Memory (RAM):"));
        add(memoryRamField);

        JButton saveButton = UIComponentUtils.createFormattedButton("Save Device");
        saveButton.addActionListener(e -> saveDevice());

        JButton saveTemplateButton = UIComponentUtils.createFormattedButton("Save as Template");
        saveTemplateButton.addActionListener(e -> saveTemplate());

        JComboBox<String> templateComboBox = UIComponentUtils.createFormattedComboBox(new String[]{});
        templateComboBox.addActionListener(e -> loadTemplate((String) templateComboBox.getSelectedItem()));

        JButton loadTemplateButton = UIComponentUtils.createFormattedButton("Load Template");
        loadTemplateButton.addActionListener(e -> {
            ArrayList<String> templateNames = InventoryData.getTemplates();
            if (templateNames.isEmpty()) {
                statusLabel.setText("No templates available");
                return;
            }
            JComboBox<String> templateCombo = UIComponentUtils.createFormattedComboBox(templateNames.toArray(new String[0]));
            int result = JOptionPane.showConfirmDialog(null, templateCombo, "Select Template", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String selected = (String) templateCombo.getSelectedItem();
                HashMap<String, String> template = InventoryData.getTemplateDetails(selected);
                if (template != null) {
                    loadTemplate(selected);
                    statusLabel.setText("Template loaded");
                } else {
                    statusLabel.setText("Template not found");
                }
            }
        });

        add(saveButton);
        add(saveTemplateButton);
        add(templateComboBox);
        add(loadTemplateButton);
    }

    private void saveDevice() {
        HashMap<String, String> deviceData = new HashMap<>();
        deviceData.put("Device_Name", DataUtils.capitalizeWords(deviceNameField.getText()));
        deviceData.put("Device_Type", "Computer");
        deviceData.put("Brand", DataUtils.capitalizeWords(brandField.getText()));
        deviceData.put("Model", DataUtils.capitalizeWords(modelField.getText()));
        deviceData.put("Serial_Number", serialNumberField.getText());
        deviceData.put("Building_Location", DataUtils.capitalizeWords(buildingLocationField.getText()));
        deviceData.put("Room_Desk", DataUtils.capitalizeWords(roomDeskField.getText()));
        deviceData.put("Specification", specificationField.getText());
        deviceData.put("Processor_Type", processorTypeField.getText());
        deviceData.put("Storage_Capacity", storageCapacityField.getText());
        deviceData.put("Network_Address", networkAddressField.getText());
        deviceData.put("OS_Version", osVersionField.getText());
        deviceData.put("Department", DataUtils.capitalizeWords(departmentField.getText()));
        deviceData.put("Added_Memory", (String) addedMemoryField.getSelectedItem());
        deviceData.put("Status", (String) statusField.getSelectedItem());
        deviceData.put("Assigned_User", DataUtils.capitalizeWords(assignedUserField.getText()));
        deviceData.put("Warranty_Expiry_Date", warrantyExpiryField.getText());
        deviceData.put("Last_Maintenance", lastMaintenanceField.getText());
        deviceData.put("Maintenance_Due", maintenanceDueField.getText());
        deviceData.put("Date_Of_Purchase", dateOfPurchaseField.getText());
        deviceData.put("Purchase_Cost", purchaseCostField.getText());
        deviceData.put("Vendor", DataUtils.capitalizeWords(vendorField.getText()));
        deviceData.put("Memory_RAM", memoryRamField.getText());

        String error = DataUtils.validateDevice(deviceData);
        if (error != null) {
            statusLabel.setText(error);
            JOptionPane.showMessageDialog(this, error, "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            InventoryData.saveDevice(deviceData);
            statusLabel.setText("Device saved successfully!");
            JOptionPane.showMessageDialog(this, "Device saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
        } catch (RuntimeException e) {
            statusLabel.setText("Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveTemplate() {
        String templateName = JOptionPane.showInputDialog(this, "Enter template name:");
        if (templateName == null || templateName.trim().isEmpty()) {
            statusLabel.setText("Template name is required");
            JOptionPane.showMessageDialog(this, "Template name is required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        HashMap<String, String> templateData = new HashMap<>();
        templateData.put("Device_Name", DataUtils.capitalizeWords(deviceNameField.getText()));
        templateData.put("Device_Type", "Computer");
        templateData.put("Brand", DataUtils.capitalizeWords(brandField.getText()));
        templateData.put("Model", DataUtils.capitalizeWords(modelField.getText()));
        templateData.put("Serial_Number", serialNumberField.getText());
        templateData.put("Building_Location", DataUtils.capitalizeWords(buildingLocationField.getText()));
        templateData.put("Room_Desk", DataUtils.capitalizeWords(roomDeskField.getText()));
        templateData.put("Specification", specificationField.getText());
        templateData.put("Processor_Type", processorTypeField.getText());
        templateData.put("Storage_Capacity", storageCapacityField.getText());
        templateData.put("Network_Address", networkAddressField.getText());
        templateData.put("OS_Version", osVersionField.getText());
        templateData.put("Department", DataUtils.capitalizeWords(departmentField.getText()));
        templateData.put("Added_Memory", (String) addedMemoryField.getSelectedItem());
        templateData.put("Status", (String) statusField.getSelectedItem());
        templateData.put("Assigned_User", DataUtils.capitalizeWords(assignedUserField.getText()));
        templateData.put("Warranty_Expiry_Date", warrantyExpiryField.getText());
        templateData.put("Last_Maintenance", lastMaintenanceField.getText());
        templateData.put("Maintenance_Due", maintenanceDueField.getText());
        templateData.put("Date_Of_Purchase", dateOfPurchaseField.getText());
        templateData.put("Purchase_Cost", purchaseCostField.getText());
        templateData.put("Vendor", DataUtils.capitalizeWords(vendorField.getText()));
        templateData.put("Memory_RAM", memoryRamField.getText());

        try {
            InventoryData.saveTemplate(templateData, templateName);
            statusLabel.setText("Template saved successfully!");
            JOptionPane.showMessageDialog(this, "Template saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            statusLabel.setText("Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTemplate(String templateName) {
        if (templateName == null || templateName.trim().isEmpty()) {
            return;
        }

        try {
            HashMap<String, String> template = InventoryData.getTemplateDetails(templateName);
            if (!template.isEmpty()) {
                deviceNameField.setText(template.getOrDefault("Device_Name", ""));
                brandField.setText(template.getOrDefault("Brand", ""));
                modelField.setText(template.getOrDefault("Model", ""));
                serialNumberField.setText(template.getOrDefault("Serial_Number", ""));
                buildingLocationField.setText(template.getOrDefault("Building_Location", ""));
                roomDeskField.setText(template.getOrDefault("Room_Desk", ""));
                specificationField.setText(template.getOrDefault("Specification", ""));
                processorTypeField.setText(template.getOrDefault("Processor_Type", ""));
                storageCapacityField.setText(template.getOrDefault("Storage_Capacity", ""));
                networkAddressField.setText(template.getOrDefault("Network_Address", ""));
                osVersionField.setText(template.getOrDefault("OS_Version", ""));
                departmentField.setText(template.getOrDefault("Department", ""));
                addedMemoryField.setSelectedItem(template.getOrDefault("Added_Memory", "null"));
                statusField.setSelectedItem(template.getOrDefault("Status", "Deployed"));
                assignedUserField.setText(template.getOrDefault("Assigned_User", ""));
                warrantyExpiryField.setText(template.getOrDefault("Warranty_Expiry_Date", ""));
                lastMaintenanceField.setText(template.getOrDefault("Last_Maintenance", ""));
                maintenanceDueField.setText(template.getOrDefault("Maintenance_Due", ""));
                dateOfPurchaseField.setText(template.getOrDefault("Date_Of_Purchase", ""));
                purchaseCostField.setText(template.getOrDefault("Purchase_Cost", ""));
                vendorField.setText(template.getOrDefault("Vendor", ""));
                memoryRamField.setText(template.getOrDefault("Memory_RAM", ""));
                statusLabel.setText("Template loaded successfully!");
            } else {
                statusLabel.setText("Template not found!");
                JOptionPane.showMessageDialog(this, "Template not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (RuntimeException e) {
            statusLabel.setText("Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        deviceNameField.setText("");
        brandField.setText("");
        modelField.setText("");
        serialNumberField.setText("");
        buildingLocationField.setText("");
        roomDeskField.setText("");
        specificationField.setText("");
        processorTypeField.setText("");
        storageCapacityField.setText("");
        networkAddressField.setText("");
        osVersionField.setText("");
        departmentField.setText("");
        addedMemoryField.setSelectedItem("null");
        statusField.setSelectedItem("Deployed");
        assignedUserField.setText("");
        warrantyExpiryField.setText("");
        lastMaintenanceField.setText("");
        maintenanceDueField.setText("");
        dateOfPurchaseField.setText("");
        purchaseCostField.setText("");
        vendorField.setText("");
        memoryRamField.setText("");
    }
}