package device_logging;

import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;

import utils.DataUtils;
import utils.InventoryData;
import utils.SQLGenerator;
import utils.UIComponentUtils;

public class SwitchPanel extends JPanel {
    private final JTextField deviceNameField = createAutoCompleteTextField("Device_Name");
    private final JTextField modelField = createAutoCompleteTextField("Model");
    private final JTextField serialNumberField = UIComponentUtils.createFormattedTextField();
    private final JTextField networkAddressField = UIComponentUtils.createFormattedTextField();
    private final JFormattedTextField purchaseCostField = new JFormattedTextField(createNumberFormatter());
    private final JTextField vendorField = UIComponentUtils.createFormattedTextField();
    private final JTextField specificationField = UIComponentUtils.createFormattedTextField();
    private final JTextField departmentField = createAutoCompleteTextField("Department");
    private final JTextField buildingLocationField = createAutoCompleteTextField("Building_Location");
    private final JTextField roomDeskField = createAutoCompleteTextField("Room_Desk");
    private final JComboBox<String> statusCombo = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
    private final JPanel warrantyExpiryDatePicker_div = UIComponentUtils.createFormattedDatePicker();
    private final JPanel dateOfPurchasePicker_div = UIComponentUtils.createFormattedDatePicker();
    private final JLabel statusLabel;

    public SwitchPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        setLayout(new GridLayout(0, 2, 5, 5));
        addComponents();
    }

    private JTextField createAutoCompleteTextField(String fieldName) {
        JTextField textField = UIComponentUtils.createFormattedTextField();
        JPopupMenu popup = new JPopupMenu();
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSuggestions(fieldName, textField, popup);
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSuggestions(fieldName, textField, popup);
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSuggestions(fieldName, textField, popup);
            }
        });
        textField.setComponentPopupMenu(popup);
        return textField;
    }

    private void updateSuggestions(String fieldName, JTextField textField, JPopupMenu popup) {
        popup.removeAll();
        String text = textField.getText().trim().toLowerCase();
        if (!text.isEmpty()) {
            List<String> suggestions = getUniqueValues(fieldName); // Use fieldName
            for (String suggestion : suggestions) {
                if (suggestion.toLowerCase().startsWith(text)) {
                    JMenuItem item = new JMenuItem(suggestion);
                    item.addActionListener(e -> textField.setText(suggestion));
                    popup.add(item);
                }
            }
            if (popup.getComponentCount() > 0) {
                popup.show(textField, 0, textField.getHeight());
            }
        }
    }

    private List<String> getUniqueValues(String fieldName) {
        // Placeholder: Replace with actual database query
        return new ArrayList<>(); // Return empty list or implement with InventoryData.query
    }

    private NumberFormatter createNumberFormatter() {
        NumberFormat format = NumberFormat.getNumberInstance();
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Double.class);
        formatter.setAllowsInvalid(false);
        formatter.setMinimum(0.0);
        return formatter;
    }

    private void addComponents() {
        JButton enterButton = UIComponentUtils.createFormattedButton("Enter");
        JButton clearButton = UIComponentUtils.createFormattedButton("Clear Form");

        enterButton.addActionListener(e -> {
            HashMap<String, String> data = collectData();
            String validationError = validateFieldTypes(data);
            if (validationError != null) {
                statusLabel.setText("Error: " + validationError);
                JOptionPane.showMessageDialog(this, validationError, "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
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
        String deviceName = deviceNameField.getText().trim();
        data.put("Device_Name", deviceName.isEmpty() ? null : DataUtils.capitalizeWords(deviceName));
        data.put("Device_Type", "Switch");
        String model = modelField.getText().trim();
        data.put("Model", model.isEmpty() ? null : DataUtils.capitalizeWords(model));
        String serialNumber = serialNumberField.getText().trim();
        data.put("Serial_Number", serialNumber.isEmpty() ? null : serialNumber);
        String networkAddress = networkAddressField.getText().trim();
        data.put("Network_Address", networkAddress.isEmpty() ? null : networkAddress);
        String specification = specificationField.getText().trim();
        data.put("Specification", specification.isEmpty() ? null : specification);
        String department = departmentField.getText().trim();
        data.put("Department", department.isEmpty() ? null : DataUtils.capitalizeWords(department));
        String buildingLocation = buildingLocationField.getText().trim();
        data.put("Building_Location", buildingLocation.isEmpty() ? null : DataUtils.capitalizeWords(buildingLocation));
        String roomDesk = roomDeskField.getText().trim();
        data.put("Room_Desk", roomDesk.isEmpty() ? null : DataUtils.capitalizeWords(roomDesk));
        data.put("Status", (String) statusCombo.getSelectedItem());
        String purchaseCost = purchaseCostField.getText().trim();
        data.put("Purchase_Cost", purchaseCost.isEmpty() ? null : purchaseCost);
        String vendor = vendorField.getText().trim();
        data.put("Vendor", vendor.isEmpty() ? null : DataUtils.capitalizeWords(vendor));
        String warrantyExpiry = UIComponentUtils.getDateFromPicker(warrantyExpiryDatePicker_div);
        data.put("Warranty_Expiry_Date", warrantyExpiry == null || warrantyExpiry.trim().isEmpty() ? null : convertDateFormat(warrantyExpiry));
        String dateOfPurchase = UIComponentUtils.getDateFromPicker(dateOfPurchasePicker_div);
        data.put("Date_Of_Purchase", dateOfPurchase == null || dateOfPurchase.trim().isEmpty() ? null : convertDateFormat(dateOfPurchase));
        return data;
    }

    private String convertDateFormat(String date) {
        if (date == null || !date.matches("\\d{2}-\\d{2}-\\d{4}")) return date;
        String[] parts = date.split("-");
        return parts[2] + "-" + parts[0] + "-" + parts[1]; // YYYY-MM-DD
    }

    private String validateFieldTypes(HashMap<String, String> data) {
        try {
            String purchaseCost = data.get("Purchase_Cost");
            if (purchaseCost != null && !purchaseCost.trim().isEmpty()) {
                Double.parseDouble(purchaseCost);
            }
            for (String dateField : new String[]{"Warranty_Expiry_Date", "Date_Of_Purchase"}) {
                String date = data.get(dateField);
                if (date != null && !date.trim().isEmpty() && !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return "Invalid date format for " + dateField + ". Use YYYY-MM-DD.";
                }
            }
        } catch (NumberFormatException e) {
            return "Numeric fields (e.g., Purchase Cost) must contain valid numbers.";
        }
        return null;
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