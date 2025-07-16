import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import utils.InventoryData;
import utils.PeripheralUtils;
import utils.UIComponentUtils;

public class LogAccessoriesTab extends JPanel {
    private JComboBox<String> accessoryTypeCombo;
    private JTextField countField;
    private JTextField newTypeField;
    private JPanel newTypePanel;
    private final JLabel statusLabel;

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
        newTypeField = UIComponentUtils.createFormattedTextField();
        newTypeField.setVisible(false);
        JButton addNewTypeButton = UIComponentUtils.createFormattedButton("Add New Type");
        newTypePanel.add(UIComponentUtils.createAlignedLabel("New Accessory Type:"));
        newTypePanel.add(newTypeField);
        newTypePanel.add(addNewTypeButton);

        countField = UIComponentUtils.createFormattedTextField();
        JButton addButton = UIComponentUtils.createFormattedButton("Add");
        JButton removeButton = UIComponentUtils.createFormattedButton("Remove");
        statusLabel = UIComponentUtils.createAlignedLabel("");

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

        addNewTypeButton.addActionListener(e -> {
            newTypeField.setVisible(true);
            newTypePanel.revalidate();
            PeripheralUtils.addNewPeripheralType(newTypeField, accessoryTypeCombo, statusLabel, InventoryData.getAccessories(), existingTypes);
        });

        accessoryTypeCombo.addActionListener(e -> {
            Object selectedItem = accessoryTypeCombo.getSelectedItem();
            if (selectedItem != null && selectedItem.equals("Add New Accessory Type")) {
                newTypeField.setVisible(true);
            } else {
                newTypeField.setVisible(false);
            }
            newTypePanel.revalidate();
            statusLabel.setText("");
        });

        panel.add(UIComponentUtils.createAlignedLabel("Accessory Type:"));
        panel.add(accessoryTypeCombo);
        panel.add(newTypePanel);
        panel.add(UIComponentUtils.createAlignedLabel("Count:"));
        panel.add(countField);
        panel.add(addButton);
        panel.add(removeButton);
        panel.add(statusLabel);

        JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(panel);
        add(scrollPane, BorderLayout.CENTER);
    }
}