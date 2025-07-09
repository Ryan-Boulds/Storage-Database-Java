import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import utils.InventoryData;
import utils.PeripheralUtils;
import utils.UIComponentUtils;

public class LogCablesTab extends JPanel {
    private JLabel statusLabel;
    private JComboBox<String> cableTypeCombo;
    private JTextField countField;
    private JTextField newTypeField;
    private JPanel newTypePanel;

    public LogCablesTab() {
        setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        ArrayList<String> existingTypes = PeripheralUtils.getPeripheralTypes(InventoryData.getCables());
        String[] initialItems = new String[existingTypes.size() + 1];
        for (int i = 0; i < existingTypes.size(); i++) {
            initialItems[i] = existingTypes.get(i);
        }
        initialItems[existingTypes.size()] = "Add New Cable Type";
        cableTypeCombo = UIComponentUtils.createFormattedComboBox(initialItems);
        cableTypeCombo.setEditable(false);

        newTypePanel = new JPanel();
        newTypePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        newTypeField = UIComponentUtils.createFormattedTextField();
        newTypeField.setVisible(false);
        newTypePanel.setVisible(false);
        JButton addNewTypeButton = UIComponentUtils.createFormattedButton("Add New Type");
        newTypePanel.add(UIComponentUtils.createAlignedLabel("New Cable Type:"));
        newTypePanel.add(newTypeField);
        newTypePanel.add(addNewTypeButton);
        newTypePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        countField = UIComponentUtils.createFormattedTextField();
        countField.setText("1");
        JButton addButton = UIComponentUtils.createFormattedButton("Add Cable");
        JButton removeButton = UIComponentUtils.createFormattedButton("Remove Cable");
        statusLabel = UIComponentUtils.createAlignedLabel("");

        cableTypeCombo.addActionListener(e -> {
            Object selectedItem = cableTypeCombo.getSelectedItem();
            if (selectedItem != null && selectedItem.equals("Add New Cable Type")) {
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
            newTypeField, cableTypeCombo, statusLabel, InventoryData.getCables(), existingTypes));

        addButton.addActionListener(e -> {
            Object selectedItem = cableTypeCombo.getSelectedItem();
            if (selectedItem == null || selectedItem.equals("Add New Cable Type")) {
                statusLabel.setText("Error: Select or add a cable type first");
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
            PeripheralUtils.updatePeripheralCount(type, count, InventoryData.getCables(), statusLabel);
        });

        removeButton.addActionListener(e -> {
            Object selectedItem = cableTypeCombo.getSelectedItem();
            if (selectedItem == null || selectedItem.equals("Add New Cable Type")) {
                statusLabel.setText("Error: Select or add a cable type first");
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
            PeripheralUtils.updatePeripheralCount(type, -count, InventoryData.getCables(), statusLabel);
        });

        panel.add(UIComponentUtils.createAlignedLabel("Cable Type:"));
        panel.add(cableTypeCombo);
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