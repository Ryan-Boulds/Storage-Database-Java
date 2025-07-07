import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;

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

        ArrayList<String> existingTypes = UIUtils.getPeripheralTypes(UIUtils.getCables());
        String[] initialItems;
        if (existingTypes.isEmpty()) {
            initialItems = new String[]{"Add New Cable Type"};
        } else {
            initialItems = new String[existingTypes.size() + 1];
            for (int i = 0; i < existingTypes.size(); i++) {
                initialItems[i] = existingTypes.get(i);
            }
            initialItems[existingTypes.size()] = "Add New Cable Type";
        }
        cableTypeCombo = new JComboBox<>(initialItems);
        cableTypeCombo.setPreferredSize(new Dimension(450, 30));
        cableTypeCombo.setMaximumSize(new Dimension(450, 30));
        cableTypeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Set no initial selection if there are existing types
        if (!existingTypes.isEmpty()) {
            cableTypeCombo.setSelectedIndex(-1);
        }

        newTypePanel = new JPanel();
        newTypePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        newTypeField = UIUtils.createFormattedTextField();
        newTypeField.setVisible(false);
        newTypePanel.setVisible(false); // Ensure panel is hidden initially
        JButton addNewTypeButton = UIUtils.createFormattedButton("Add New Type");
        newTypePanel.add(UIUtils.createAlignedLabel("New Cable Type:"));
        newTypePanel.add(newTypeField);
        newTypePanel.add(addNewTypeButton);
        newTypePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        countField = UIUtils.createFormattedTextField();
        countField.setText("1"); // Default to 1
        JButton addButton = UIUtils.createFormattedButton("Add Cable");
        JButton removeButton = UIUtils.createFormattedButton("Remove Cable");
        statusLabel = UIUtils.createAlignedLabel("");

        // Temporarily disable ActionListener during initialization
        ActionListener[] listeners = cableTypeCombo.getActionListeners();
        for (ActionListener listener : listeners) {
            cableTypeCombo.removeActionListener(listener);
        }

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

        // Re-add listeners after setting initial state
        for (ActionListener listener : listeners) {
            cableTypeCombo.addActionListener(listener);
        }

        addNewTypeButton.addActionListener(e -> {
            String newType = newTypeField.getText().trim();
            if (newType.isEmpty()) {
                statusLabel.setText("Error: Enter a new cable type");
                return;
            }
            newType = UIUtils.capitalizeWords(newType);
            HashMap<String, String> newCable = new HashMap<>();
            newCable.put("Peripheral_Type", newType);
            newCable.put("Count", "0");
            UIUtils.getCables().add(newCable);
            UIUtils.saveCables();
            cableTypeCombo.removeItem("Add New Cable Type");
            cableTypeCombo.addItem(newType);
            cableTypeCombo.addItem("Add New Cable Type");
            cableTypeCombo.setSelectedItem(newType); // Safely set the new type
            newTypeField.setText("");
            newTypeField.setVisible(false);
            newTypePanel.setVisible(false); // Hide panel after adding
            statusLabel.setText(newType + " added with count 0");
            panel.revalidate();
            panel.repaint();
        });

        addButton.addActionListener(e -> {
            Object selectedItem = cableTypeCombo.getSelectedItem();
            if (selectedItem == null || selectedItem.equals("Add New Cable Type")) {
                statusLabel.setText("Error: Select or add a cable type first");
                return;
            }
            String type = selectedItem.toString();
            int count = 1;
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
            UIUtils.updatePeripheralCount(type, count, UIUtils.getCables(), statusLabel);
        });

        removeButton.addActionListener(e -> {
            Object selectedItem = cableTypeCombo.getSelectedItem();
            if (selectedItem == null || selectedItem.equals("Add New Cable Type")) {
                statusLabel.setText("Error: Select or add a cable type first");
                return;
            }
            String type = selectedItem.toString();
            int count = 1;
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
            UIUtils.updatePeripheralCount(type, -count, UIUtils.getCables(), statusLabel);
        });

        panel.add(UIUtils.createAlignedLabel("Cable Type:"));
        panel.add(cableTypeCombo);
        panel.add(newTypePanel);
        panel.add(UIUtils.createAlignedLabel("Count:"));
        panel.add(countField);
        panel.add(addButton);
        panel.add(removeButton);

        JScrollPane scrollPane = UIUtils.createScrollableContentPanel(panel);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }
}