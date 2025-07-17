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

import utils.DatabaseUtils;
import utils.FileUtils;
import utils.UIComponentUtils;

public class LogAdaptersTab extends JPanel {
    private final JTable adapterTable;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;

    public LogAdaptersTab() {
        setLayout(new BorderLayout(10, 10));

        // Initialize table model and table
        tableModel = new DefaultTableModel(new String[]{"Adapter Type", "Count"}, 0);
        adapterTable = new JTable(tableModel);
        adapterTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        updateTableData();

        // Main panel with vertical layout
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        JButton addAdapterButton = UIComponentUtils.createFormattedButton("Add New Adapter Type");
        JButton addToStorageButton = UIComponentUtils.createFormattedButton("Add to Storage");
        JButton removeFromStorageButton = UIComponentUtils.createFormattedButton("Remove from Storage");
        statusLabel = UIComponentUtils.createAlignedLabel("");

        addAdapterButton.addActionListener(new AddAdapterAction());
        addToStorageButton.addActionListener(new AddToStorageAction());
        removeFromStorageButton.addActionListener(new RemoveFromStorageAction());

        buttonPanel.add(addAdapterButton);
        buttonPanel.add(addToStorageButton);
        buttonPanel.add(removeFromStorageButton);
        panel.add(UIComponentUtils.createScrollableContentPanel(adapterTable));
        panel.add(buttonPanel);
        panel.add(statusLabel);

        add(panel, BorderLayout.CENTER);
    }

    private void updateTableData() {
        tableModel.setRowCount(0);
        try {
            ArrayList<HashMap<String, String>> adapters = FileUtils.loadAdapters();
            if (adapters == null || adapters.isEmpty()) {
                tableModel.addRow(new Object[]{"No Data", 0});
            } else {
                for (HashMap<String, String> adapter : adapters) {
                    String type = adapter.getOrDefault("Adapter_Type", "");
                    String countStr = adapter.getOrDefault("Count", "0");
                    if (!type.isEmpty()) {
                        int count = Integer.parseInt(countStr);
                        tableModel.addRow(new Object[]{type, count});
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading adapters: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class AddAdapterAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(LogAdaptersTab.this), "Add New Adapter Type", true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(300, 150);
            dialog.setLocationRelativeTo(LogAdaptersTab.this);

            JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
            JTextField typeField = UIComponentUtils.createFormattedTextField();
            typeField.setToolTipText("Start typing to see existing types (use valid characters: letters, numbers, -, _)");
            inputPanel.add(UIComponentUtils.createAlignedLabel("Adapter Type:"), BorderLayout.NORTH);
            inputPanel.add(typeField, BorderLayout.CENTER);

            // Autofill suggestion
            Set<String> existingTypes = new HashSet<>();
            try {
                ArrayList<HashMap<String, String>> adapters = FileUtils.loadAdapters();
                if (adapters != null) {
                    for (HashMap<String, String> adapter : adapters) {
                        String type = adapter.getOrDefault("Adapter_Type", "");
                        if (!type.isEmpty()) {
                            existingTypes.add(type); // Preserve original case
                        }
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error loading existing adapter types: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.showMessageDialog(dialog, "Adapter type cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                } else if (!newType.matches("[a-zA-Z0-9-_]+")) {
                    JOptionPane.showMessageDialog(dialog, "Invalid characters. Use letters, numbers, -, or _ only", "Error", JOptionPane.ERROR_MESSAGE);
                } else if (existingTypes.contains(newType)) {
                    JOptionPane.showMessageDialog(dialog, "Adapter type already exists", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    try {
                        DatabaseUtils.updatePeripheralCount(newType, 0, "Adapter");
                        JOptionPane.showMessageDialog(dialog, "Adapter type '" + newType + "' added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        updateTableData();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(dialog, "Error adding adapter type: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            int selectedRow = adapterTable.getSelectedRow();
            if (selectedRow == -1) {
                statusLabel.setText("Error: Select an adapter type first");
                return;
            }
            String type = (String) tableModel.getValueAt(selectedRow, 0);
            int currentCount = (int) tableModel.getValueAt(selectedRow, 1);
            try {
                DatabaseUtils.updatePeripheralCount(type, 1, "Adapter");
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
            int selectedRow = adapterTable.getSelectedRow();
            if (selectedRow == -1) {
                statusLabel.setText("Error: Select an adapter type first");
                return;
            }
            String type = (String) tableModel.getValueAt(selectedRow, 0);
            int currentCount = (int) tableModel.getValueAt(selectedRow, 1);
            if (currentCount <= 0) {
                statusLabel.setText("Error: Count cannot go below 0");
                return;
            }
            try {
                DatabaseUtils.updatePeripheralCount(type, -1, "Adapter");
                tableModel.setValueAt(currentCount - 1, selectedRow, 1);
                statusLabel.setText("Successfully removed 1 from " + type);
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        }
    }
}