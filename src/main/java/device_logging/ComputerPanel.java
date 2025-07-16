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
import utils.UIComponentUtils;

public class ComputerPanel extends JPanel {

    private final JTextField deviceNameField = UIComponentUtils.createFormattedTextField();
    private final JTextField brandField = createAutoCompleteTextField("Brand");
    private final JTextField modelField = createAutoCompleteTextField("Model");
    private final JTextField serialNumberField = UIComponentUtils.createFormattedTextField();
    private final JTextField buildingLocationField = createAutoCompleteTextField("Building_Location");
    private final JTextField roomDeskField = createAutoCompleteTextField("Room_Desk");
    private final JTextField specificationField = UIComponentUtils.createFormattedTextField();
    private final JTextField processorTypeField = createAutoCompleteTextField("Processor_Type");
    private final JTextField storageCapacityField = createAutoCompleteTextField("Storage_Capacity");
    private final JTextField networkAddressField = createAutoCompleteTextField("Network_Address");
    private final JTextField osVersionField = createAutoCompleteTextField("OS_Version");
    private final JTextField departmentField = createAutoCompleteTextField("Department");
    private final JComboBox<String> addedMemoryField = UIComponentUtils.createFormattedComboBox(new String[]{"TRUE", "FALSE", "null"});
    private final JComboBox<String> statusField = UIComponentUtils.createFormattedComboBox(new String[]{"Deployed", "In Storage", "Needs Repair"});
    private final JTextField assignedUserField = createAutoCompleteTextField("Assigned_User");
    private final JPanel warrantyExpiryField = UIComponentUtils.createFormattedDatePicker();
    private final JPanel lastMaintenanceField = UIComponentUtils.createFormattedDatePicker();
    private final JPanel maintenanceDueField = UIComponentUtils.createFormattedDatePicker();
    private final JPanel dateOfPurchaseField = UIComponentUtils.createFormattedDatePicker();
    private final JFormattedTextField purchaseCostField = new JFormattedTextField(createNumberFormatter());
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
            List<String> suggestions = getUniqueValues(fieldName);
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

