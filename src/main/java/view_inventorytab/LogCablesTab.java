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

public class LogCablesTab extends JPanel {
    private final JTable cableTable;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;

    public LogCablesTab() {
        setLayout(new BorderLayout(10, 10));

        // Initialize table model and table
        tableModel = new DefaultTableModel(new String[]{"Cable Type", "Count"}, 0);
        cableTable = new JTable(tableModel);
        cableTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        updateTableData();

        // Main panel with vertical layout
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        JButton addCableButton = UIComponentUtils.createFormattedButton("Add New Cable Type");
        JButton addToStorageButton = UIComponentUtils.createFormattedButton("Add to Storage");
        JButton removeFromStorageButton = UIComponentUtils.createFormattedButton("Remove from Storage");
        statusLabel = UIComponentUtils.createAlignedLabel("");

        addCableButton.addActionListener(new AddCableAction());
        addToStorageButton.addActionListener(new AddToStorageAction());
        removeFromStorageButton.addActionListener(new RemoveFromStorageAction());

        buttonPanel.add(addCableButton);
        buttonPanel.add(addToStorageButton);
        buttonPanel.add(removeFromStorageButton);
        panel.add(UIComponentUtils.createScrollableContentPanel(cableTable));
        panel.add(buttonPanel);
        panel.add(statusLabel);

        add(panel, BorderLayout.CENTER);
    }

    private void updateTableData() {
        tableModel.setRowCount(0);
        try {
            ArrayList<HashMap<String, String>> cables = FileUtils.loadCables();
            if (cables == null || cables.isEmpty()) {
                tableModel.addRow(new Object[]{"No Data", 0});
            } else {
                for (HashMap<String, String> cable : cables) {
                    String type = cable.getOrDefault("Cable_Type", "").toLowerCase();
                    String countStr = cable.getOrDefault("Count", "0");
                    if (!type.isEmpty()) {
                        int count = Integer.parseInt(countStr);
                        tableModel.addRow(new Object[]{type, count});
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading cables: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class AddCableAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(LogCablesTab.this), "Add New Cable Type", true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(300, 150);
            dialog.setLocationRelativeTo(LogCablesTab.this);

            JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
            JTextField typeField = UIComponentUtils.createFormattedTextField();
            typeField.setToolTipText("Start typing to see existing types");
            inputPanel.add(UIComponentUtils.createAlignedLabel("Cable Type:"), BorderLayout.NORTH);
            inputPanel.add(typeField, BorderLayout.CENTER);

            // Autofill suggestion
            Set<String> existingTypes = new HashSet<>();
            try {
                ArrayList<HashMap<String, String>> cables = FileUtils.loadCables();
                if (cables != null) {
                    for (HashMap<String, String> cable : cables) {
                        String type = cable.getOrDefault("Cable_Type", "").toLowerCase();
                        if (!type.isEmpty()) {
                            existingTypes.add(type);
                        }
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error loading existing cable types: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }

            typeField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSuggestion(typeField, existingTypes); }
                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSuggestion(typeField, existingTypes); }
                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSuggestion(typeField, existingTypes); }
                private void updateSuggestion(JTextField field, Set<String> types) {
                    String text = field.getText().toLowerCase();
                    if (text.length() > 0) {
                        for (String type : types) {
                            if (type.startsWith(text) && !type.equals(text)) {
                                field.setToolTipText("Suggestion: " + type);
                                return;
                            }
                        }
                    }
                    field.setToolTipText("Start typing to see existing types");
                }
            });

            JButton addButton = UIComponentUtils.createFormattedButton("Add");
            addButton.addActionListener(ev -> {
                String newType = typeField.getText().trim();
                if (newType.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Cable type cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                } else if (existingTypes.contains(newType.toLowerCase())) {
                    JOptionPane.showMessageDialog(dialog, "Cable type already exists", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    try {
                        DatabaseUtils.updatePeripheralCount(newType, 0, "Cable");
                        JOptionPane.showMessageDialog(dialog, "Cable type '" + newType + "' added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        updateTableData();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(dialog, "Error adding cable type: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            int selectedRow = cableTable.getSelectedRow();
            if (selectedRow == -1) {
                statusLabel.setText("Error: Select a cable type first");
                return;
            }
            String type = (String) tableModel.getValueAt(selectedRow, 0);
            int currentCount = (int) tableModel.getValueAt(selectedRow, 1);
            try {
                DatabaseUtils.updatePeripheralCount(type, 1, "Cable");
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
            int selectedRow = cableTable.getSelectedRow();
            if (selectedRow == -1) {
                statusLabel.setText("Error: Select a cable type first");
                return;
            }
            String type = (String) tableModel.getValueAt(selectedRow, 0);
            int currentCount = (int) tableModel.getValueAt(selectedRow, 1);
            if (currentCount <= 0) {
                statusLabel.setText("Error: Count cannot go below 0");
                return;
            }
            try {
                DatabaseUtils.updatePeripheralCount(type, -1, "Cable");
                tableModel.setValueAt(currentCount - 1, selectedRow, 1);
                statusLabel.setText("Successfully removed 1 from " + type);
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        }
    }
}