import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class LogNewDeviceTab extends JPanel {
    private JPanel warrantyExpiryDatePicker_div, maintenanceDatesPicker_div, lastMaintenancePicker_div, dateOfPurchasePicker_div;
    private JPanel computerPanel, printerPanel, routerPanel, switchPanel;
    private JTextField deviceNameField, deviceTypeField, brandField, modelField, serialNumberField, processorTypeField,
                      storageCapacityField, networkAddressField, departmentField, purchaseCostField, vendorField,
                      osVersionField, roomDeskField, specificationField, assignedUserField, buildingLocationField,
                      memoryRAMField;
    private JComboBox<String> addedMemoryCombo, addedStorageCombo, statusCombo;
    private JLabel statusLabel;

    public LogNewDeviceTab() {
        setLayout(new BorderLayout(10, 10));

        JComboBox<String> deviceTypeCombo = UIComponentUtils.createFormattedComboBox(new String[]{"Computer", "Printer", "Router", "Switch"});
        JPanel topPanel = new JPanel();
        topPanel.add(UIComponentUtils.createAlignedLabel("Select Device Type:"));
        topPanel.add(deviceTypeCombo);

        computerPanel = createComputerPanel();
        printerPanel = createPrinterPanel();
        routerPanel = createRouterPanel();
        switchPanel = createSwitchPanel();

        JPanel contentPanel = new JPanel();
        JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(contentPanel);
        contentPanel.add(computerPanel);
        statusLabel = UIComponentUtils.createAlignedLabel("");
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        deviceTypeCombo.addActionListener(e -> {
            String selectedDeviceType = (String) deviceTypeCombo.getSelectedItem();
            contentPanel.removeAll();
            switch (selectedDeviceType) {
                case "Computer": contentPanel.add(computerPanel); break;
                case "Printer": contentPanel.add(printerPanel); break;
                case "Router": contentPanel.add(routerPanel); break;
                case "Switch": contentPanel.add(switchPanel); break;
            }
            contentPanel.revalidate();
            contentPanel.repaint();
            statusLabel.setText("");
        });

        deviceTypeCombo.setSelectedItem("Computer");
    }

    private JPanel createComputerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

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
            System.out.println("[LogNewDeviceTab - Computer] " + sql);
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

        clearButton.addActionListener(e -> {
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
        });

        panel.add(UIComponentUtils.createAlignedLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(UIComponentUtils.createAlignedLabel("Device Type:"));
        panel.add(deviceTypeField);
        panel.add(UIComponentUtils.createAlignedLabel("Brand:"));
        panel.add(brandField);
        panel.add(UIComponentUtils.createAlignedLabel("Model:"));
        panel.add(modelField);
        panel.add(UIComponentUtils.createAlignedLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(UIComponentUtils.createAlignedLabel("Processor Type:"));
        panel.add(processorTypeField);
        panel.add(UIComponentUtils.createAlignedLabel("Storage Capacity:"));
        panel.add(storageCapacityField);
        panel.add(UIComponentUtils.createAlignedLabel("Network Address:"));
        panel.add(networkAddressField);
        panel.add(UIComponentUtils.createAlignedLabel("Department:"));
        panel.add(departmentField);
        panel.add(UIComponentUtils.createAlignedLabel("Building Location:"));
        panel.add(buildingLocationField);
        panel.add(UIComponentUtils.createAlignedLabel("Room/Desk:"));
        panel.add(roomDeskField);
        panel.add(UIComponentUtils.createAlignedLabel("Specification:"));
        panel.add(specificationField);
        panel.add(UIComponentUtils.createAlignedLabel("OS Version:"));
        panel.add(osVersionField);
        panel.add(UIComponentUtils.createAlignedLabel("Assigned User:"));
        panel.add(assignedUserField);
        panel.add(UIComponentUtils.createAlignedLabel("Memory (RAM):"));
        panel.add(memoryRAMField);
        panel.add(UIComponentUtils.createAlignedLabel("Added Memory:"));
        panel.add(addedMemoryCombo);
        panel.add(UIComponentUtils.createAlignedLabel("Added Storage:"));
        panel.add(addedStorageCombo);
        panel.add(UIComponentUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIComponentUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIComponentUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(UIComponentUtils.createAlignedLabel("Warranty Expiry Date:"));
        panel.add(warrantyExpiryDatePicker_div);
        panel.add(UIComponentUtils.createAlignedLabel("Last Maintenance:"));
        panel.add(lastMaintenancePicker_div);
        panel.add(UIComponentUtils.createAlignedLabel("Maintenance Due:"));
        panel.add(maintenanceDatesPicker_div);
        panel.add(UIComponentUtils.createAlignedLabel("Date of Purchase:"));
        panel.add(dateOfPurchasePicker_div);
        panel.add(enterButton);
        panel.add(saveTemplateButton);
        panel.add(loadTemplateButton);
        panel.add(clearButton);

        return panel;
    }

    private JPanel createPrinterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField deviceNameField = UIComponentUtils.createFormattedTextField();
        JTextField brandField = UIComponentUtils.createFormattedTextField();
        JTextField modelField = UIComponentUtils.createFormattedTextField();
        JTextField serialNumberField = UIComponentUtils.createFormattedTextField();
        JTextField purchaseCostField = UIComponentUtils.createFormattedTextField();
        JTextField vendorField = UIComponentUtils.createFormattedTextField();
        JTextField specificationField = UIComponentUtils.createFormattedTextField();
        JTextField departmentField = UIComponentUtils.createFormattedTextField();
        JTextField buildingLocationField = UIComponentUtils.createFormattedTextField();
        JTextField roomDeskField = UIComponentUtils.createFormattedTextField();
        JComboBox<String> statusCombo = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
        JPanel warrantyExpiryDatePicker_div = UIComponentUtils.createFormattedDatePicker();
        JPanel dateOfPurchasePicker_div = UIComponentUtils.createFormattedDatePicker();

        JButton enterButton = UIComponentUtils.createFormattedButton("Enter");
        JButton clearButton = UIComponentUtils.createFormattedButton("Clear Form");

        enterButton.addActionListener(e -> {
            Map<String, String> data = new HashMap<>();
            data.put("Device_Name", deviceNameField.getText());
            data.put("Device_Type", "Printer");
            data.put("Brand", brandField.getText());
            data.put("Model", modelField.getText());
            data.put("Serial_Number", serialNumberField.getText());
            data.put("Specification", specificationField.getText());
            data.put("Department", departmentField.getText());
            data.put("Building_Location", buildingLocationField.getText());
            data.put("Room_Desk", roomDeskField.getText());
            data.put("Status", (String) statusCombo.getSelectedItem());
            data.put("Purchase_Cost", purchaseCostField.getText());
            data.put("Vendor", vendorField.getText());
            data.put("Warranty_Expiry_Date", UIComponentUtils.getDateFromPicker(warrantyExpiryDatePicker_div));
            data.put("Date_Of_Purchase", UIComponentUtils.getDateFromPicker(dateOfPurchasePicker_div));

            String error = DataUtils.validateDevice(data);
            if (error != null) {
                statusLabel.setText("Error: " + error);
                return;
            }
            String sql = SQLGenerator.formatDeviceSQL(data);
            System.out.println("[LogNewDeviceTab - Printer] " + sql);
            InventoryData.saveDevice(data);
            FileUtils.saveDevices();
            statusLabel.setText("Device saved successfully");
        });

        clearButton.addActionListener(e -> {
            deviceNameField.setText("");
            brandField.setText("");
            modelField.setText("");
            serialNumberField.setText("");
            specificationField.setText("");
            departmentField.setText("");
            buildingLocationField.setText("");
            roomDeskField.setText("");
            statusCombo.setSelectedItem("Deployed");
            purchaseCostField.setText("");
            vendorField.setText("");
            ((JTextField) warrantyExpiryDatePicker_div.getComponent(0)).setText(new SimpleDateFormat("MM-dd-yyyy").format(new Date()));
            ((JTextField) dateOfPurchasePicker_div.getComponent(0)).setText(new SimpleDateFormat("MM-dd-yyyy").format(new Date()));
            statusLabel.setText("Form cleared");
        });

        panel.add(UIComponentUtils.createAlignedLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(UIComponentUtils.createAlignedLabel("Brand:"));
        panel.add(brandField);
        panel.add(UIComponentUtils.createAlignedLabel("Model:"));
        panel.add(modelField);
        panel.add(UIComponentUtils.createAlignedLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(UIComponentUtils.createAlignedLabel("Specification:"));
        panel.add(specificationField);
        panel.add(UIComponentUtils.createAlignedLabel("Department:"));
        panel.add(departmentField);
        panel.add(UIComponentUtils.createAlignedLabel("Building Location:"));
        panel.add(buildingLocationField);
        panel.add(UIComponentUtils.createAlignedLabel("Room/Desk:"));
        panel.add(roomDeskField);
        panel.add(UIComponentUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIComponentUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIComponentUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(UIComponentUtils.createAlignedLabel("Warranty Expiry Date:"));
        panel.add(warrantyExpiryDatePicker_div);
        panel.add(UIComponentUtils.createAlignedLabel("Date of Purchase:"));
        panel.add(dateOfPurchasePicker_div);
        panel.add(enterButton);
        panel.add(clearButton);

        return panel;
    }

    private JPanel createRouterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField deviceNameField = UIComponentUtils.createFormattedTextField();
        JTextField modelField = UIComponentUtils.createFormattedTextField();
        JTextField serialNumberField = UIComponentUtils.createFormattedTextField();
        JTextField networkAddressField = UIComponentUtils.createFormattedTextField();
        JTextField purchaseCostField = UIComponentUtils.createFormattedTextField();
        JTextField vendorField = UIComponentUtils.createFormattedTextField();
        JTextField specificationField = UIComponentUtils.createFormattedTextField();
        JTextField departmentField = UIComponentUtils.createFormattedTextField();
        JTextField buildingLocationField = UIComponentUtils.createFormattedTextField();
        JTextField roomDeskField = UIComponentUtils.createFormattedTextField();
        JComboBox<String> statusCombo = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
        JPanel warrantyExpiryDatePicker_div = UIComponentUtils.createFormattedDatePicker();
        JPanel dateOfPurchasePicker_div = UIComponentUtils.createFormattedDatePicker();

        JButton enterButton = UIComponentUtils.createFormattedButton("Enter");
        JButton clearButton = UIComponentUtils.createFormattedButton("Clear Form");

        enterButton.addActionListener(e -> {
            Map<String, String> data = new HashMap<>();
            data.put("Device_Name", deviceNameField.getText());
            data.put("Device_Type", "Router");
            data.put("Model", modelField.getText());
            data.put("Serial_Number", serialNumberField.getText());
            data.put("Network_Address", networkAddressField.getText());
            data.put("Specification", specificationField.getText());
            data.put("Department", departmentField.getText());
            data.put("Building_Location", buildingLocationField.getText());
            data.put("Room_Desk", roomDeskField.getText());
            data.put("Status", (String) statusCombo.getSelectedItem());
            data.put("Purchase_Cost", purchaseCostField.getText());
            data.put("Vendor", vendorField.getText());
            data.put("Warranty_Expiry_Date", UIComponentUtils.getDateFromPicker(warrantyExpiryDatePicker_div));
            data.put("Date_Of_Purchase", UIComponentUtils.getDateFromPicker(dateOfPurchasePicker_div));

            String error = DataUtils.validateDevice(data);
            if (error != null) {
                statusLabel.setText("Error: " + error);
                return;
            }
            String sql = SQLGenerator.formatDeviceSQL(data);
            System.out.println("[LogNewDeviceTab - Router] " + sql);
            InventoryData.saveDevice(data);
            FileUtils.saveDevices();
            statusLabel.setText("Device saved successfully");
        });

        clearButton.addActionListener(e -> {
            deviceNameField.setText("");
            modelField.setText("");
            serialNumberField.setText("");
            networkAddressField.setText("");
            specificationField.setText("");
            departmentField.setText("");
            buildingLocationField.setText("");
            roomDeskField.setText("");
            statusCombo.setSelectedItem("Deployed");
            purchaseCostField.setText("");
            vendorField.setText("");
            ((JTextField) warrantyExpiryDatePicker_div.getComponent(0)).setText(new SimpleDateFormat("MM-dd-yyyy").format(new Date()));
            ((JTextField) dateOfPurchasePicker_div.getComponent(0)).setText(new SimpleDateFormat("MM-dd-yyyy").format(new Date()));
            statusLabel.setText("Form cleared");
        });

        panel.add(UIComponentUtils.createAlignedLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(UIComponentUtils.createAlignedLabel("Model:"));
        panel.add(modelField);
        panel.add(UIComponentUtils.createAlignedLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(UIComponentUtils.createAlignedLabel("Network Address:"));
        panel.add(networkAddressField);
        panel.add(UIComponentUtils.createAlignedLabel("Specification:"));
        panel.add(specificationField);
        panel.add(UIComponentUtils.createAlignedLabel("Department:"));
        panel.add(departmentField);
        panel.add(UIComponentUtils.createAlignedLabel("Building Location:"));
        panel.add(buildingLocationField);
        panel.add(UIComponentUtils.createAlignedLabel("Room/Desk:"));
        panel.add(roomDeskField);
        panel.add(UIComponentUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIComponentUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIComponentUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(UIComponentUtils.createAlignedLabel("Warranty Expiry Date:"));
        panel.add(warrantyExpiryDatePicker_div);
        panel.add(UIComponentUtils.createAlignedLabel("Date of Purchase:"));
        panel.add(dateOfPurchasePicker_div);
        panel.add(enterButton);
        panel.add(clearButton);

        return panel;
    }

    private JPanel createSwitchPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField deviceNameField = UIComponentUtils.createFormattedTextField();
        JTextField modelField = UIComponentUtils.createFormattedTextField();
        JTextField serialNumberField = UIComponentUtils.createFormattedTextField();
        JTextField networkAddressField = UIComponentUtils.createFormattedTextField();
        JTextField purchaseCostField = UIComponentUtils.createFormattedTextField();
        JTextField vendorField = UIComponentUtils.createFormattedTextField();
        JTextField specificationField = UIComponentUtils.createFormattedTextField();
        JTextField departmentField = UIComponentUtils.createFormattedTextField();
        JTextField buildingLocationField = UIComponentUtils.createFormattedTextField();
        JTextField roomDeskField = UIComponentUtils.createFormattedTextField();
        JComboBox<String> statusCombo = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
        JPanel warrantyExpiryDatePicker_div = UIComponentUtils.createFormattedDatePicker();
        JPanel dateOfPurchasePicker_div = UIComponentUtils.createFormattedDatePicker();

        JButton enterButton = UIComponentUtils.createFormattedButton("Enter");
        JButton clearButton = UIComponentUtils.createFormattedButton("Clear Form");

        enterButton.addActionListener(e -> {
            Map<String, String> data = new HashMap<>();
            data.put("Device_Name", deviceNameField.getText());
            data.put("Device_Type", "Switch");
            data.put("Model", modelField.getText());
            data.put("Serial_Number", serialNumberField.getText());
            data.put("Network_Address", networkAddressField.getText());
            data.put("Specification", specificationField.getText());
            data.put("Department", departmentField.getText());
            data.put("Building_Location", buildingLocationField.getText());
            data.put("Room_Desk", roomDeskField.getText());
            data.put("Status", (String) statusCombo.getSelectedItem());
            data.put("Purchase_Cost", purchaseCostField.getText());
            data.put("Vendor", vendorField.getText());
            data.put("Warranty_Expiry_Date", UIComponentUtils.getDateFromPicker(warrantyExpiryDatePicker_div));
            data.put("Date_Of_Purchase", UIComponentUtils.getDateFromPicker(dateOfPurchasePicker_div));

            String error = DataUtils.validateDevice(data);
            if (error != null) {
                statusLabel.setText("Error: " + error);
                return;
            }
            String sql = SQLGenerator.formatDeviceSQL(data);
            System.out.println("[LogNewDeviceTab - Switch] " + sql);
            InventoryData.saveDevice(data);
            FileUtils.saveDevices();
            statusLabel.setText("Device saved successfully");
        });

        clearButton.addActionListener(e -> {
            deviceNameField.setText("");
            modelField.setText("");
            serialNumberField.setText("");
            networkAddressField.setText("");
            specificationField.setText("");
            departmentField.setText("");
            buildingLocationField.setText("");
            roomDeskField.setText("");
            statusCombo.setSelectedItem("Deployed");
            purchaseCostField.setText("");
            vendorField.setText("");
            ((JTextField) warrantyExpiryDatePicker_div.getComponent(0)).setText(new SimpleDateFormat("MM-dd-yyyy").format(new Date()));
            ((JTextField) dateOfPurchasePicker_div.getComponent(0)).setText(new SimpleDateFormat("MM-dd-yyyy").format(new Date()));
            statusLabel.setText("Form cleared");
        });

        panel.add(UIComponentUtils.createAlignedLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(UIComponentUtils.createAlignedLabel("Model:"));
        panel.add(modelField);
        panel.add(UIComponentUtils.createAlignedLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(UIComponentUtils.createAlignedLabel("Network Address:"));
        panel.add(networkAddressField);
        panel.add(UIComponentUtils.createAlignedLabel("Specification:"));
        panel.add(specificationField);
        panel.add(UIComponentUtils.createAlignedLabel("Department:"));
        panel.add(departmentField);
        panel.add(UIComponentUtils.createAlignedLabel("Building Location:"));
        panel.add(buildingLocationField);
        panel.add(UIComponentUtils.createAlignedLabel("Room/Desk:"));
        panel.add(roomDeskField);
        panel.add(UIComponentUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIComponentUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIComponentUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(UIComponentUtils.createAlignedLabel("Warranty Expiry Date:"));
        panel.add(warrantyExpiryDatePicker_div);
        panel.add(UIComponentUtils.createAlignedLabel("Date of Purchase:"));
        panel.add(dateOfPurchasePicker_div);
        panel.add(enterButton);
        panel.add(clearButton);

        return panel;
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
}