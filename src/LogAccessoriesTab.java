import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class LogAccessoriesTab extends JPanel {

    public LogAccessoriesTab() {
        setLayout(new BorderLayout(10, 10)); // Use BorderLayout for better control

        // Create the dropdown for accessory selection
        JComboBox<String> accessoryCombo = new JComboBox<>(new String[]{"Keyboard", "Mouse", "Monitor"});
        JPanel topPanel = new JPanel(); // Create a top panel to hold the dropdown and label
        topPanel.add(new JLabel("Select Accessory:"));
        topPanel.add(accessoryCombo);

        // Create the panels for each accessory type
        JPanel keyboardPanel = createKeyboardPanel();
        JPanel mousePanel = createMousePanel();
        JPanel monitorPanel = createMonitorPanel();

        // Add the topPanel first (select accessory dropdown)
        add(topPanel, BorderLayout.NORTH);

        // Create a scrollable panel for the content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS)); // Stack vertically
        JScrollPane scrollPane = new JScrollPane(contentPanel); // Make the content scrollable
        add(scrollPane, BorderLayout.CENTER);

        // Action listener for the accessory combo box
        accessoryCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selected = (String) accessoryCombo.getSelectedItem();

                // Remove all components from the contentPanel
                contentPanel.removeAll();

                // Add the selected accessory panel
                if ("Keyboard".equals(selected)) {
                    contentPanel.add(keyboardPanel);
                } else if ("Mouse".equals(selected)) {
                    contentPanel.add(mousePanel);
                } else if ("Monitor".equals(selected)) {
                    contentPanel.add(monitorPanel);
                }

                // Revalidate and repaint the content to update the view
                contentPanel.revalidate();
                contentPanel.repaint();
            }
        });

        // Set initial selection (defaults to Keyboard)
        accessoryCombo.setSelectedItem("Keyboard");
    }

    // Method to create the keyboard panel
    private JPanel createKeyboardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Stack vertically

        JTextField brandField = new JTextField();
        brandField.setPreferredSize(new Dimension(150, 30)); // Set size for brand field (fixed width)
        brandField.setMaximumSize(new Dimension(150, 30)); // Ensure it doesn't stretch
        JButton addToStorageButton = new JButton("Add to Storage");
        JButton deployedButton = new JButton("Deployed");

        // Add action listeners to print to terminal when buttons are clicked
        addToStorageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Keyboard Added to Storage - Brand: " + (brandField.getText().isEmpty() ? "null" : brandField.getText()));
            }
        });

        deployedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Keyboard Deployed - Brand: " + (brandField.getText().isEmpty() ? "null" : brandField.getText()));
            }
        });

        panel.add(new JLabel("Brand:"));
        panel.add(brandField);
        panel.add(addToStorageButton);
        panel.add(deployedButton);

        return panel;
    }

    // Method to create the mouse panel
    private JPanel createMousePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Stack vertically

        JTextField mouseBrandField = new JTextField();
        mouseBrandField.setPreferredSize(new Dimension(150, 30)); // Set size for mouse brand field (fixed width)
        mouseBrandField.setMaximumSize(new Dimension(150, 30)); // Ensure it doesn't stretch
        JButton mouseAddToStorageButton = new JButton("Add to Storage");
        JButton mouseDeployedButton = new JButton("Deployed");

        // Add action listeners to print to terminal when buttons are clicked
        mouseAddToStorageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Mouse Added to Storage - Brand: " + (mouseBrandField.getText().isEmpty() ? "null" : mouseBrandField.getText()));
            }
        });

        mouseDeployedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Mouse Deployed - Brand: " + (mouseBrandField.getText().isEmpty() ? "null" : mouseBrandField.getText()));
            }
        });

        panel.add(new JLabel("Brand:"));
        panel.add(mouseBrandField);
        panel.add(mouseAddToStorageButton);
        panel.add(mouseDeployedButton);

        return panel;
    }

    // Method to create the monitor panel
    private JPanel createMonitorPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Stack components vertically

        JTextField monitorBrandField = new JTextField();
        monitorBrandField.setPreferredSize(new Dimension(150, 30)); // Set size for monitor brand field (fixed width)
        monitorBrandField.setMaximumSize(new Dimension(150, 30)); // Ensure it doesn't stretch
        
        JTextField screenSizeField = new JTextField();
        screenSizeField.setPreferredSize(new Dimension(50, 30)); // Narrow width for screen size
        screenSizeField.setMaximumSize(new Dimension(50, 30)); // Ensure it doesn't stretch

        JComboBox<String> resolutionCombo = new JComboBox<>(new String[]{"Widescreen", "Full Screen"});
        resolutionCombo.setPreferredSize(new Dimension(150, 30)); // Set size for resolution combo box
        resolutionCombo.setMaximumSize(new Dimension(150, 30)); // Ensure it doesn't stretch
        
        JCheckBox vgaCheckBox = new JCheckBox("VGA");
        JCheckBox hdmiCheckBox = new JCheckBox("HDMI");
        JCheckBox displayPortCheckBox = new JCheckBox("DisplayPort");

        JButton addToStorageButton = new JButton("Add to Storage");
        JButton deployedButton = new JButton("Deployed");

        // Action listeners to print to terminal when buttons are clicked
        addToStorageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Monitor Added to Storage - Brand: " + (monitorBrandField.getText().isEmpty() ? "null" : monitorBrandField.getText()) +
                                   ", Screen Size: " + (screenSizeField.getText().isEmpty() ? "null" : screenSizeField.getText()) +
                                   ", Resolution: " + (resolutionCombo.getSelectedItem() == null ? "null" : resolutionCombo.getSelectedItem()));
            }
        });

        deployedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Monitor Deployed - Brand: " + (monitorBrandField.getText().isEmpty() ? "null" : monitorBrandField.getText()) +
                                   ", Screen Size: " + (screenSizeField.getText().isEmpty() ? "null" : screenSizeField.getText()) +
                                   ", Resolution: " + (resolutionCombo.getSelectedItem() == null ? "null" : resolutionCombo.getSelectedItem()));
            }
        });

        panel.add(new JLabel("Brand:"));
        panel.add(monitorBrandField);
        panel.add(new JLabel("Screen Size (inches):"));
        panel.add(screenSizeField);
        panel.add(new JLabel("Resolution:"));
        panel.add(resolutionCombo);
        panel.add(vgaCheckBox);
        panel.add(hdmiCheckBox);
        panel.add(displayPortCheckBox);
        panel.add(addToStorageButton);
        panel.add(deployedButton);

        return panel;
    }
}
