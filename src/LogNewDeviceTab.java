import java.awt.*;
import javax.swing.*;

public class LogNewDeviceTab extends JPanel {

    private JPanel warrantyExpiryDatePicker_div;
    private JPanel maintenanceDatesPicker_div;

    private JPanel computerPanel;
    private JPanel printerPanel;
    private JPanel routerPanel;
    private JPanel switchPanel;

    public LogNewDeviceTab() {
        setLayout(new BorderLayout(10, 10));

        // Create the dropdown for device selection
        JComboBox<String> deviceTypeCombo = UIUtils.createFormattedComboBox(
            new String[]{"Computer", "Printer", "Router", "Switch"}
        );
        JPanel topPanel = new JPanel();
        topPanel.add(UIUtils.createAlignedLabel("Select Device Type:"));
        topPanel.add(deviceTypeCombo);

        // Initialize the panels for each device type
        computerPanel = createComputerPanel();
        printerPanel = createPrinterPanel();
        routerPanel = createRouterPanel();
        switchPanel = createSwitchPanel();

        // Create a scrollable content panel
        JPanel contentPanel = new JPanel();
        JScrollPane scrollPane = UIUtils.createScrollableContentPanel(contentPanel);
        contentPanel.add(computerPanel); // Default to computer panel
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Action listener for the device combo box
        deviceTypeCombo.addActionListener(e -> {
            String selectedDeviceType = (String) deviceTypeCombo.getSelectedItem();
            contentPanel.removeAll();
            switch (selectedDeviceType) {
                case "Computer":
                    contentPanel.add(computerPanel);
                    break;
                case "Printer":
                    contentPanel.add(printerPanel);
                    break;
                case "Router":
                    contentPanel.add(routerPanel);
                    break;
                case "Switch":
                    contentPanel.add(switchPanel);
                    break;
            }
            contentPanel.revalidate();
            contentPanel.repaint();
        });

        // Set initial selection
        deviceTypeCombo.setSelectedItem("Computer");
    }

    private JPanel createComputerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField deviceNameField = UIUtils.createFormattedTextField();
        JTextField deviceTypeField = UIUtils.createFormattedTextField();
        JTextField brandField = UIUtils.createFormattedTextField();
        JTextField modelField = UIUtils.createFormattedTextField();
        JTextField serialNumberField = UIUtils.createFormattedTextField();
        JTextField processorTypeField = UIUtils.createFormattedTextField();
        JTextField storageCapacityField = UIUtils.createFormattedTextField();
        JTextField networkAddressField = UIUtils.createFormattedTextField();
        JTextField departmentField = UIUtils.createFormattedTextField();
        JTextField purchaseCostField = UIUtils.createFormattedTextField();
        JTextField vendorField = UIUtils.createFormattedTextField();
        JTextField osVersionField = UIUtils.createFormattedTextField();
        JTextField roomDeskField = UIUtils.createFormattedTextField();
        JTextField specificationField = UIUtils.createFormattedTextField();
        JTextField assignedUserField = UIUtils.createFormattedTextField();

        JComboBox<String> addedMemoryCombo = UIUtils.createFormattedComboBox(
            new String[]{"TRUE", "FALSE", "null"}
        );
        JComboBox<String> addedStorageCombo = UIUtils.createFormattedComboBox(
            new String[]{"TRUE", "FALSE", "null"}
        );
        JComboBox<String> statusCombo = UIUtils.createFormattedComboBox(
            new String[]{"Deployed", "In Storage"}
        );

        warrantyExpiryDatePicker_div = UIUtils.createFormattedDatePicker();
        maintenanceDatesPicker_div = UIUtils.createFormattedDatePicker();

