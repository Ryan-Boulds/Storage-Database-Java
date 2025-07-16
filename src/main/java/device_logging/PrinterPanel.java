package device_logging;

import java.awt.GridLayout;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import utils.DataUtils;
import utils.InventoryData;
import utils.UIComponentUtils;

public class PrinterPanel extends JPanel {
    private final JTextField deviceNameField = UIComponentUtils.createFormattedTextField();
    private final JTextField brandField = UIComponentUtils.createFormattedTextField();
    private final JTextField modelField = UIComponentUtils.createFormattedTextField();
    private final JTextField serialNumberField = UIComponentUtils.createFormattedTextField();
    private final JTextField buildingLocationField = UIComponentUtils.createFormattedTextField();
    private final JTextField roomDeskField = UIComponentUtils.createFormattedTextField();
    private final JTextField networkAddressField = UIComponentUtils.createFormattedTextField();
    private final JTextField statusField = UIComponentUtils.createFormattedTextField();
    private final JTextField assignedUserField = UIComponentUtils.createFormattedTextField();
    private final JTextField warrantyExpiryField = UIComponentUtils.createFormattedTextField();
    private final JTextField lastMaintenanceField = UIComponentUtils.createFormattedTextField();
    private final JTextField maintenanceDueField = UIComponentUtils.createFormattedTextField();
    private final JTextField dateOfPurchaseField = UIComponentUtils.createFormattedTextField();
    private final JTextField purchaseCostField = UIComponentUtils.createFormattedTextField();
    private final JTextField vendorField = UIComponentUtils.createFormattedTextField();
    private final JLabel statusLabel;

    public PrinterPanel(JLabel statusLabel) {
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
        add(UIComponentUtils.createAlignedLabel("Network Address:"));
        add(networkAddressField);
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

        JButton saveButton = UIComponentUtils.createFormattedButton("Save Device");
        saveButton.addActionListener(e -> saveDevice());

        add(saveButton);
        add(new JPanel()); // Placeholder for alignment
    }

    private void saveDevice() {
        HashMap<String, String> deviceData = new HashMap<>();
        deviceData.put("Device_Name", DataUtils.capitalizeWords(deviceNameField.getText()));
        deviceData.put("Device_Type", "Printer");
        deviceData.put("Brand", DataUtils.capitalizeWords(brandField.getText()));
        deviceData.put("Model", DataUtils.capitalizeWords(modelField.getText()));
        deviceData.put("Serial_Number", serialNumberField.getText());
        deviceData.put("Building_Location", DataUtils.capitalizeWords(buildingLocationField.getText()));
        deviceData.put("Room_Desk", DataUtils.capitalizeWords(roomDeskField.getText()));
        deviceData.put("Network_Address", networkAddressField.getText());
        deviceData.put("Status", DataUtils.capitalizeWords(statusField.getText()));
        deviceData.put("Assigned_User", DataUtils.capitalizeWords(assignedUserField.getText()));
        deviceData.put("Warranty_Expiry_Date", warrantyExpiryField.getText());
        deviceData.put("Last_Maintenance", lastMaintenanceField.getText());
        deviceData.put("Maintenance_Due", maintenanceDueField.getText());
        deviceData.put("Date_Of_Purchase", dateOfPurchaseField.getText());
        deviceData.put("Purchase_Cost", purchaseCostField.getText());
        deviceData.put("Vendor", DataUtils.capitalizeWords(vendorField.getText()));

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

    private void clearFields() {
        deviceNameField.setText("");
        brandField.setText("");
        modelField.setText("");
        serialNumberField.setText("");
        buildingLocationField.setText("");
        roomDeskField.setText("");
        networkAddressField.setText("");
        statusField.setText("");
        assignedUserField.setText("");
        warrantyExpiryField.setText("");
        lastMaintenanceField.setText("");
        maintenanceDueField.setText("");
        dateOfPurchaseField.setText("");
        purchaseCostField.setText("");
        vendorField.setText("");
    }
}