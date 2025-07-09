import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import utils.InventoryData;
import utils.PeripheralUtils;
import utils.UIComponentUtils;

public class LogAccessoriesTab extends JPanel {
    private JLabel statusLabel;
    private JComboBox<String> accessoryTypeCombo;
    private JTextField countField;
    private JTextField newTypeField;
    private JPanel newTypePanel;

    public LogAccessoriesTab() {
        setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        ArrayList<String> existingTypes = PeripheralUtils.getPeripheralTypes(InventoryData.getAccessories());
        String[] initialItems = new String[existingTypes.size() + 1];
        for (int i = 0; i < existingTypes.size(); i++) {
            initialItems[i] = existingTypes.get(i);
        }
        initialItems[existingTypes.size()] = "Add New Accessory Type";
        accessoryTypeCombo = UIComponentUtils.createFormattedComboBox(initialItems);
        accessoryTypeCombo.setEditable(false);

        newTypePanel = new JPanel();
        newTypePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        newTypeField = UIComponentUtils.createFormattedTextField();
        newTypeField.setVisible(false);
        newTypePanel.setVisible(false);
        JButton addNewTypeButton = UIComponentUtils.createFormattedButton("Add New Type");
        newTypePanel.add(UIComponentUtils.createAlignedLabel("New Accessory Type:"));
        newTypePanel.add(newTypeField);
        newTypePanel.add(addNewTypeButton);
        newTypePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        countField = UIComponentUtils.createFormattedTextField();
        countField.setText("1");
        JButton addButton = UIComponentUtils.createFormattedButton("Add Accessory");
        JButton removeButton = UIComponentUtils.createFormattedButton("Remove Accessory");
        statusLabel = UIComponentUtils.createAlignedLabel("");

        accessoryTypeCombo.addActionListener(e -> {
            Object selectedItem = accessoryTypeCombo.getSelectedItem();
            if (selectedItem != null && selectedItem.equals("Add New Accessory Type")) {
                newTypeField.setVisible(true);
                newTypePanel.setVisible(true);
            } else {
                newTypeField.setVisible(false);
                newTypePanel.setVisible(false);
            }
            panel.revalidate();
            panel.repaint();
        });

        addNewTypeButton.addActionListener(e -> PeripheralUtils.addNewPeripheralType(
            newTypeField, accessoryTypeCombo, statusLabel, InventoryData.getAccessories(), existingTypes));

        addButton.addActionListener(e -> {
            Object selectedItem = accessoryTypeCombo.getSelectedItem();
            if (selectedItem == null || selectedItem.equals("Add New Accessory Type")) {
                statusLabel.setText("Error: Select or add an accessory type first");
                return;
            }
            String type = selectedItem.toString();
            int count;
            try {
                count = Integer.parseInt(countField.getText().trim());
                if (count <= 0) {
                    statusLabel.setText("Error: Count must be positive");
                    return;
                }
            } catch (NumberFormatException ex) {
                statusLabel.setText("Error: Invalid count");
                return;
            }
            PeripheralUtils.updatePeripheralCount(type, count, InventoryData.getAccessories(), statusLabel);
        });

        removeButton.addActionListener(e -> {
            Object selectedItem = accessoryTypeCombo.getSelectedItem();
            if (selectedItem == null || selectedItem.equals("Add New Accessory Type")) {
                statusLabel.setText("Error: Select or add an accessory type first");
                return;
            }
            String type = selectedItem.toString();
            int count;
            try {
                count = Integer.parseInt(countField.getText().trim());
                if (count <= 0) {
                    statusLabel.setText("Error: Count must be positive");
                    return;
                }
            } catch (NumberFormatException ex) {
                statusLabel.setText("Error: Invalid count");
                return;
            }
            PeripheralUtils.updatePeripheralCount(type, -count, InventoryData.getAccessories(), statusLabel);
        });

        panel.add(UIComponentUtils.createAlignedLabel("Accessory Type:"));
        panel.add(accessoryTypeCombo);
        panel.add(newTypePanel);
        panel.add(UIComponentUtils.createAlignedLabel("Count:"));
        panel.add(countField);
        panel.add(addButton);
        panel.add(removeButton);

        JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(panel);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }
}