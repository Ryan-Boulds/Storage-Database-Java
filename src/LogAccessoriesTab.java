import java.awt.*;
import javax.swing.*;

public class LogAccessoriesTab extends JPanel {

    public LogAccessoriesTab() {
        setLayout(new BorderLayout(10, 10));

        // Create the dropdown for accessory selection
        JComboBox<String> accessoryCombo = UIUtils.createFormattedComboBox(
            new String[]{"Keyboard", "Mouse", "Monitor"}
        );
        JPanel topPanel = new JPanel();
        topPanel.add(UIUtils.createAlignedLabel("Select Accessory:"));
        topPanel.add(accessoryCombo);

        // Create the panels for each accessory type
        JPanel keyboardPanel = createKeyboardPanel();
        JPanel mousePanel = createMousePanel();
        JPanel monitorPanel = createMonitorPanel();

        // Create a scrollable content panel
        JPanel contentPanel = new JPanel();
        JScrollPane scrollPane = UIUtils.createScrollableContentPanel(contentPanel);
        contentPanel.add(keyboardPanel); // Default to keyboard panel
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Action listener for the accessory combo box
        accessoryCombo.addActionListener(e -> {
            String selected = (String) accessoryCombo.getSelectedItem();
            contentPanel.removeAll();
            switch (selected) {
                case "Keyboard":
                    contentPanel.add(keyboardPanel);
                    break;
                case "Mouse":
                    contentPanel.add(mousePanel);
                    break;
                case "Monitor":
                    contentPanel.add(monitorPanel);
                    break;
            }
            contentPanel.revalidate();
            contentPanel.repaint();
        });

        // Set initial selection
        accessoryCombo.setSelectedItem("Keyboard");
    }

    private JPanel createKeyboardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField brandField = UIUtils.createFormattedTextField();
        JButton addToStorageButton = UIUtils.createFormattedButton("Add to Storage");
        JButton deployedButton = UIUtils.createFormattedButton("Deployed");

        addToStorageButton.addActionListener(e -> {
            System.out.println("Keyboard Added to Storage - Brand: " +
                (brandField.getText().isEmpty() ? "null" : brandField.getText()));
        });

        deployedButton.addActionListener(e -> {
            System.out.println("Keyboard Deployed - Brand: " +
                (brandField.getText().isEmpty() ? "null" : brandField.getText()));
        });

        panel.add(UIUtils.createAlignedLabel("Brand:"));
        panel.add(brandField);
        panel.add(addToStorageButton);
        panel.add(deployedButton);

        return panel;
    }

    private JPanel createMousePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField mouseBrandField = UIUtils.createFormattedTextField();
        JButton mouseAddToStorageButton = UIUtils.createFormattedButton("Add to Storage");
        JButton mouseDeployedButton = UIUtils.createFormattedButton("Deployed");

        mouseAddToStorageButton.addActionListener(e -> {
            System.out.println("Mouse Added to Storage - Brand: " +
                (mouseBrandField.getText().isEmpty() ? "null" : mouseBrandField.getText()));
        });

        mouseDeployedButton.addActionListener(e -> {
            System.out.println("Mouse Deployed - Brand: " +
                (mouseBrandField.getText().isEmpty() ? "null" : mouseBrandField.getText()));
        });

        panel.add(UIUtils.createAlignedLabel("Brand:"));
        panel.add(mouseBrandField);
        panel.add(mouseAddToStorageButton);
        panel.add(mouseDeployedButton);

        return panel;
    }

    private JPanel createMonitorPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField monitorBrandField = UIUtils.createFormattedTextField();
        JTextField screenSizeField = UIUtils.createFormattedTextField();
        JComboBox<String> resolutionCombo = UIUtils.createFormattedComboBox(
            new String[]{"Widescreen", "Full Screen"}
        );
        JCheckBox vgaCheckBox = UIUtils.createFormattedCheckBox("VGA");
        JCheckBox hdmiCheckBox = UIUtils.createFormattedCheckBox("HDMI");
        JCheckBox displayPortCheckBox = UIUtils.createFormattedCheckBox("DisplayPort");
        JButton addToStorageButton = UIUtils.createFormattedButton("Add to Storage");
        JButton deployedButton = UIUtils.createFormattedButton("Deployed");

        addToStorageButton.addActionListener(e -> {
            System.out.println("Monitor Added to Storage - Brand: " +
                (monitorBrandField.getText().isEmpty() ? "null" : monitorBrandField.getText()) +
                ", Screen Size: " + (screenSizeField.getText().isEmpty() ? "null" : screenSizeField.getText()) +
                ", Resolution: " + resolutionCombo.getSelectedItem() +
                ", Ports: " + UIUtils.getSelectedPorts(vgaCheckBox, hdmiCheckBox, displayPortCheckBox));
        });

        deployedButton.addActionListener(e -> {
            System.out.println("Monitor Deployed - Brand: " +
                (monitorBrandField.getText().isEmpty() ? "null" : monitorBrandField.getText()) +
                ", Screen Size: " + (screenSizeField.getText().isEmpty() ? "null" : screenSizeField.getText()) +
                ", Resolution: " + resolutionCombo.getSelectedItem() +
                ", Ports: " + UIUtils.getSelectedPorts(vgaCheckBox, hdmiCheckBox, displayPortCheckBox));
        });

        panel.add(UIUtils.createAlignedLabel("Brand:"));
        panel.add(monitorBrandField);
        panel.add(UIUtils.createAlignedLabel("Screen Size (inches):"));
        panel.add(screenSizeField);
        panel.add(UIUtils.createAlignedLabel("Resolution:"));
        panel.add(resolutionCombo);
        panel.add(vgaCheckBox);
        panel.add(hdmiCheckBox);
        panel.add(displayPortCheckBox);
        panel.add(addToStorageButton);
        panel.add(deployedButton);

        return panel;
    }
}