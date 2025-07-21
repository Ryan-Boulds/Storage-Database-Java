package view_inventorytab;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import utils.DataUtils;
import utils.DatabaseUtils;
import utils.FileUtils;
import utils.PeripheralUtils;
import utils.UIComponentUtils;

public final class AccessoriesCountTab extends JPanel {
    private final DefaultTableModel accessoryTableModel;
    private final JTable accessoryTable;
    private final JPanel mainPanel;
    private final JLabel statusLabel;

    public AccessoriesCountTab() {
        setLayout(new BorderLayout(10, 10));

        // Initialize table model and table
        accessoryTableModel = new DefaultTableModel(new String[]{"Accessory Type", "Count"}, 0);
        accessoryTable = new JTable(accessoryTableModel);
        accessoryTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // Main panel with vertical layout
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Accessories section
        JPanel accessoriesPanel = new JPanel(new BorderLayout(10, 10));
        accessoriesPanel.add(UIComponentUtils.createScrollableContentPanel(accessoryTable), BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        JButton addAccessoryButton = UIComponentUtils.createFormattedButton("Add New Accessory Type");
        JButton addToStorageButton = UIComponentUtils.createFormattedButton("Add to Storage");
        JButton removeFromStorageButton = UIComponentUtils.createFormattedButton("Remove from Storage");
        statusLabel = UIComponentUtils.createAlignedLabel("");

        addAccessoryButton.addActionListener(new AddAccessoryAction());
        addToStorageButton.addActionListener(new AddToStorageAction());
        removeFromStorageButton.addActionListener(new RemoveFromStorageAction());

        buttonPanel.add(addAccessoryButton);
        buttonPanel.add(addToStorageButton);
        buttonPanel.add(removeFromStorageButton);
        accessoriesPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(accessoriesPanel);

        // Refresh button at the bottom
        JButton refreshButton = UIComponentUtils.createFormattedButton("Refresh");
        refreshButton.addActionListener(e -> updateDisplay());
        add(mainPanel, BorderLayout.CENTER);
        add(refreshButton, BorderLayout.SOUTH);

        updateDisplay();
    }

    private void updateDisplay() {
        // Clear existing data
        accessoryTableModel.setRowCount(0);

        // Update Accessories
        try {
            ArrayList<HashMap<String, String>> accessories = FileUtils.loadAccessories();
            if (accessories == null || accessories.isEmpty()) {
                accessoryTableModel.addRow(new Object[]{"No Data", 0});
            } else {
                for (String type : PeripheralUtils.getPeripheralTypes(accessories)) {
                    int count = PeripheralUtils.getPeripheralCount(type, accessories);
                    accessoryTableModel.addRow(new Object[]{type, count});
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating accessories display: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private class AddAccessoryAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(AccessoriesCountTab.this), "Add New Accessory Type", true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(300, 150);
            dialog.setLocationRelativeTo(AccessoriesCountTab.this);

            JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
            JTextField typeField = UIComponentUtils.createFormattedTextField();
            typeField.setToolTipText("Start typing to see existing types (use valid characters: letters, numbers, -, _)");
            inputPanel.add(UIComponentUtils.createAlignedLabel("Accessory Type:"), BorderLayout.NORTH);
            inputPanel.add(typeField, BorderLayout.CENTER);

            // Autofill suggestion
            Set<String> existingTypes = new HashSet<>();
            try {
                ArrayList<HashMap<String, String>> accessories = FileUtils.loadAccessories();
                if (accessories != null) {
                    for (String type : PeripheralUtils.getPeripheralTypes(accessories)) {
                        existingTypes.add(DataUtils.capitalizeWords(type)); // Normalize to title case
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error loading existing accessory types: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }

            typeField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSuggestion(typeField, existingTypes); }
                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSuggestion(typeField, existingTypes); }
                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSuggestion(typeField, existingTypes); }
                private void updateSuggestion(JTextField field, Set<String> types) {
                    String text = field.getText();
                    if (text.length() > 0) {
                        for (String type : types) {
                            if (type.startsWith(text) && !type.equals(text)) {
                                field.setToolTipText("Suggestion: " + type);
                                return;
                            }
                        }
                    }
                    field.setToolTipText("Start typing to see existing types (use valid characters: letters, numbers, -, _)");
                }
            });

            JButton addButton = UIComponentUtils.createFormattedButton("Add");
            addButton.addActionListener(ev -> {
                String newType = typeField.getText().trim();
                if (newType.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Accessory type cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                } else if (!newType.matches("[a-zA-Z0-9-_]+")) {
                    JOptionPane.showMessageDialog(dialog, "Invalid characters. Use letters, numbers, -, or _ only", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    String normalizedType = DataUtils.capitalizeWords(newType); // Normalize to title case
                    if (existingTypes.contains(normalizedType)) {
                        JOptionPane.showMessageDialog(dialog, "Accessory type already exists", "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        try {
                            DatabaseUtils.updatePeripheralCount(normalizedType, 0, "Accessory");
                            JOptionPane.showMessageDialog(dialog, "Accessory type '" + normalizedType + "' added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                            dialog.dispose();
                            updateDisplay();
                        } catch (SQLException ex) {
                            JOptionPane.showMessageDialog(dialog, "Error adding accessory type: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });

            dialog.add(inputPanel, BorderLayout.CENTER);
            dialog.add(addButton, BorderLayout.SOUTH);
            dialog.setVisible(true);
        }
    }

    private class AddToStorageAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedRow = accessoryTable.getSelectedRow();
            if (selectedRow == -1) {
                statusLabel.setText("Error: Select an accessory type first");
                return;
            }
            String type = (String) accessoryTableModel.getValueAt(selectedRow, 0);
            int currentCount = (int) accessoryTableModel.getValueAt(selectedRow, 1);
            try {
                DatabaseUtils.updatePeripheralCount(type, 1, "Accessory");
                accessoryTableModel.setValueAt(currentCount + 1, selectedRow, 1);
                statusLabel.setText("Successfully added 1 to " + type);
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        }
    }

    private class RemoveFromStorageAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedRow = accessoryTable.getSelectedRow();
            if (selectedRow == -1) {
                statusLabel.setText("Error: Select an accessory type first");
                return;
            }
            String type = (String) accessoryTableModel.getValueAt(selectedRow, 0);
            int currentCount = (int) accessoryTableModel.getValueAt(selectedRow, 1);
            if (currentCount <= 0) {
                statusLabel.setText("Error: Count cannot go below 0");
                return;
            }
            try {
                DatabaseUtils.updatePeripheralCount(type, -1, "Accessory");
                accessoryTableModel.setValueAt(currentCount - 1, selectedRow, 1);
                statusLabel.setText("Successfully removed 1 from " + type);
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        }
    }
}