        JButton enterButton = UIUtils.createFormattedButton("Enter");
        enterButton.addActionListener(e -> {
            printDeviceDetails("Device Name", deviceNameField);
            printDeviceDetails("Device Type", deviceTypeField);
            printDeviceDetails("Brand", brandField);
            printDeviceDetails("Model", modelField);
            printDeviceDetails("Serial Number", serialNumberField);
            printDeviceDetails("Processor Type", processorTypeField);
            printDeviceDetails("Storage Capacity", storageCapacityField);
            printDeviceDetails("Network Address", networkAddressField);
            printDeviceDetails("Department", departmentField);
            printDeviceDetails("Status", statusCombo);
            printDeviceDetails("Purchase Cost", purchaseCostField);
            printDeviceDetails("Vendor", vendorField);
            printDeviceDetails("OS Version", osVersionField);
            printDeviceDetails("Room/Desk", roomDeskField);
            printDeviceDetails("Specification", specificationField);
            printDeviceDetails("Assigned User", assignedUserField);
            printDeviceDetails("Added Memory", addedMemoryCombo);
            printDeviceDetails("Added Storage", addedStorageCombo);
            printDeviceDetails("Warranty Expiry Date", warrantyExpiryDatePicker_div);
            printDeviceDetails("Maintenance Dates", maintenanceDatesPicker_div);
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
        panel.add(UIUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(UIUtils.createAlignedLabel("OS Version:"));
        panel.add(osVersionField);
        panel.add(UIUtils.createAlignedLabel("Room/Desk:"));
        panel.add(roomDeskField);
        panel.add(UIUtils.createAlignedLabel("Specification:"));
        panel.add(specificationField);
        panel.add(UIUtils.createAlignedLabel("Assigned User:"));
        panel.add(assignedUserField);
        panel.add(UIUtils.createAlignedLabel("Added Memory:"));
        panel.add(addedMemoryCombo);
        panel.add(UIUtils.createAlignedLabel("Added Storage:"));
        panel.add(addedStorageCombo);
        panel.add(UIUtils.createAlignedLabel("Warranty Expiry Date:"));
        panel.add(warrantyExpiryDatePicker_div);
        panel.add(UIUtils.createAlignedLabel("Maintenance Dates:"));
        panel.add(maintenanceDatesPicker_div);
        panel.add(enterButton);

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
        JComboBox<String> statusCombo = UIUtils.createFormattedComboBox(
            new String[]{"Deployed", "In Storage"}
        );

        JButton enterButton = UIUtils.createFormattedButton("Enter");
        enterButton.addActionListener(e -> {
            printDeviceDetails("Device Name", deviceNameField);
            printDeviceDetails("Brand", brandField);
            printDeviceDetails("Model", modelField);
            printDeviceDetails("Serial Number", serialNumberField);
            printDeviceDetails("Status", statusCombo);
            printDeviceDetails("Purchase Cost", purchaseCostField);
            printDeviceDetails("Vendor", vendorField);
        });

        panel.add(UIUtils.createAlignedLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(UIUtils.createAlignedLabel("Brand:"));
        panel.add(brandField);
        panel.add(UIUtils.createAlignedLabel("Model:"));
        panel.add(modelField);
        panel.add(UIUtils.createAlignedLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(UIUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(enterButton);

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
        JComboBox<String> statusCombo = UIUtils.createFormattedComboBox(
            new String[]{"Deployed", "In Storage"}
        );

        JButton enterButton = UIUtils.createFormattedButton("Enter");
        enterButton.addActionListener(e -> {
            printDeviceDetails("Device Name", deviceNameField);
            printDeviceDetails("Model", modelField);
            printDeviceDetails("Serial Number", serialNumberField);
            printDeviceDetails("Network Address", networkAddressField);
            printDeviceDetails("Status", statusCombo);
            printDeviceDetails("Purchase Cost", purchaseCostField);
            printDeviceDetails("Vendor", vendorField);
        });

        panel.add(UIUtils.createAlignedLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(UIUtils.createAlignedLabel("Model:"));
        panel.add(modelField);
        panel.add(UIUtils.createAlignedLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(UIUtils.createAlignedLabel("Network Address:"));
        panel.add(networkAddressField);
        panel.add(UIUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(enterButton);

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
        JComboBox<String> statusCombo = UIUtils.createFormattedComboBox(
            new String[]{"Deployed", "In Storage"}
        );

        JButton enterButton = UIUtils.createFormattedButton("Enter");
        enterButton.addActionListener(e -> {
            printDeviceDetails("Device Name", deviceNameField);
            printDeviceDetails("Model", modelField);
            printDeviceDetails("Serial Number", serialNumberField);
            printDeviceDetails("Network Address", networkAddressField);
            printDeviceDetails("Status", statusCombo);
            printDeviceDetails("Purchase Cost", purchaseCostField);
            printDeviceDetails("Vendor", vendorField);
        });

        panel.add(UIUtils.createAlignedLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(UIUtils.createAlignedLabel("Model:"));
        panel.add(modelField);
        panel.add(UIUtils.createAlignedLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(UIUtils.createAlignedLabel("Network Address:"));
        panel.add(networkAddressField);
        panel.add(UIUtils.createAlignedLabel("Status:"));
        panel.add(statusCombo);
        panel.add(UIUtils.createAlignedLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(UIUtils.createAlignedLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(enterButton);

        return panel;
    }

    private static void printDeviceDetails(String label, JComponent component) {
        String value = "";
        if (component instanceof JTextField) {
            value = ((JTextField) component).getText();
        } else if (component instanceof JComboBox) {
            value = (String) ((JComboBox<?>) component).getSelectedItem();
        } else if (component instanceof JPanel) {
            value = UIUtils.getDateFromPicker((JPanel) component);
        }
        System.out.println(label + ": " + (value.isEmpty() ? "null" : value));
    }
}