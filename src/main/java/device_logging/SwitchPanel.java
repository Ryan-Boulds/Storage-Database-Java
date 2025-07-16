package device_logging;

import java.awt.GridLayout;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import utils.DataUtils;
import utils.InventoryData;
import utils.SQLGenerator;
import utils.UIComponentUtils;

public class SwitchPanel extends JPanel {
    private final JTextField deviceNameField = UIComponentUtils.createFormattedTextField();
    private final JTextField modelField = UIComponentUtils.createFormattedTextField();
    private final JTextField serialNumberField = UIComponentUtils.createFormattedTextField();
    private final JTextField networkAddressField = UIComponentUtils.createFormattedTextField();
    private final JTextField purchaseCostField = UIComponentUtils.createFormattedTextField();
    private final JTextField vendorField = UIComponentUtils.createFormattedTextField();
    private final JTextField specificationField = UIComponentUtils.createFormattedTextField();
    private final JTextField departmentField = UIComponentUtils.createFormattedTextField();
    private final JTextField buildingLocationField = UIComponentUtils.createFormattedTextField();
    private final JTextField roomDeskField = UIComponentUtils.createFormattedTextField();
    private final JComboBox<String> statusCombo = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
    private final JPanel warrantyExpiryDatePicker_div = UIComponentUtils.createFormattedDatePicker();
    private final JPanel dateOfPurchasePicker_div = UIComponentUtils.createFormattedDatePicker();
    private final JLabel statusLabel;

    public SwitchPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        setLayout(new GridLayout(0, 2, 5, 5));
        addComponents();
    }

    private void addComponents() {
        JButton enterButton = UIComponentUtils.createFormattedButton("Enter");
        JButton clearButton = UIComponentUtils.createFormattedButton("Clear Form");

        enterButton.addActionListener(e -> {
            HashMap<String, String> data = collectData();
            String error = DataUtils.validateDevice(data);
            if (error != null) {
                statusLabel.setText("Error: " + error);
                JOptionPane.showMessageDialog(this, error, "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String sql = SQLGenerator.formatDeviceSQL(data);
            System.out.println("[SwitchPanel] " + sql);
            try {
                InventoryData.saveDevice(data);
                statusLabel.setText("Device saved successfully");
                JOptionPane.showMessageDialog(this, "Device saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
            } catch (RuntimeException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
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

    private HashMap<String, String> collectData() {
        HashMap<String, String> data = new HashMap<>();
        data.put("Device_Name", DataUtils.capitalizeWords(deviceNameField.getText()));
        data.put("Device_Type", "Switch");
        data.put("Model", DataUtils.capitalizeWords(modelField.getText()));
        data.put("Serial_Number", serialNumberField.getText());
        data.put("Network_Address", networkAddressField.getText());
        data.put("Specification", specificationField.getText());
        data.put("Department", DataUtils.capitalizeWords(departmentField.getText()));
        data.put("Building_Location", DataUtils.capitalizeWords(buildingLocationField.getText()));
        data.put("Room_Desk", DataUtils.capitalizeWords(roomDeskField.getText()));
        data.put("Status", (String) statusCombo.getSelectedItem());
        data.put("Purchase_Cost", purchaseCostField.getText());
        data.put("Vendor", DataUtils.capitalizeWords(vendorField.getText()));
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
        ((JTextField) warrantyExpiryDatePicker_div.getComponent(0)).setText("");
        ((JTextField) dateOfPurchasePicker_div.getComponent(0)).setText("");
        statusLabel.setText("Form cleared");
    }
}