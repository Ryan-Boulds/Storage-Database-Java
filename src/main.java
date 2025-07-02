import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.util.Date;

public class main {

    public static void main(String[] args) {
        // Set up the frame
        JFrame frame = new JFrame("Tabbed Pane Example");
        frame.setSize(600, 600);

        // Create a tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Create the first panel with buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton button1 = new JButton("Button 1");
        button1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Button 1 pressed");
            }
        });

        JButton button2 = new JButton("Button 2");
        button2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Button 2 pressed");
            }
        });

        buttonPanel.add(button1);
        buttonPanel.add(button2);

        // Create the second panel with a text box and an Enter button
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new FlowLayout());

        JTextField textField = new JTextField(15);
        JButton enterButton = new JButton("Enter");

        // Action listener for the Enter button
        enterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Print the text field content to the terminal
                System.out.println("Text entered: " + textField.getText());
            }
        });

        // Add components to the second panel
        textPanel.add(new JLabel("Enter text and press Enter:"));
        textPanel.add(textField);
        textPanel.add(enterButton);

        // Create a demo tab
        JPanel demoPanel = new JPanel();
        demoPanel.setLayout(new FlowLayout()); // Ensure we're using FlowLayout for auto positioning

        // Create and add JLabel to the demo tab
        JLabel label1 = new JLabel("simple text");
        demoPanel.add(label1);

        //
        //
        //
        //
        //
        //
        //END OF demo panels
        // Creating the "Log New Device" tab
        JPanel logNewDevice = new JPanel();
        logNewDevice.setLayout(new GridLayout(12, 2, 10, 10)); // GridLayout for a cleaner form layout

        // Create JTextField for each field
        JTextField deviceNameField = new JTextField(15);
        JTextField deviceTypeField = new JTextField(15);
        JTextField brandField = new JTextField(15);
        JTextField modelField = new JTextField(15);
        JTextField serialNumberField = new JTextField(15);
        JTextField buildingLocationField = new JTextField(15);
        JTextField roomDeskField = new JTextField(15);
        JTextField specificationField = new JTextField(15);
        JTextField processorTypeField = new JTextField(15);
        JTextField storageCapacityField = new JTextField(15);
        JTextField networkAddressField = new JTextField(15);
        JTextField osVersionField = new JTextField(15);
        JTextField departmentField = new JTextField(15);
        JTextField addedMemoryField = new JTextField(15);
        JTextField addedStorageField = new JTextField(15);
        JTextField statusField = new JTextField(15);
        JTextField assignedUserField = new JTextField(15);

        // Create JSpinner for date selection
        JSpinner warrantyExpiryDatePicker = new JSpinner(new SpinnerDateModel());
        JSpinner maintenanceDatesPicker = new JSpinner(new SpinnerDateModel());

        // Set the date format to display only the date (MM/DD/YYYY)
        JSpinner.DateEditor warrantyExpiryDateEditor = new JSpinner.DateEditor(warrantyExpiryDatePicker, "MM/dd/yyyy");
        warrantyExpiryDatePicker.setEditor(warrantyExpiryDateEditor);

        JSpinner.DateEditor maintenanceDatesEditor = new JSpinner.DateEditor(maintenanceDatesPicker, "MM/dd/yyyy");
        maintenanceDatesPicker.setEditor(maintenanceDatesEditor);

        // Create the Purchase Cost field with a dollar sign
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        NumberFormatter currencyFormatter = new NumberFormatter(currencyFormat);
        currencyFormatter.setAllowsInvalid(false);
        JFormattedTextField purchaseCostField = new JFormattedTextField(currencyFormatter);
        purchaseCostField.setColumns(15);

        // Vendor field
        JTextField vendorField = new JTextField(15);

        // Add labels and text fields to the panel
        logNewDevice.add(new JLabel("Device Name (Primary Key):"));
        logNewDevice.add(deviceNameField);

        logNewDevice.add(new JLabel("Device Type:"));
        logNewDevice.add(deviceTypeField);

        logNewDevice.add(new JLabel("Brand:"));
        logNewDevice.add(brandField);

        logNewDevice.add(new JLabel("Model:"));
        logNewDevice.add(modelField);

        logNewDevice.add(new JLabel("Serial Number:"));
        logNewDevice.add(serialNumberField);

        logNewDevice.add(new JLabel("Building Location:"));
        logNewDevice.add(buildingLocationField);

        logNewDevice.add(new JLabel("Room/Desk:"));
        logNewDevice.add(roomDeskField);

        logNewDevice.add(new JLabel("Specification:"));
        logNewDevice.add(specificationField);

        logNewDevice.add(new JLabel("Processor Type:"));
        logNewDevice.add(processorTypeField);

        logNewDevice.add(new JLabel("Storage Capacity:"));
        logNewDevice.add(storageCapacityField);

        logNewDevice.add(new JLabel("Network Address:"));
        logNewDevice.add(networkAddressField);

        logNewDevice.add(new JLabel("OS Version:"));
        logNewDevice.add(osVersionField);

        logNewDevice.add(new JLabel("Department:"));
        logNewDevice.add(departmentField);

        logNewDevice.add(new JLabel("Added Memory (Boolean):"));
        logNewDevice.add(addedMemoryField);

        logNewDevice.add(new JLabel("Added Storage (Boolean):"));
        logNewDevice.add(addedStorageField);

        logNewDevice.add(new JLabel("Status:"));
        logNewDevice.add(statusField);

        logNewDevice.add(new JLabel("Assigned User:"));
        logNewDevice.add(assignedUserField);

        logNewDevice.add(new JLabel("Warranty Expiry Date:"));
        logNewDevice.add(warrantyExpiryDatePicker);

        logNewDevice.add(new JLabel("Maintenance Dates:"));
        logNewDevice.add(maintenanceDatesPicker);

        logNewDevice.add(new JLabel("Purchase Cost:"));
        logNewDevice.add(purchaseCostField);

        logNewDevice.add(new JLabel("Vendor:"));
        logNewDevice.add(vendorField);

        // Create the Enter button for the Log New Device tab
        JButton enterButton1 = new JButton("Enter");

        // Action listener for the Enter button
        enterButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean isValid = true;

                // Validate all fields including dates
                if (!validateDate(warrantyExpiryDatePicker)) {
                    isValid = false;
                    System.out.println("Invalid Warranty Expiry Date");
                }

                if (!validateDate(maintenanceDatesPicker)) {
                    isValid = false;
                    System.out.println("Invalid Maintenance Date");
                }

                // If all fields are valid, proceed with printing device details
                if (isValid) {
                    printDeviceDetails("Device Name", deviceNameField);
                    printDeviceDetails("Device Type", deviceTypeField);
                    printDeviceDetails("Brand", brandField);
                    printDeviceDetails("Model", modelField);
                    printDeviceDetails("Serial Number", serialNumberField);
                    printDeviceDetails("Building Location", buildingLocationField);
                    printDeviceDetails("Room/Desk", roomDeskField);
                    printDeviceDetails("Specification", specificationField);
                    printDeviceDetails("Processor Type", processorTypeField);
                    printDeviceDetails("Storage Capacity", storageCapacityField);
                    printDeviceDetails("Network Address", networkAddressField);
                    printDeviceDetails("OS Version", osVersionField);
                    printDeviceDetails("Department", departmentField);
                    printDeviceDetails("Added Memory", addedMemoryField);
                    printDeviceDetails("Added Storage", addedStorageField);
                    printDeviceDetails("Status", statusField);
                    printDeviceDetails("Assigned User", assignedUserField);
                    printDeviceDetails("Warranty Expiry Date", warrantyExpiryDatePicker);
                    printDeviceDetails("Maintenance Dates", maintenanceDatesPicker);
                    printDeviceDetails("Purchase Cost", purchaseCostField);
                    printDeviceDetails("Vendor", vendorField);
                }
            }
        });

        // Add Enter button to the panel
        logNewDevice.add(enterButton1);

        // Add tabs to the tabbed pane
        tabbedPane.addTab("Buttons", buttonPanel);
        tabbedPane.addTab("Text Box", textPanel);
        tabbedPane.addTab("Demo Tab", demoPanel);
        tabbedPane.addTab("Log New Device", logNewDevice);

        // Add the tabbed pane to the frame
        frame.add(tabbedPane);

        // Set default close operation and display the frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Listen for window closing event and ensure proper shutdown
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                System.exit(0); // Ensures JVM exits when window is closed
            }
        });
    }

    // Method to validate the date format and highlight the fields
    private static boolean validateDate(JSpinner dateSpinner) {
        // Get the date value from the JSpinner
        Object value = dateSpinner.getValue();

        // Check if the value is a valid date object
        if (value instanceof Date) {
            // Correct date format - reset the background color
            ((JComponent) dateSpinner.getEditor()).setBackground(Color.WHITE);
            return true;
        } else {
            // Invalid date, highlight the field in red
            ((JComponent) dateSpinner.getEditor()).setBackground(Color.RED);
            return false;
        }
    }

    // Helper method to print the device details with a label and value
    private static void printDeviceDetails(String label, JComponent component) {
        String value = "";
        if (component instanceof JTextField) {
            value = ((JTextField) component).getText();
        } else if (component instanceof JSpinner) {
            value = ((JSpinner) component).getValue().toString();
        } else if (component instanceof JFormattedTextField) {
            value = ((JFormattedTextField) component).getText();
        }
        System.out.println(label + ": " + (value.isEmpty() ? "null" : value));
    }
}
