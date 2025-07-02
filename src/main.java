import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import javax.swing.*;

public class main {

    public static void main(String[] args) {
        // Set up the frame
        JFrame frame = new JFrame("Tabbed Pane Example");
        frame.setSize(600, 600);

        // Create a tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Creating the "Log New Device" tab
        JPanel logNewDevice = new JPanel();
        logNewDevice.setLayout(new GridLayout(12, 2, 10, 10)); // GridLayout for a cleaner form layout

        // Create JTextField for fields that need text input
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
        JTextField statusField = new JTextField(15);
        JTextField assignedUserField = new JTextField(15);
        JTextField purchaseCostField = new JTextField(15);
        JTextField vendorField = new JTextField(15);

        // JComboBox for Boolean values (TRUE, FALSE, and null)
        JComboBox<String> addedMemoryCombo = new JComboBox<>(new String[]{"TRUE", "FALSE", "null"});
        JComboBox<String> addedStorageCombo = new JComboBox<>(new String[]{"TRUE", "FALSE", "null"});

        // Create JSpinner for date selection
        JSpinner warrantyExpiryDatePicker = new JSpinner(new SpinnerDateModel());
        JSpinner maintenanceDatesPicker = new JSpinner(new SpinnerDateModel());

        // Set the date format to display only the date (MM/DD/YYYY)
        JSpinner.DateEditor warrantyExpiryDateEditor = new JSpinner.DateEditor(warrantyExpiryDatePicker, "MM/dd/yyyy");
        warrantyExpiryDatePicker.setEditor(warrantyExpiryDateEditor);

        JSpinner.DateEditor maintenanceDatesEditor = new JSpinner.DateEditor(maintenanceDatesPicker, "MM/dd/yyyy");
        maintenanceDatesPicker.setEditor(maintenanceDatesEditor);

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

        logNewDevice.add(new JLabel("Added Memory:"));
        logNewDevice.add(addedMemoryCombo);

        logNewDevice.add(new JLabel("Added Storage:"));
        logNewDevice.add(addedStorageCombo);

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

                // Validate year format (4 characters for year)
                if (!validateYearFormat(warrantyExpiryDatePicker) || !validateYearFormat(maintenanceDatesPicker)) {
                    isValid = false;
                    System.out.println("Invalid Year Format (should be 4 digits)");
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
                    printDeviceDetails("Added Memory", addedMemoryCombo);
                    printDeviceDetails("Added Storage", addedStorageCombo);
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

        // "Log Accessories" tab
        JPanel logAccessoriesPanel = new JPanel();
        logAccessoriesPanel.setLayout(new FlowLayout());

        // Dropdown menu for category selection
        String[] categories = {"Select Category", "Keyboard", "Mouse", "Monitor"};
        JComboBox<String> categoryDropdown = new JComboBox<>(categories);
        logAccessoriesPanel.add(new JLabel("Select Category:"));
        logAccessoriesPanel.add(categoryDropdown);

        // Panel for the dynamic fields based on selection
        JPanel dynamicPanel = new JPanel();
        dynamicPanel.setLayout(new BoxLayout(dynamicPanel, BoxLayout.Y_AXIS));

        // TextField for Brand (visible for Keyboard, Mouse, and Monitor)
        JTextField brandFieldForAccessories = new JTextField(15);
        dynamicPanel.add(new JLabel("Brand:"));
        dynamicPanel.add(brandFieldForAccessories);

        // Monitor specific fields (screen size, input types)
        JPanel monitorPanel = new JPanel();
        monitorPanel.setLayout(new GridLayout(3, 2)); // GridLayout for monitor specifics

        JTextField screenSizeField = new JTextField(10);
        JCheckBox vgaCheckBox = new JCheckBox("VGA");
        JCheckBox hdmiCheckBox = new JCheckBox("HDMI");
        JCheckBox dpCheckBox = new JCheckBox("DisplayPort");

        monitorPanel.add(new JLabel("Screen Size (inches):"));
        monitorPanel.add(screenSizeField);
        monitorPanel.add(vgaCheckBox);
        monitorPanel.add(hdmiCheckBox);
        monitorPanel.add(dpCheckBox);

        // Buttons for adding to storage or deployment
        JButton addToStorageButton = new JButton("Add to Storage");
        JButton deployedButton = new JButton("Deployed");

        // Action listeners for the buttons
        addToStorageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String brand = brandFieldForAccessories.getText();
                if (categoryDropdown.getSelectedItem().equals("Monitor")) {
                    System.out.println("Monitor added to storage with brand: " + brand + ", Screen Size: " + screenSizeField.getText() + ", VGA: " + vgaCheckBox.isSelected() + ", HDMI: " + hdmiCheckBox.isSelected() + ", DisplayPort: " + dpCheckBox.isSelected());
                } else {
                    System.out.println(brand + " added to storage.");
                }
            }
        });

        deployedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String brand = brandFieldForAccessories.getText();
                if (categoryDropdown.getSelectedItem().equals("Monitor")) {
                    System.out.println("Monitor deployed with brand: " + brand + ", Screen Size: " + screenSizeField.getText() + ", VGA: " + vgaCheckBox.isSelected() + ", HDMI: " + hdmiCheckBox.isSelected() + ", DisplayPort: " + dpCheckBox.isSelected());
                } else {
                    System.out.println(brand + " deployed.");
                }
            }
        });

        // Add dynamic fields and buttons to the logAccessoriesPanel
        logAccessoriesPanel.add(dynamicPanel);
        logAccessoriesPanel.add(addToStorageButton);
        logAccessoriesPanel.add(deployedButton);

        // Listen for category selection change to update UI
        categoryDropdown.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                String selectedCategory = (String) categoryDropdown.getSelectedItem();

                // Reset dynamic panel
                dynamicPanel.removeAll();
                dynamicPanel.add(new JLabel("Brand:"));
                dynamicPanel.add(brandFieldForAccessories);

                if (selectedCategory.equals("Monitor")) {
                    dynamicPanel.add(monitorPanel);  // Add monitor-specific fields
                }

                // Revalidate and repaint the panel to show changes
                logAccessoriesPanel.revalidate();
                logAccessoriesPanel.repaint();
            }
        });

        // Add the "Log Accessories" tab to the tabbed pane
        tabbedPane.addTab("Log New Device", logNewDevice);
        tabbedPane.addTab("Log Accessories", logAccessoriesPanel);

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

    // Method to validate the year format
    private static boolean validateYearFormat(JSpinner dateSpinner) {
        Object value = dateSpinner.getValue();
        String dateString = ((JSpinner.DateEditor) dateSpinner.getEditor()).getFormat().format(value);
        String year = dateString.substring(6, 10);  // Extract the year (MM/DD/YYYY format)

        // Check if year is exactly 4 digits
        if (year.length() != 4) {
            ((JComponent) dateSpinner.getEditor()).setBackground(Color.RED);
            return false;
        } else {
            ((JComponent) dateSpinner.getEditor()).setBackground(Color.WHITE);
            return true;
        }
    }

    // Helper method to print the device details with a label and value
    private static void printDeviceDetails(String label, JComponent component) {
        String value = "";

        // Check if the component is a JTextField
        if (component instanceof JTextField) {
            value = ((JTextField) component).getText();
        } // Check if the component is a JComboBox (for Boolean fields)
        else if (component instanceof JComboBox) {
            value = (String) ((JComboBox<?>) component).getSelectedItem();
        } // Check if the component is a JSpinner (for date fields)
        else if (component instanceof JSpinner) {
            value = ((JSpinner) component).getValue().toString();
        } // Check if the component is a JFormattedTextField (e.g., for currency)
        else if (component instanceof JFormattedTextField) {
            value = ((JFormattedTextField) component).getText();
        }

        // If the value is empty or selected as "null" in the combo box, print "null"
        if (value.isEmpty() || value.equals("null")) {
            System.out.println(label + ": null");
        } else {
            // Otherwise, print the actual value entered
            System.out.println(label + ": " + value);
        }
    }
}
