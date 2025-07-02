import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Date;

public class LogNewDeviceTab extends JPanel {

    private JSpinner warrantyExpiryDatePicker;
    private JSpinner maintenanceDatesPicker;

    // Declare panels for each device type
    private JPanel computerPanel;
    private JPanel printerPanel;
    private JPanel routerPanel;
    private JPanel switchPanel;

    public LogNewDeviceTab() {
        setLayout(new BorderLayout(10, 10)); // Use BorderLayout for better control

        // Create the dropdown for device selection
        JComboBox<String> deviceTypeCombo = new JComboBox<>(new String[]{"Computer", "Printer", "Router", "Switch"});
        deviceTypeCombo.setPreferredSize(new Dimension(200, 30)); // Fixed size
        JPanel topPanel = new JPanel(); // Create a top panel to hold the dropdown and label
        topPanel.add(new JLabel("Select Device Type:"));
        topPanel.add(deviceTypeCombo);

        // Initialize the panels for each device type
        computerPanel = createComputerPanel();
        printerPanel = createPrinterPanel();
        routerPanel = createRouterPanel();
        switchPanel = createSwitchPanel();

        // Set the default panel (computer panel)
        JPanel currentPanel = computerPanel;

        // Create a scrollable panel for the content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS)); // Stack vertically
        JScrollPane scrollPane = new JScrollPane(contentPanel); // Make the content scrollable
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Add the initial panel (default to computer)
        contentPanel.add(currentPanel);

        // Action listener for the device combo box
        deviceTypeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedDeviceType = (String) deviceTypeCombo.getSelectedItem();

                // Remove all components from the contentPanel and add the selected device type panel
                contentPanel.removeAll();

                // Add the appropriate panel based on the selected device type
                if ("Computer".equals(selectedDeviceType)) {
                    contentPanel.add(computerPanel);
                } else if ("Printer".equals(selectedDeviceType)) {
                    contentPanel.add(printerPanel);
                } else if ("Router".equals(selectedDeviceType)) {
                    contentPanel.add(routerPanel);
                } else if ("Switch".equals(selectedDeviceType)) {
                    contentPanel.add(switchPanel);
                }

                // Revalidate and repaint the content to update the view
                contentPanel.revalidate();
                contentPanel.repaint();
            }
        });

        // Set initial selection (defaults to "Computer")
        deviceTypeCombo.setSelectedItem("Computer");
    }

    // Method to create the Computer panel (with all the fields)
    private JPanel createComputerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Stack components vertically

        // Create text fields for the device details
        JTextField deviceNameField = new JTextField();
        JTextField deviceTypeField = new JTextField();
        JTextField brandField = new JTextField();
        JTextField modelField = new JTextField();
        JTextField serialNumberField = new JTextField();
        JTextField processorTypeField = new JTextField();
        JTextField storageCapacityField = new JTextField();
        JTextField networkAddressField = new JTextField();
        JTextField departmentField = new JTextField();
        JTextField statusField = new JTextField();
        JTextField purchaseCostField = new JTextField();
        JTextField vendorField = new JTextField();
        JTextField osVersionField = new JTextField();
        JTextField roomDeskField = new JTextField();
        JTextField specificationField = new JTextField();
        JTextField assignedUserField = new JTextField();

        // Combo boxes for "Added Memory" and "Added Storage"
        JComboBox<String> addedMemoryCombo = new JComboBox<>(new String[]{"TRUE", "FALSE", "null"});
        JComboBox<String> addedStorageCombo = new JComboBox<>(new String[]{"TRUE", "FALSE", "null"});

        // Status dropdown (Deployed/In Storage)
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Deployed", "In Storage"});

        // Add date pickers for warranty expiry and maintenance dates
        warrantyExpiryDatePicker = new JSpinner(new SpinnerDateModel());
        maintenanceDatesPicker = new JSpinner(new SpinnerDateModel());

        // Create action listener for the Enter button
        JButton enterButton = new JButton("Enter");
        enterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Print all details when Enter button is pressed
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
                printDeviceDetails("Warranty Expiry Date", warrantyExpiryDatePicker);
                printDeviceDetails("Maintenance Dates", maintenanceDatesPicker);
            }
        });

        // Adding fields to the panel
        panel.add(new JLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(new JLabel("Device Type:"));
        panel.add(deviceTypeField);
        panel.add(new JLabel("Brand:"));
        panel.add(brandField);
        panel.add(new JLabel("Model:"));
        panel.add(modelField);
        panel.add(new JLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(new JLabel("Processor Type:"));
        panel.add(processorTypeField);
        panel.add(new JLabel("Storage Capacity:"));
        panel.add(storageCapacityField);
        panel.add(new JLabel("Network Address:"));
        panel.add(networkAddressField);
        panel.add(new JLabel("Department:"));
        panel.add(departmentField);
        panel.add(new JLabel("Status:"));
        panel.add(statusCombo);
        panel.add(new JLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(new JLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(new JLabel("OS Version:"));
        panel.add(osVersionField);
        panel.add(new JLabel("Room/Desk:"));
        panel.add(roomDeskField);
        panel.add(new JLabel("Specification:"));
        panel.add(specificationField);
        panel.add(new JLabel("Assigned User:"));
        panel.add(assignedUserField);
        panel.add(new JLabel("Added Memory:"));
        panel.add(addedMemoryCombo);
        panel.add(new JLabel("Added Storage:"));
        panel.add(addedStorageCombo);
        panel.add(new JLabel("Warranty Expiry Date:"));
        panel.add(warrantyExpiryDatePicker);
        panel.add(new JLabel("Maintenance Dates:"));
        panel.add(maintenanceDatesPicker);
        panel.add(enterButton);

        return panel;
    }

    // Method to create the Printer panel (exclude "Processor Type" and "Added Memory")
    private JPanel createPrinterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Stack components vertically

        // Create fields (same as computer, but exclude "Processor Type" and "Added Memory")
        JTextField deviceNameField = new JTextField();
        JTextField brandField = new JTextField();
        JTextField modelField = new JTextField();
        JTextField serialNumberField = new JTextField();
        JTextField statusField = new JTextField();
        JTextField purchaseCostField = new JTextField();
        JTextField vendorField = new JTextField();

        // Status dropdown (Deployed/In Storage)
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Deployed", "In Storage"});
        statusCombo.setPreferredSize(new Dimension(200, 30));

        JButton enterButton = new JButton("Enter");
        enterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                printDeviceDetails("Device Name", deviceNameField);
                printDeviceDetails("Brand", brandField);
                printDeviceDetails("Model", modelField);
                printDeviceDetails("Serial Number", serialNumberField);
                printDeviceDetails("Status", statusCombo);
                printDeviceDetails("Purchase Cost", purchaseCostField);
                printDeviceDetails("Vendor", vendorField);
            }
        });

        panel.add(new JLabel("Device Name:"));
        panel.add(deviceNameField);
        panel.add(new JLabel("Brand:"));
        panel.add(brandField);
        panel.add(new JLabel("Model:"));
        panel.add(modelField);
        panel.add(new JLabel("Serial Number:"));
        panel.add(serialNumberField);
        panel.add(new JLabel("Status:"));
        panel.add(statusCombo);
        panel.add(new JLabel("Purchase Cost:"));
        panel.add(purchaseCostField);
        panel.add(new JLabel("Vendor:"));
        panel.add(vendorField);
        panel.add(enterButton);

        return panel;
    }

    // Method to create the Router panel (exclude "Processor Type" and "Added Memory")
    private JPanel createRouterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Stack components vertically
        // Create router-specific fields
        JTextField deviceNameField = new JTextField();
        JTextField modelField = new JTextField();
        JTextField serialNumberField = new JTextField();
        JTextField networkAddressField = new JTextField();
        JTextField statusField = new JTextField();
        JTextField purchaseCostField = new JTextField();
        JTextField vendorField = new JTextField();
        // Add fields similarly to how it's done for Printer
        return panel;
    }

    // Method to create the Switch panel (exclude "Processor Type" and "Added Memory")
    private JPanel createSwitchPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Stack components vertically
        // Same as Router panel, but for Switch
        return panel;
    }

    // Helper method to print the device details with a label and value
    private static void printDeviceDetails(String label, JComponent component) {
        String value = "";

        if (component instanceof JTextField) {
            value = ((JTextField) component).getText();
        } else if (component instanceof JComboBox) {
            value = (String) ((JComboBox<?>) component).getSelectedItem();
        } else if (component instanceof JSpinner) {
            value = ((JSpinner) component).getValue().toString();
        }

        System.out.println(label + ": " + (value.isEmpty() ? "null" : value));
    }
}
