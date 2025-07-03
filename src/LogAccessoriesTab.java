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

private JPanel createKeyboardPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Stack vertically

    JTextField brandField = new JTextField();
    brandField.setPreferredSize(new Dimension(450, 30));
    brandField.setMaximumSize(new Dimension(450, 30));
    brandField.setAlignmentX(Component.LEFT_ALIGNMENT);

    JButton addToStorageButton = new JButton("Add to Storage");
    addToStorageButton.setAlignmentX(Component.LEFT_ALIGNMENT);

    JButton deployedButton = new JButton("Deployed");
    deployedButton.setAlignmentX(Component.LEFT_ALIGNMENT);

    // Action listeners
    addToStorageButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Keyboard Added to Storage - Brand: " +
                (brandField.getText().isEmpty() ? "null" : brandField.getText()));
        }
    });

    deployedButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Keyboard Deployed - Brand: " +
                (brandField.getText().isEmpty() ? "null" : brandField.getText()));
        }
    });

    panel.add(createAlignedLabel("Brand:"));
    panel.add(brandField);
    panel.add(addToStorageButton);
    panel.add(deployedButton);

    return panel;
}


private JPanel createMousePanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Stack vertically

    JTextField mouseBrandField = new JTextField();
    mouseBrandField.setPreferredSize(new Dimension(450, 30));
    mouseBrandField.setMaximumSize(new Dimension(450, 30));
    mouseBrandField.setAlignmentX(Component.LEFT_ALIGNMENT);

    JButton mouseAddToStorageButton = new JButton("Add to Storage");
    mouseAddToStorageButton.setAlignmentX(Component.LEFT_ALIGNMENT);

    JButton mouseDeployedButton = new JButton("Deployed");
    mouseDeployedButton.setAlignmentX(Component.LEFT_ALIGNMENT);

    // Add action listeners
    mouseAddToStorageButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Mouse Added to Storage - Brand: " +
                (mouseBrandField.getText().isEmpty() ? "null" : mouseBrandField.getText()));
        }
    });

    mouseDeployedButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Mouse Deployed - Brand: " +
                (mouseBrandField.getText().isEmpty() ? "null" : mouseBrandField.getText()));
        }
    });

    panel.add(createAlignedLabel("Brand:"));
    panel.add(mouseBrandField);
    panel.add(mouseAddToStorageButton);
    panel.add(mouseDeployedButton);

    return panel;
}


private JPanel createMonitorPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Vertical stack

    JTextField monitorBrandField = new JTextField();
    monitorBrandField.setPreferredSize(new Dimension(450, 30));
    monitorBrandField.setMaximumSize(new Dimension(450, 30));
    monitorBrandField.setAlignmentX(Component.LEFT_ALIGNMENT);

    JTextField screenSizeField = new JTextField();
    screenSizeField.setPreferredSize(new Dimension(450, 30));
    screenSizeField.setMaximumSize(new Dimension(450, 30));
    screenSizeField.setAlignmentX(Component.LEFT_ALIGNMENT);

    JComboBox<String> resolutionCombo = new JComboBox<>(new String[]{"Widescreen", "Full Screen"});
    resolutionCombo.setPreferredSize(new Dimension(450, 30));
    resolutionCombo.setMaximumSize(new Dimension(450, 30));
    resolutionCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

    JCheckBox vgaCheckBox = new JCheckBox("VGA");
    vgaCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    JCheckBox hdmiCheckBox = new JCheckBox("HDMI");
    hdmiCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    JCheckBox displayPortCheckBox = new JCheckBox("DisplayPort");
    displayPortCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

    JButton addToStorageButton = new JButton("Add to Storage");
    addToStorageButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    JButton deployedButton = new JButton("Deployed");
    deployedButton.setAlignmentX(Component.LEFT_ALIGNMENT);

    // ✅ Add action listeners
    addToStorageButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Monitor Added to Storage - Brand: " +
                (monitorBrandField.getText().isEmpty() ? "null" : monitorBrandField.getText()) +
                ", Screen Size: " + (screenSizeField.getText().isEmpty() ? "null" : screenSizeField.getText()) +
                ", Resolution: " + resolutionCombo.getSelectedItem() +
                ", Ports: " + getSelectedPorts(vgaCheckBox, hdmiCheckBox, displayPortCheckBox));
        }
    });

    deployedButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Monitor Deployed - Brand: " +
                (monitorBrandField.getText().isEmpty() ? "null" : monitorBrandField.getText()) +
                ", Screen Size: " + (screenSizeField.getText().isEmpty() ? "null" : screenSizeField.getText()) +
                ", Resolution: " + resolutionCombo.getSelectedItem() +
                ", Ports: " + getSelectedPorts(vgaCheckBox, hdmiCheckBox, displayPortCheckBox));
        }
    });

    panel.add(createAlignedLabel("Brand:"));
    panel.add(monitorBrandField);
    panel.add(createAlignedLabel("Screen Size (inches):"));
    panel.add(screenSizeField);
    panel.add(createAlignedLabel("Resolution:"));
    panel.add(resolutionCombo);
    panel.add(vgaCheckBox);
    panel.add(hdmiCheckBox);
    panel.add(displayPortCheckBox);
    panel.add(addToStorageButton);
    panel.add(deployedButton);

    return panel;
}


// Helper to get selected ports as comma-separated string
private String getSelectedPorts(JCheckBox... ports) {
    StringBuilder sb = new StringBuilder();
    for (JCheckBox port : ports) {
        if (port.isSelected()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(port.getText());
        }
    }
    return sb.length() > 0 ? sb.toString() : "None";
}

// Helper to align JLabels to the left
private JLabel createAlignedLabel(String text) {
    JLabel label = new JLabel(text);
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    return label;
}

}
