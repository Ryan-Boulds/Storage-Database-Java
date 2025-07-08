package device_logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import utils.DataUtils;
import utils.FileUtils;
import utils.InventoryData;
import utils.SQLGenerator;
import utils.UIComponentUtils;

public class RouterPanel extends JPanel {
    private JTextField deviceNameField, modelField, serialNumberField, networkAddressField,
                      purchaseCostField, vendorField, specificationField, departmentField,
                      buildingLocationField, roomDeskField;
    private JComboBox<String> statusCombo;
    private JPanel warrantyExpiryDatePicker_div, dateOfPurchasePicker_div;
    private JLabel statusLabel;

    public RouterPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        initializeComponents();
        addComponents();
    }

    private void initializeComponents() {
        deviceNameField = UIComponentUtils.createFormattedTextField();
        modelField = UIComponentUtils.createFormattedTextField();
        serialNumberField = UIComponentUtils.createFormattedTextField();
        networkAddressField = UIComponentUtils.createFormattedTextField();
        purchaseCostField = UIComponentUtils.createFormattedTextField();
        vendorField = UIComponentUtils.createFormattedTextField();
        specificationField = UIComponentUtils.createFormattedTextField();
        departmentField = UIComponentUtils.createFormattedTextField();
        buildingLocationField = UIComponentUtils.createFormattedTextField();
        roomDeskField = UIComponentUtils.createFormattedTextField();
        statusCombo = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
        warrantyExpiryDatePicker_div = UIComponentUtils.createFormattedDatePicker();
        dateOfPurchasePicker_div = UIComponentUtils.createFormattedDatePicker();
    }

    private void addComponents() {
        JButton enterButton = UIComponentUtils.createFormattedButton("Enter");
        JButton clearButton = UIComponentUtils.createFormattedButton("Clear Form");

        enterButton.addActionListener(e -> {
            Map<String, String> data = collectData();
            String error = DataUtils.validateDevice(data);
            if (error != null) {
                statusLabel.setText("Error: " + error);
                return;
            }
            String sql = SQLGenerator.formatDeviceSQL(data);
            System.out.println("[RouterPanel] " + sql);
            InventoryData.saveDevice(data);
            FileUtils.saveDevices();
            statusLabel.setText("Device saved successfully");
        });

        clearButton.addActionListener(e -> clearForm());

        add(UIComponentUtils.createAlignedLabel("Device Name:"));
        add(deviceNameField);
        add(UIComponentUtils.createAlignedLabel("Model:"));
        add(modelField);
        add(UIComponentUtils.createAlignedLabel("Serial Number:"));
        add(serialNumberField);
        add(UIComponentUtils.createAlignedLabel("Network Address:"));
        add(networkAddressField);
        add(UIComponentUtils.createAlignedLabel("Specification:"));
        add(specificationField);
        add(UIComponentUtils.createAlignedLabel("Department:"));
        add(departmentField);
        add(UIComponentUtils.createAlignedLabel("Building Location:"));
        add(buildingLocationField);
        add(UIComponentUtils.createAlignedLabel("Room/Desk:"));
        add(roomDeskField);
        add(UIComponentUtils.createAlignedLabel("Status:"));
        add(statusCombo);
        add(UIComponentUtils.createAlignedLabel("Purchase Cost:"));
        add(purchaseCostField);
        add(UIComponentUtils.createAlignedLabel("Vendor:"));
        add(vendorField);
        add(UIComponentUtils.createAlignedLabel("Warranty Expiry Date:"));
        add(warrantyExpiryDatePicker_div);
        add(UIComponentUtils.createAlignedLabel("Date of Purchase:"));
        add(dateOfPurchasePicker_div);
        add(enterButton);
        add(clearButton);
    }

    private Map<String, String> collectData() {
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
        return data;
    }

    private void clearForm() {
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
    }
}