    private void saveDevice() {
        HashMap<String, String> deviceData = new HashMap<>();
        String deviceName = deviceNameField.getText().trim();
        deviceData.put("Device_Name", deviceName.isEmpty() ? null : DataUtils.capitalizeWords(deviceName));
        deviceData.put("Device_Type", "Computer");
        String brand = brandField.getText().trim();
        deviceData.put("Brand", brand.isEmpty() ? null : DataUtils.capitalizeWords(brand));
        String model = modelField.getText().trim();
        deviceData.put("Model", model.isEmpty() ? null : DataUtils.capitalizeWords(model));
        String serialNumber = serialNumberField.getText().trim();
        deviceData.put("Serial_Number", serialNumber.isEmpty() ? null : serialNumber);
        String buildingLocation = buildingLocationField.getText().trim();
        deviceData.put("Building_Location", buildingLocation.isEmpty() ? null : DataUtils.capitalizeWords(buildingLocation));
        String roomDesk = roomDeskField.getText().trim();
        deviceData.put("Room_Desk", roomDesk.isEmpty() ? null : DataUtils.capitalizeWords(roomDesk));
        String specification = specificationField.getText().trim();
        deviceData.put("Specification", specification.isEmpty() ? null : specification);
        String processorType = processorTypeField.getText().trim();
        deviceData.put("Processor_Type", processorType.isEmpty() ? null : processorType);
        String storageCapacity = storageCapacityField.getText().trim();
        deviceData.put("Storage_Capacity", storageCapacity.isEmpty() ? null : storageCapacity);
        String networkAddress = networkAddressField.getText().trim();
        deviceData.put("Network_Address", networkAddress.isEmpty() ? null : networkAddress);
        String osVersion = osVersionField.getText().trim();
        deviceData.put("OS_Version", osVersion.isEmpty() ? null : osVersion);
        String department = departmentField.getText().trim();
        deviceData.put("Department", department.isEmpty() ? null : DataUtils.capitalizeWords(department));
        deviceData.put("Added_Memory", (String) addedMemoryField.getSelectedItem());
        deviceData.put("Status", (String) statusField.getSelectedItem());
        String assignedUser = assignedUserField.getText().trim();
        deviceData.put("Assigned_User", assignedUser.isEmpty() ? null : DataUtils.capitalizeWords(assignedUser));
        String warrantyExpiry = UIComponentUtils.getDateFromPicker(warrantyExpiryField);
        deviceData.put("Warranty_Expiry_Date", warrantyExpiry == null || warrantyExpiry.trim().isEmpty() ? null : convertDateFormat(warrantyExpiry));
        String lastMaintenance = UIComponentUtils.getDateFromPicker(lastMaintenanceField);
        deviceData.put("Last_Maintenance", lastMaintenance == null || lastMaintenance.trim().isEmpty() ? null : convertDateFormat(lastMaintenance));
        String maintenanceDue = UIComponentUtils.getDateFromPicker(maintenanceDueField);
        deviceData.put("Maintenance_Due", maintenanceDue == null || maintenanceDue.trim().isEmpty() ? null : convertDateFormat(maintenanceDue));
        String dateOfPurchase = UIComponentUtils.getDateFromPicker(dateOfPurchaseField);
        deviceData.put("Date_Of_Purchase", dateOfPurchase == null || dateOfPurchase.trim().isEmpty() ? null : convertDateFormat(dateOfPurchase));
        String purchaseCost = purchaseCostField.getText().trim();
        deviceData.put("Purchase_Cost", purchaseCost.isEmpty() ? null : purchaseCost);
        String vendor = vendorField.getText().trim();
        deviceData.put("Vendor", vendor.isEmpty() ? null : DataUtils.capitalizeWords(vendor));
        String memoryRam = memoryRamField.getText().trim();
        deviceData.put("Memory_RAM", memoryRam.isEmpty() ? null : memoryRam);

        String validationError = validateFieldTypes(deviceData);
        if (validationError != null) {
            statusLabel.setText(validationError);
            JOptionPane.showMessageDialog(this, validationError, "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

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

    private String convertDateFormat(String date) {
        if (date == null || !date.matches("\\d{2}-\\d{2}-\\d{4}")) return date;
        String[] parts = date.split("-");
        return parts[2] + "-" + parts[0] + "-" + parts[1]; // YYYY-MM-DD
    }

    private String validateFieldTypes(HashMap<String, String> deviceData) {
        try {
            String purchaseCost = deviceData.get("Purchase_Cost");
            if (purchaseCost != null && !purchaseCost.trim().isEmpty()) {
                Double.parseDouble(purchaseCost);
            }
            String memoryRam = deviceData.get("Memory_RAM");
            if (memoryRam != null && !memoryRam.trim().isEmpty()) {
                if (memoryRam.toUpperCase().endsWith("GB")) {
                    String numericPart = memoryRam.replaceAll("[^0-9.]", "");
                    Double.parseDouble(numericPart);
                } else {
                    Double.parseDouble(memoryRam);
                }
            }
            for (String dateField : new String[]{"Warranty_Expiry_Date", "Last_Maintenance", "Maintenance_Due", "Date_Of_Purchase"}) {
                String date = deviceData.get(dateField);
                if (date != null && !date.trim().isEmpty() && !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return "Invalid date format for " + dateField + ". Use YYYY-MM-DD.";
                }
            }
        } catch (NumberFormatException e) {
            return "Numeric fields (e.g., Purchase Cost, Memory RAM) must contain valid numbers.";
        }
        return null;
    }

    private void saveTemplate() {
        String templateName = JOptionPane.showInputDialog(this, "Enter template name:");
        if (templateName == null || templateName.trim().isEmpty()) {
            statusLabel.setText("Template name is required");
            JOptionPane.showMessageDialog(this, "Template name is required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        HashMap<String, String> templateData = new HashMap<>();
        String deviceName = deviceNameField.getText().trim();
        templateData.put("Device_Name", deviceName.isEmpty() ? null : DataUtils.capitalizeWords(deviceName));
        templateData.put("Device_Type", "Computer");
        String brand = brandField.getText().trim();
        templateData.put("Brand", brand.isEmpty() ? null : DataUtils.capitalizeWords(brand));
        String model = modelField.getText().trim();
        templateData.put("Model", model.isEmpty() ? null : DataUtils.capitalizeWords(model));
        String serialNumber = serialNumberField.getText().trim();
        templateData.put("Serial_Number", serialNumber.isEmpty() ? null : serialNumber);
        String buildingLocation = buildingLocationField.getText().trim();
        templateData.put("Building_Location", buildingLocation.isEmpty() ? null : DataUtils.capitalizeWords(buildingLocation));
        String roomDesk = roomDeskField.getText().trim();
        templateData.put("Room_Desk", roomDesk.isEmpty() ? null : DataUtils.capitalizeWords(roomDesk));
        String specification = specificationField.getText().trim();
        templateData.put("Specification", specification.isEmpty() ? null : specification);
        String processorType = processorTypeField.getText().trim();
        templateData.put("Processor_Type", processorType.isEmpty() ? null : processorType);
        String storageCapacity = storageCapacityField.getText().trim();
        templateData.put("Storage_Capacity", storageCapacity.isEmpty() ? null : storageCapacity);
        String networkAddress = networkAddressField.getText().trim();
        templateData.put("Network_Address", networkAddress.isEmpty() ? null : networkAddress);
        String osVersion = osVersionField.getText().trim();
        templateData.put("OS_Version", osVersion.isEmpty() ? null : osVersion);
        String department = departmentField.getText().trim();
        templateData.put("Department", department.isEmpty() ? null : DataUtils.capitalizeWords(department));
        templateData.put("Added_Memory", (String) addedMemoryField.getSelectedItem());
        templateData.put("Status", (String) statusField.getSelectedItem());
        String assignedUser = assignedUserField.getText().trim();
        templateData.put("Assigned_User", assignedUser.isEmpty() ? null : DataUtils.capitalizeWords(assignedUser));
        String warrantyExpiry = UIComponentUtils.getDateFromPicker(warrantyExpiryField);
        templateData.put("Warranty_Expiry_Date", warrantyExpiry == null || warrantyExpiry.trim().isEmpty() ? null : convertDateFormat(warrantyExpiry));
        String lastMaintenance = UIComponentUtils.getDateFromPicker(lastMaintenanceField);
        templateData.put("Last_Maintenance", lastMaintenance == null || lastMaintenance.trim().isEmpty() ? null : convertDateFormat(lastMaintenance));
        String maintenanceDue = UIComponentUtils.getDateFromPicker(maintenanceDueField);
        templateData.put("Maintenance_Due", maintenanceDue == null || maintenanceDue.trim().isEmpty() ? null : convertDateFormat(maintenanceDue));
        String dateOfPurchase = UIComponentUtils.getDateFromPicker(dateOfPurchaseField);
        templateData.put("Date_Of_Purchase", dateOfPurchase == null || dateOfPurchase.trim().isEmpty() ? null : convertDateFormat(dateOfPurchase));
        String purchaseCost = purchaseCostField.getText().trim();
        templateData.put("Purchase_Cost", purchaseCost.isEmpty() ? null : purchaseCost);
        String vendor = vendorField.getText().trim();
        templateData.put("Vendor", vendor.isEmpty() ? null : DataUtils.capitalizeWords(vendor));
        String memoryRam = memoryRamField.getText().trim();
        templateData.put("Memory_RAM", memoryRam.isEmpty() ? null : memoryRam);

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
                ((JTextField) warrantyExpiryField.getComponent(0)).setText(template.getOrDefault("Warranty_Expiry_Date", ""));
                ((JTextField) lastMaintenanceField.getComponent(0)).setText(template.getOrDefault("Last_Maintenance", ""));
                ((JTextField) maintenanceDueField.getComponent(0)).setText(template.getOrDefault("Maintenance_Due", ""));
                ((JTextField) dateOfPurchaseField.getComponent(0)).setText(template.getOrDefault("Date_Of_Purchase", ""));
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
        ((JTextField) warrantyExpiryField.getComponent(0)).setText("");
        ((JTextField) lastMaintenanceField.getComponent(0)).setText("");
        ((JTextField) maintenanceDueField.getComponent(0)).setText("");
        ((JTextField) dateOfPurchaseField.getComponent(0)).setText("");
        purchaseCostField.setText("");
        vendorField.setText("");
        memoryRamField.setText("");
    }
}