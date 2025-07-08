package device_logging;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import utils.DataUtils;
import utils.FileUtils;
import utils.InventoryData;
import utils.SQLGenerator;
import utils.UIComponentUtils;

public class ComputerPanel extends JPanel {
    private JTextField deviceNameField, deviceTypeField, brandField, modelField, serialNumberField,
                      processorTypeField, storageCapacityField, networkAddressField, departmentField,
                      purchaseCostField, vendorField, osVersionField, roomDeskField, specificationField,
                      assignedUserField, buildingLocationField, memoryRAMField;
    private JComboBox<String> addedMemoryCombo, addedStorageCombo, statusCombo;
    private JPanel warrantyExpiryDatePicker_div, maintenanceDatesPicker_div, lastMaintenancePicker_div,
                   dateOfPurchasePicker_div;
    private final JLabel statusLabel;

    public ComputerPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        initializeComponents();
        addComponents();
    }

    private void initializeComponents() {
        deviceNameField = UIComponentUtils.createFormattedTextField();
        deviceTypeField = UIComponentUtils.createFormattedTextField();
        brandField = UIComponentUtils.createFormattedTextField();
        modelField = UIComponentUtils.createFormattedTextField();
        serialNumberField = UIComponentUtils.createFormattedTextField();
        processorTypeField = UIComponentUtils.createFormattedTextField();
        storageCapacityField = UIComponentUtils.createFormattedTextField();
        networkAddressField = UIComponentUtils.createFormattedTextField();
        departmentField = UIComponentUtils.createFormattedTextField();
        purchaseCostField = UIComponentUtils.createFormattedTextField();
        vendorField = UIComponentUtils.createFormattedTextField();
        osVersionField = UIComponentUtils.createFormattedTextField();
        roomDeskField = UIComponentUtils.createFormattedTextField();
        specificationField = UIComponentUtils.createFormattedTextField();
        assignedUserField = UIComponentUtils.createFormattedTextField();
        buildingLocationField = UIComponentUtils.createFormattedTextField();
        memoryRAMField = UIComponentUtils.createFormattedTextField();
        addedMemoryCombo = UIComponentUtils.createFormattedComboBox(new String[]{"TRUE", "FALSE", "null"});
        addedStorageCombo = UIComponentUtils.createFormattedComboBox(new String[]{"TRUE", "FALSE", "null"});
        statusCombo = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
        warrantyExpiryDatePicker_div = UIComponentUtils.createFormattedDatePicker();
        lastMaintenancePicker_div = UIComponentUtils.createFormattedDatePicker();
        maintenanceDatesPicker_div = UIComponentUtils.createFormattedDatePicker();
        dateOfPurchasePicker_div = UIComponentUtils.createFormattedDatePicker();
    }

    private void addComponents() {
        JButton enterButton = UIComponentUtils.createFormattedButton("Enter");
        JButton saveTemplateButton = UIComponentUtils.createFormattedButton("Save Template");
        JButton loadTemplateButton = UIComponentUtils.createFormattedButton("Load Template");
        JButton clearButton = UIComponentUtils.createFormattedButton("Clear Form");

        enterButton.addActionListener(e -> {
            Map<String, String> data = collectData();
            String error = DataUtils.validateDevice(data);
            if (error != null) {
                statusLabel.setText("Error: " + error);
                return;
            }
            String sql = SQLGenerator.formatDeviceSQL(data);
            System.out.println("[ComputerPanel] " + sql);
            InventoryData.saveDevice(data);
            FileUtils.saveDevices();
            statusLabel.setText("Device saved successfully");
        });

        saveTemplateButton.addActionListener(e -> {
            Map<String, String> data = collectData();
            InventoryData.saveTemplate(data);
            FileUtils.saveTemplates();
            statusLabel.setText("Template saved successfully");
        });

        loadTemplateButton.addActionListener(e -> {
            JComboBox<String> templateCombo = UIComponentUtils.createFormattedComboBox(new String[]{});
            ArrayList<HashMap<String, String>> templates = InventoryData.getTemplates();
            if (templates.isEmpty()) {
                statusLabel.setText("No templates available");
                return;
            }
            for (HashMap<String, String> template : templates) {
                templateCombo.addItem(template.getOrDefault("Device_Name", ""));
            }
            int result = JOptionPane.showConfirmDialog(null, templateCombo, "Select Template", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String selected = (String) templateCombo.getSelectedItem();
                for (HashMap<String, String> template : templates) {
                    if (template.getOrDefault("Device_Name", "").equals(selected)) {
                        loadTemplateData(template);
                        statusLabel.setText("Template loaded");
                        break;
                    }
                }
            }
        });

        clearButton.addActionListener(e -> clearForm());

        add(UIComponentUtils.createAlignedLabel("Device Name:"));
        add(deviceNameField);
        add(UIComponentUtils.createAlignedLabel("Device Type:"));
        add(deviceTypeField);
        add(UIComponentUtils.createAlignedLabel("Brand:"));
        add(brandField);
        add(UIComponentUtils.createAlignedLabel("Model:"));
        add(modelField);
        add(UIComponentUtils.createAlignedLabel("Serial Number:"));
        add(serialNumberField);
        add(UIComponentUtils.createAlignedLabel("Processor Type:"));
        add(processorTypeField);
        add(UIComponentUtils.createAlignedLabel("Storage Capacity:"));
        add(storageCapacityField);
        add(UIComponentUtils.createAlignedLabel("Network Address:"));
        add(networkAddressField);
        add(UIComponentUtils.createAlignedLabel("Department:"));
        add(departmentField);
        add(UIComponentUtils.createAlignedLabel("Building Location:"));
        add(buildingLocationField);
        add(UIComponentUtils.createAlignedLabel("Room/Desk:"));
        add(roomDeskField);
        add(UIComponentUtils.createAlignedLabel("Specification:"));
        add(specificationField);
        add(UIComponentUtils.createAlignedLabel("OS Version:"));
        add(osVersionField);
        add(UIComponentUtils.createAlignedLabel("Assigned User:"));
        add(assignedUserField);
        add(UIComponentUtils.createAlignedLabel("Memory (RAM):"));
        add(memoryRAMField);
        add(UIComponentUtils.createAlignedLabel("Added Memory:"));
        add(addedMemoryCombo);
        add(UIComponentUtils.createAlignedLabel("Added Storage:"));
        add(addedStorageCombo);
        add(UIComponentUtils.createAlignedLabel("Status:"));
        add(statusCombo);
        add(UIComponentUtils.createAlignedLabel("Purchase Cost:"));
        add(purchaseCostField);
        add(UIComponentUtils.createAlignedLabel("Vendor:"));
        add(vendorField);
        add(UIComponentUtils.createAlignedLabel("Warranty Expiry Date:"));
        add(warrantyExpiryDatePicker_div);
        add(UIComponentUtils.createAlignedLabel("Last Maintenance:"));
        add(lastMaintenancePicker_div);
        add(UIComponentUtils.createAlignedLabel("Maintenance Due:"));
        add(maintenanceDatesPicker_div);
        add(UIComponentUtils.createAlignedLabel("Date of Purchase:"));
        add(dateOfPurchasePicker_div);
        add(enterButton);
        add(saveTemplateButton);
        add(loadTemplateButton);
        add(clearButton);
    }

    private Map<String, String> collectData() {
        Map<String, String> data = new HashMap<>();
        data.put("Device_Name", deviceNameField.getText());
        data.put("Device_Type", deviceTypeField.getText());
        data.put("Brand", brandField.getText());
        data.put("Model", modelField.getText());
        data.put("Serial_Number", serialNumberField.getText());
        data.put("Processor_Type", processorTypeField.getText());
        data.put("Storage_Capacity", storageCapacityField.getText());
        data.put("Network_Address", networkAddressField.getText());
        data.put("Department", departmentField.getText());
        data.put("Building_Location", buildingLocationField.getText());
        data.put("Room_Desk", roomDeskField.getText());
        data.put("Specification", specificationField.getText());
        data.put("OS_Version", osVersionField.getText());
        data.put("Assigned_User", assignedUserField.getText());
        data.put("Memory_RAM", memoryRAMField.getText());
        data.put("Added_Memory", (String) addedMemoryCombo.getSelectedItem());
        data.put("Added_Storage", (String) addedStorageCombo.getSelectedItem());
        data.put("Status", (String) statusCombo.getSelectedItem());
        data.put("Purchase_Cost", purchaseCostField.getText());
        data.put("Vendor", vendorField.getText());
        data.put("Warranty_Expiry_Date", UIComponentUtils.getDateFromPicker(warrantyExpiryDatePicker_div));
        data.put("Last_Maintenance", UIComponentUtils.getDateFromPicker(lastMaintenancePicker_div));
        data.put("Maintenance_Due", UIComponentUtils.getDateFromPicker(maintenanceDatesPicker_div));
        data.put("Date_Of_Purchase", UIComponentUtils.getDateFromPicker(dateOfPurchasePicker_div));
        return data;
    }

    private void loadTemplateData(HashMap<String, String> template) {
        deviceNameField.setText(template.getOrDefault("Device_Name", ""));
        deviceTypeField.setText(template.getOrDefault("Device_Type", ""));
        brandField.setText(template.getOrDefault("Brand", ""));
        modelField.setText(template.getOrDefault("Model", ""));
        serialNumberField.setText(template.getOrDefault("Serial_Number", ""));
        processorTypeField.setText(template.getOrDefault("Processor_Type", ""));
        storageCapacityField.setText(template.getOrDefault("Storage_Capacity", ""));
        networkAddressField.setText(template.getOrDefault("Network_Address", ""));
        departmentField.setText(template.getOrDefault("Department", ""));
        buildingLocationField.setText(template.getOrDefault("Building_Location", ""));
        roomDeskField.setText(template.getOrDefault("Room_Desk", ""));
        specificationField.setText(template.getOrDefault("Specification", ""));
        osVersionField.setText(template.getOrDefault("OS_Version", ""));
        assignedUserField.setText(template.getOrDefault("Assigned_User", ""));
        memoryRAMField.setText(template.getOrDefault("Memory_RAM", ""));
        purchaseCostField.setText(template.getOrDefault("Purchase_Cost", ""));
        vendorField.setText(template.getOrDefault("Vendor", ""));
        addedMemoryCombo.setSelectedItem(template.getOrDefault("Added_Memory", "null"));
        addedStorageCombo.setSelectedItem(template.getOrDefault("Added_Storage", "null"));
        statusCombo.setSelectedItem(template.getOrDefault("Status", "Deployed"));
        String date = template.getOrDefault("Warranty_Expiry_Date", "");
        if (!date.isEmpty()) ((JTextField) warrantyExpiryDatePicker_div.getComponent(0)).setText(date);
        date = template.getOrDefault("Last_Maintenance", "");
        if (!date.isEmpty()) ((JTextField) lastMaintenancePicker_div.getComponent(0)).setText(date);
        date = template.getOrDefault("Maintenance_Due", "");
        if (!date.isEmpty()) ((JTextField) maintenanceDatesPicker_div.getComponent(0)).setText(date);
        date = template.getOrDefault("Date_Of_Purchase", "");
        if (!date.isEmpty()) ((JTextField) dateOfPurchasePicker_div.getComponent(0)).setText(date);
    }

    private void clearForm() {
        deviceNameField.setText("");
        deviceTypeField.setText("");
        brandField.setText("");
        modelField.setText("");
        serialNumberField.setText("");
        processorTypeField.setText("");
        storageCapacityField.setText("");
        networkAddressField.setText("");
        departmentField.setText("");
        purchaseCostField.setText("");
        vendorField.setText("");
        osVersionField.setText("");
        roomDeskField.setText("");
        specificationField.setText("");
        assignedUserField.setText("");
        buildingLocationField.setText("");
        memoryRAMField.setText("");
        addedMemoryCombo.setSelectedItem("null");
        addedStorageCombo.setSelectedItem("null");
        statusCombo.setSelectedItem("Deployed");
        ((JTextField) warrantyExpiryDatePicker_div.getComponent(0)).setText(new SimpleDateFormat("MM-dd-yyyy").format(new Date()));
        ((JTextField) lastMaintenancePicker_div.getComponent(0)).setText(new SimpleDateFormat("MM-dd-yyyy").format(new Date()));
        ((JTextField) maintenanceDatesPicker_div.getComponent(0)).setText(new SimpleDateFormat("MM-dd-yyyy").format(new Date()));
        ((JTextField) dateOfPurchasePicker_div.getComponent(0)).setText(new SimpleDateFormat("MM-dd-yyyy").format(new Date()));
        statusLabel.setText("Form cleared");
    }
}