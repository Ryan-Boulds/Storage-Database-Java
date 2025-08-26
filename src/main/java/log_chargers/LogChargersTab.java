package log_chargers;

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

import utils.DatabaseUtils;
import utils.FileUtils;
import utils.UIComponentUtils;

public final class LogChargersTab extends JPanel {
    private final JTable chargerTable;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;

    public LogChargersTab() {
        setLayout(new BorderLayout(10, 10));

        // Initialize table model and table
        tableModel = new DefaultTableModel(new String[]{"Charger Type", "Count"}, 0);
        chargerTable = new JTable(tableModel);
        chargerTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        refresh();

        // Main panel with vertical layout
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        JButton addChargerButton = UIComponentUtils.createFormattedButton("Add New Charger Type");
        JButton addToStorageButton = UIComponentUtils.createFormattedButton("Add to Storage");
        JButton removeFromStorageButton = UIComponentUtils.createFormattedButton("Remove from Storage");
        statusLabel = UIComponentUtils.createAlignedLabel("");

        addChargerButton.addActionListener(new AddChargerAction());
        addToStorageButton.addActionListener(new AddToStorageAction());
        removeFromStorageButton.addActionListener(new RemoveFromStorageAction());

        buttonPanel.add(addChargerButton);
        buttonPanel.add(addToStorageButton);
        buttonPanel.add(removeFromStorageButton);
        panel.add(UIComponentUtils.createScrollableContentPanel(chargerTable));
        panel.add(buttonPanel);
        panel.add(statusLabel);

        add(panel, BorderLayout.CENTER);
    }

    public void refresh() {
        tableModel.setRowCount(0);
        try {
            ArrayList<HashMap<String, String>> chargers = FileUtils.loadChargers();
            if (chargers == null || chargers.isEmpty()) {
                tableModel.addRow(new Object[]{"No Data", 0});
            } else {
                for (HashMap<String, String> charger : chargers) {
                    String type = charger.getOrDefault("Charger_Type", "");
                    String countStr = charger.getOrDefault("Count", "0");
                    if (!type.isEmpty()) {
                        int count = Integer.parseInt(countStr);
                        tableModel.addRow(new Object[]{type, count});
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading chargers: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class AddChargerAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(LogChargersTab.this), "Add New Charger Type", true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(300, 150);
            dialog.setLocationRelativeTo(LogChargersTab.this);

            JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
            JTextField typeField = UIComponentUtils.createFormattedTextField();
            typeField.setToolTipText("Start typing to see existing types (use valid characters: letters, numbers, -, _)");
            inputPanel.add(UIComponentUtils.createAlignedLabel("Charger Type:"), BorderLayout.NORTH);
            inputPanel.add(typeField, BorderLayout.CENTER);

            // Autofill suggestion
            Set<String> existingTypes = new HashSet<>();
            try {
                ArrayList<HashMap<String, String>> chargers = FileUtils.loadChargers();
                if (chargers != null) {
                    for (HashMap<String, String> charger : chargers) {
                        String type = charger.getOrDefault("Charger_Type", "");
                        if (!type.isEmpty()) {
                            existingTypes.add(type);
                        }
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error loading existing charger types: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.showMessageDialog(dialog, "Charger type cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                } else if (!newType.matches("[a-zA-Z0-9-_]+")) {
                    JOptionPane.showMessageDialog(dialog, "Invalid characters. Use letters, numbers, -, or _ only", "Error", JOptionPane.ERROR_MESSAGE);
                } else if (existingTypes.contains(newType)) {
                    JOptionPane.showMessageDialog(dialog, "Charger type already exists", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    try {
                        DatabaseUtils.updatePeripheral(newType, 0, "Charger");
                        JOptionPane.showMessageDialog(dialog, "Charger type '" + newType + "' added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        refresh();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(dialog, "Error adding charger type: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            int selectedRow = chargerTable.getSelectedRow();
            if (selectedRow == -1) {
                statusLabel.setText("Error: Select a charger type first");
                return;
            }
            String type = (String) tableModel.getValueAt(selectedRow, 0);
            int currentCount = (int) tableModel.getValueAt(selectedRow, 1);
            try {
                DatabaseUtils.updatePeripheral(type, 1, "Charger");
                tableModel.setValueAt(currentCount + 1, selectedRow, 1);
                statusLabel.setText("Successfully added 1 to " + type);
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        }
    }

    private class RemoveFromStorageAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedRow = chargerTable.getSelectedRow();
            if (selectedRow == -1) {
                statusLabel.setText("Error: Select a charger type first");
                return;
            }
            String type = (String) tableModel.getValueAt(selectedRow, 0);
            int currentCount = (int) tableModel.getValueAt(selectedRow, 1);
            if (currentCount <= 0) {
                statusLabel.setText("Error: Count cannot go below 0");
                return;
            }
            try {
                DatabaseUtils.updatePeripheral(type, -1, "Charger");
                tableModel.setValueAt(currentCount - 1, selectedRow, 1);
                statusLabel.setText("Successfully removed 1 from " + type);
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        }
    }
}