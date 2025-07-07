import java.awt.*;
import java.text.SimpleDateFormat;
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

        JComboBox<String> deviceTypeCombo = UIUtils.createFormattedComboBox(new String[]{"Computer", "Printer", "Router", "Switch"});
        JPanel topPanel = new JPanel();
        topPanel.add(UIUtils.createAlignedLabel("Select Device Type:"));
        topPanel.add(deviceTypeCombo);

        computerPanel = createComputerPanel();
        printerPanel = createPrinterPanel();
        routerPanel = createRouterPanel();
        switchPanel = createSwitchPanel();

        JPanel contentPanel = new JPanel();
        JScrollPane scrollPane = UIUtils.createScrollableContentPanel(contentPanel);
        contentPanel.add(computerPanel);
        statusLabel = UIUtils.createAlignedLabel("");
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

        deviceNameField = UIUtils.createFormattedTextField();
        deviceTypeField = UIUtils.createFormattedTextField();
        brandField = UIUtils.createFormattedTextField();
        modelField = UIUtils.createFormattedTextField();
        serialNumberField = UIUtils.createFormattedTextField();
        processorTypeField = UIUtils.createFormattedTextField();
        storageCapacityField = UIUtils.createFormattedTextField();
        networkAddressField = UIUtils.createFormattedTextField();
        departmentField = UIUtils.createFormattedTextField();
        purchaseCostField = UIUtils.createFormattedTextField();
        vendorField = UIUtils.createFormattedTextField();
        osVersionField = UIUtils.createFormattedTextField();
        roomDeskField = UIUtils.createFormattedTextField();
        specificationField = UIUtils.createFormattedTextField();
        assignedUserField = UIUtils.createFormattedTextField();
        buildingLocationField = UIUtils.createFormattedTextField();
        memoryRAMField = UIUtils.createFormattedTextField();
        addedMemoryCombo = UIUtils.createFormattedComboBox(new String[]{"TRUE", "FALSE", "null"});
        addedStorageCombo = UIUtils.createFormattedComboBox(new String[]{"TRUE", "FALSE", "null"});
        statusCombo = UIUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
        warrantyExpiryDatePicker_div = UIUtils.createFormattedDatePicker();
        lastMaintenancePicker_div = UIUtils.createFormattedDatePicker();
        maintenanceDatesPicker_div = UIUtils.createFormattedDatePicker();
        dateOfPurchasePicker_div = UIUtils.createFormattedDatePicker();

        JButton enterButton = UIUtils.createFormattedButton("Enter");
        JButton saveTemplateButton = UIUtils.createFormattedButton("Save Template");
        JButton loadTemplateButton = UIUtils.createFormattedButton("Load Template");
        JButton clearButton = UIUtils.createFormattedButton("Clear Form");

        enterButton.addActionListener(e -> {
            Map<String, String> data = collectData();
            String error = UIUtils.validateDevice(data);
            if (error != null) {
                statusLabel.setText("Error: " + error);
                return;
            }
            String sql = SQLGenerator.formatDeviceSQL(data);
            System.out.println("[LogNewDeviceTab - Computer] " + sql);
            UIUtils.saveDevice(data);
            statusLabel.setText("Device saved successfully");
        });

        saveTemplateButton.addActionListener(e -> {
            Map<String, String> data = collectData();
            UIUtils.saveTemplate(data);
            statusLabel.setText("Template saved successfully");
        });

        loadTemplateButton.addActionListener(e -> {
            JComboBox<String> templateCombo = new JComboBox<>();
            for (HashMap<String, String> template : UIUtils.getTemplates()) {
                templateCombo.addItem(template.getOrDefault("Device_Name", ""));
            }
            int result = JOptionPane.showConfirmDialog(null, templateCombo, "Select Template", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String selected = (String) templateCombo.getSelectedItem();
                for (HashMap<String, String> template : UIUtils.getTemplates()) {
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

        panel.add(UIUtils.createAlignedLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(UIUtils.createAlignedLabel("Device Type:"));
        panel.add(deviceTypeField);
        panel.add(UIUtils.createAlignedLabel("Brand:"));
        panel.add(brandField);
        panel.add(UIUtils.createAlignedLabel("Model:"));
        panel.add(modelField);
        panel.add(UIUtils.createAlignedLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(UIUtils.createAlignedLabel("Processor Type:"));
        panel.add(processorTypeField);
        panel.add(UIUtils.createAlignedLabel("Storage Capacity:"));
        panel.add(storageCapacityField);
        panel.add(UIUtils.createAlignedLabel("Network Address:"));
        panel.add(networkAddressField);
        panel.add(UIUtils.createAlignedLabel("Department:"));
        panel.add(departmentField);
        panel.add(UIUtils.createAlignedLabel("Building Location:"));
        panel.add(buildingLocationField);
        panel.add(UIUtils.createAlignedLabel("Room/Desk:"));
        panel.add(roomDeskField);
        panel.add(UIUtils.createAlignedLabel("Specification:"));
        panel.add(specificationField);
        panel.add(UIUtils.createAlignedLabel("OS Version:"));
        panel.add(osVersionField);
        panel.add(UIUtils.createAlignedLabel("Assigned User:"));
        panel.add(assignedUserField);
        panel.add(UIUtils.createAlignedLabel("Memory (RAM):"));
        panel.add(memoryRAMField);
        panel.add(UIUtils.createAlignedLabel("Added Memory:"));
        panel.add(addedMemoryCombo);
        panel.add(UIUtils.createAlignedLabel("Added Storage:"));
        panel.add(addedStorageCombo);
        panel.add(UIUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(UIUtils.createAlignedLabel("Warranty Expiry Date:"));
        panel.add(warrantyExpiryDatePicker_div);
        panel.add(UIUtils.createAlignedLabel("Last Maintenance:"));
        panel.add(lastMaintenancePicker_div);
        panel.add(UIUtils.createAlignedLabel("Maintenance Due:"));
        panel.add(maintenanceDatesPicker_div);
        panel.add(UIUtils.createAlignedLabel("Date of Purchase:"));
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

        JTextField deviceNameField = UIUtils.createFormattedTextField();
        JTextField brandField = UIUtils.createFormattedTextField();
        JTextField modelField = UIUtils.createFormattedTextField();
        JTextField serialNumberField = UIUtils.createFormattedTextField();
        JTextField purchaseCostField = UIUtils.createFormattedTextField();
        JTextField vendorField = UIUtils.createFormattedTextField();
        JTextField specificationField = UIUtils.createFormattedTextField();
        JTextField departmentField = UIUtils.createFormattedTextField();
        JTextField buildingLocationField = UIUtils.createFormattedTextField();
        JTextField roomDeskField = UIUtils.createFormattedTextField();
        JComboBox<String> statusCombo = UIUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
        JPanel warrantyExpiryDatePicker_div = UIUtils.createFormattedDatePicker();
        JPanel dateOfPurchasePicker_div = UIUtils.createFormattedDatePicker();

        JButton enterButton = UIUtils.createFormattedButton("Enter");
        JButton clearButton = UIUtils.createFormattedButton("Clear Form");

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
            data.put("Warranty_Expiry_Date", UIUtils.getDateFromPicker(warrantyExpiryDatePicker_div));
            data.put("Date_Of_Purchase", UIUtils.getDateFromPicker(dateOfPurchasePicker_div));

            String error = UIUtils.validateDevice(data);
            if (error != null) {
                statusLabel.setText("Error: " + error);
                return;
            }
            String sql = SQLGenerator.formatDeviceSQL(data);
            System.out.println("[LogNewDeviceTab - Printer] " + sql);
            UIUtils.saveDevice(data);
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

        panel.add(UIUtils.createAlignedLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(UIUtils.createAlignedLabel("Brand:"));
        panel.add(brandField);
        panel.add(UIUtils.createAlignedLabel("Model:"));
        panel.add(modelField);
        panel.add(UIUtils.createAlignedLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(UIUtils.createAlignedLabel("Specification:"));
        panel.add(specificationField);
        panel.add(UIUtils.createAlignedLabel("Department:"));
        panel.add(departmentField);
        panel.add(UIUtils.createAlignedLabel("Building Location:"));
        panel.add(buildingLocationField);
        panel.add(UIUtils.createAlignedLabel("Room/Desk:"));
        panel.add(roomDeskField);
        panel.add(UIUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(UIUtils.createAlignedLabel("Warranty Expiry Date:"));
        panel.add(warrantyExpiryDatePicker_div);
        panel.add(UIUtils.createAlignedLabel("Date of Purchase:"));
        panel.add(dateOfPurchasePicker_div);
        panel.add(enterButton);
        panel.add(clearButton);

        return panel;
    }

    private JPanel createRouterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField deviceNameField = UIUtils.createFormattedTextField();
        JTextField modelField = UIUtils.createFormattedTextField();
        JTextField serialNumberField = UIUtils.createFormattedTextField();
        JTextField networkAddressField = UIUtils.createFormattedTextField();
        JTextField purchaseCostField = UIUtils.createFormattedTextField();
        JTextField vendorField = UIUtils.createFormattedTextField();
        JTextField specificationField = UIUtils.createFormattedTextField();
        JTextField departmentField = UIUtils.createFormattedTextField();
        JTextField buildingLocationField = UIUtils.createFormattedTextField();
        JTextField roomDeskField = UIUtils.createFormattedTextField();
        JComboBox<String> statusCombo = UIUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
        JPanel warrantyExpiryDatePicker_div = UIUtils.createFormattedDatePicker();
        JPanel dateOfPurchasePicker_div = UIUtils.createFormattedDatePicker();

        JButton enterButton = UIUtils.createFormattedButton("Enter");
        JButton clearButton = UIUtils.createFormattedButton("Clear Form");

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
            data.put("Warranty_Expiry_Date", UIUtils.getDateFromPicker(warrantyExpiryDatePicker_div));
            data.put("Date_Of_Purchase", UIUtils.getDateFromPicker(dateOfPurchasePicker_div));

            String error = UIUtils.validateDevice(data);
            if (error != null) {
                statusLabel.setText("Error: " + error);
                return;
            }
            String sql = SQLGenerator.formatDeviceSQL(data);
            System.out.println("[LogNewDeviceTab - Router] " + sql);
            UIUtils.saveDevice(data);
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

        panel.add(UIUtils.createAlignedLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(UIUtils.createAlignedLabel("Model:"));
        panel.add(modelField);
        panel.add(UIUtils.createAlignedLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(UIUtils.createAlignedLabel("Network Address:"));
        panel.add(networkAddressField);
        panel.add(UIUtils.createAlignedLabel("Specification:"));
        panel.add(specificationField);
        panel.add(UIUtils.createAlignedLabel("Department:"));
        panel.add(departmentField);
        panel.add(UIUtils.createAlignedLabel("Building Location:"));
        panel.add(buildingLocationField);
        panel.add(UIUtils.createAlignedLabel("Room/Desk:"));
        panel.add(roomDeskField);
        panel.add(UIUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(UIUtils.createAlignedLabel("Warranty Expiry Date:"));
        panel.add(warrantyExpiryDatePicker_div);
        panel.add(UIUtils.createAlignedLabel("Date of Purchase:"));
        panel.add(dateOfPurchasePicker_div);
        panel.add(enterButton);
        panel.add(clearButton);

        return panel;
    }

    private JPanel createSwitchPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField deviceNameField = UIUtils.createFormattedTextField();
        JTextField modelField = UIUtils.createFormattedTextField();
        JTextField serialNumberField = UIUtils.createFormattedTextField();
        JTextField networkAddressField = UIUtils.createFormattedTextField();
        JTextField purchaseCostField = UIUtils.createFormattedTextField();
        JTextField vendorField = UIUtils.createFormattedTextField();
        JTextField specificationField = UIUtils.createFormattedTextField();
        JTextField departmentField = UIUtils.createFormattedTextField();
        JTextField buildingLocationField = UIUtils.createFormattedTextField();
        JTextField roomDeskField = UIUtils.createFormattedTextField();
        JComboBox<String> statusCombo = UIUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
        JPanel warrantyExpiryDatePicker_div = UIUtils.createFormattedDatePicker();
        JPanel dateOfPurchasePicker_div = UIUtils.createFormattedDatePicker();

        JButton enterButton = UIUtils.createFormattedButton("Enter");
        JButton clearButton = UIUtils.createFormattedButton("Clear Form");

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
            data.put("Warranty_Expiry_Date", UIUtils.getDateFromPicker(warrantyExpiryDatePicker_div));
            data.put("Date_Of_Purchase", UIUtils.getDateFromPicker(dateOfPurchasePicker_div));

            String error = UIUtils.validateDevice(data);
            if (error != null) {
                statusLabel.setText("Error: " + error);
                return;
            }
            String sql = SQLGenerator.formatDeviceSQL(data);
            System.out.println("[LogNewDeviceTab - Switch] " + sql);
            UIUtils.saveDevice(data);
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

        panel.add(UIUtils.createAlignedLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(UIUtils.createAlignedLabel("Model:"));
        panel.add(modelField);
        panel.add(UIUtils.createAlignedLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(UIUtils.createAlignedLabel("Network Address:"));
        panel.add(networkAddressField);
        panel.add(UIUtils.createAlignedLabel("Specification:"));
        panel.add(specificationField);
        panel.add(UIUtils.createAlignedLabel("Department:"));
        panel.add(departmentField);
        panel.add(UIUtils.createAlignedLabel("Building Location:"));
        panel.add(buildingLocationField);
        panel.add(UIUtils.createAlignedLabel("Room/Desk:"));
        panel.add(roomDeskField);
        panel.add(UIUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(UIUtils.createAlignedLabel("Warranty Expiry Date:"));
        panel.add(warrantyExpiryDatePicker_div);
        panel.add(UIUtils.createAlignedLabel("Date of Purchase:"));
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
        data.put("Warranty_Expiry_Date", UIUtils.getDateFromPicker(warrantyExpiryDatePicker_div));
        data.put("Last_Maintenance", UIUtils.getDateFromPicker(lastMaintenancePicker_div));
        data.put("Maintenance_Due", UIUtils.getDateFromPicker(maintenanceDatesPicker_div));
        data.put("Date_Of_Purchase", UIUtils.getDateFromPicker(dateOfPurchasePicker_div));
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