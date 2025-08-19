package accessories_count;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(AccessoriesCountTab.class.getName());

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
        refreshButton.addActionListener(e -> refresh());
        add(mainPanel, BorderLayout.CENTER);
        add(refreshButton, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        LOGGER.info("Refreshing AccessoriesCountTab");
        // Clear existing data
        accessoryTableModel.setRowCount(0);

        // Update Accessories
        try {
            ArrayList<HashMap<String, String>> accessories = FileUtils.loadAccessories();
            if (accessories == null || accessories.isEmpty()) {
                accessoryTableModel.addRow(new Object[]{"No Data", 0});
                LOGGER.info("No accessories data found");
            } else {
                for (String type : PeripheralUtils.getPeripheralTypes(accessories)) {
                    int count = PeripheralUtils.getPeripheralCount(type, accessories);
                    accessoryTableModel.addRow(new Object[]{type, count});
                    LOGGER.log(Level.INFO, "Added accessory type ''{0}'' with count {1}", new Object[]{type, count});
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating accessories display: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Error updating accessories display: {0}", e.getMessage());
        }

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private class AddAccessoryAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, AccessoriesCountTab.this), "Add New Accessory Type", true);
            dialog.setSize(300, 150);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setLocationRelativeTo(AccessoriesCountTab.this);

            JPanel inputPanel = new JPanel();
            inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
            JTextField typeField = UIComponentUtils.createFormattedTextField();
            inputPanel.add(UIComponentUtils.createAlignedLabel("Accessory Type:"));
            inputPanel.add(typeField);

            try {
                ArrayList<HashMap<String, String>> accessories = FileUtils.loadAccessories();
                Set<String> existingTypes = new HashSet<>(PeripheralUtils.getPeripheralTypes(accessories));
                typeField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                    @Override
                    public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
                    @Override
                    public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
                    @Override
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }

                    private void update() {
                        String text = typeField.getText().trim();
                        if (!text.isEmpty()) {
                            String normalized = DataUtils.capitalizeWords(text);
                            if (existingTypes.contains(normalized)) {
                                typeField.setToolTipText("'" + normalized + "' already exists");
                            } else {
                                typeField.setToolTipText("Start typing to see existing types (use valid characters: letters, numbers, -, _)");
                            }
                        }
                    }
                });
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error loading existing accessory types: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.SEVERE, "Error loading existing accessory types: {0}", ex.getMessage());
                return;
            }

            JButton addButton = UIComponentUtils.createFormattedButton("Add");
            addButton.addActionListener(ev -> {
                String newType = typeField.getText().trim();
                if (newType.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Accessory type cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.severe("Attempted to add empty accessory type");
                } else if (!newType.matches("[a-zA-Z0-9-_]+")) {
                    JOptionPane.showMessageDialog(dialog, "Invalid characters. Use letters, numbers, -, or _ only", "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.log(Level.SEVERE, "Invalid characters in accessory type: {0}", newType);
                } else {
                    String normalizedType = DataUtils.capitalizeWords(newType); // Normalize to title case
                    try {
                        ArrayList<HashMap<String, String>> accessories = FileUtils.loadAccessories();
                        Set<String> existingTypes = new HashSet<>(PeripheralUtils.getPeripheralTypes(accessories));
                        if (existingTypes.contains(normalizedType)) {
                            JOptionPane.showMessageDialog(dialog, "Accessory type already exists", "Error", JOptionPane.ERROR_MESSAGE);
                            LOGGER.log(Level.SEVERE, "Accessory type ''{0}'' already exists", normalizedType);
                        } else {
                            DatabaseUtils.updatePeripheral(normalizedType, 0, "Accessory");
                            JOptionPane.showMessageDialog(dialog, "Accessory type '" + normalizedType + "' added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                            LOGGER.log(Level.INFO, "Added accessory type ''{0}'' with count 0", normalizedType);
                            dialog.dispose();
                            refresh();
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(dialog, "Error adding accessory type: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        LOGGER.log(Level.SEVERE, "Error adding accessory type ''{0}'': {1}", new Object[]{normalizedType, ex.getMessage()});
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
                LOGGER.severe("No accessory type selected for AddToStorageAction");
                return;
            }
            String type = (String) accessoryTableModel.getValueAt(selectedRow, 0);
            int currentCount = (int) accessoryTableModel.getValueAt(selectedRow, 1);
            try {
                DatabaseUtils.updatePeripheral(type, 1, "Accessory");
                accessoryTableModel.setValueAt(currentCount + 1, selectedRow, 1);
                statusLabel.setText("Successfully added 1 to " + type);
                LOGGER.log(Level.INFO, "Added 1 to accessory type ''{0}'', new count: {1}{2}", new Object[]{type, currentCount, 1});
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
                LOGGER.log(Level.SEVERE, "Error adding to accessory type ''{0}'': {1}", new Object[]{type, ex.getMessage()});
            }
        }
    }

    private class RemoveFromStorageAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedRow = accessoryTable.getSelectedRow();
            if (selectedRow == -1) {
                statusLabel.setText("Error: Select an accessory type first");
                LOGGER.severe("No accessory type selected for RemoveFromStorageAction");
                return;
            }
            String type = (String) accessoryTableModel.getValueAt(selectedRow, 0);
            int currentCount = (int) accessoryTableModel.getValueAt(selectedRow, 1);
            if (currentCount <= 0) {
                statusLabel.setText("Error: Count cannot go below 0");
                LOGGER.log(Level.SEVERE, "Attempted to reduce count below 0 for accessory type ''{0}''", type);
                return;
            }
            try {
                DatabaseUtils.updatePeripheral(type, -1, "Accessory");
                accessoryTableModel.setValueAt(currentCount - 1, selectedRow, 1);
                statusLabel.setText("Successfully removed 1 from " + type);
                LOGGER.log(Level.INFO, "Removed 1 from accessory type ''{0}'', new count: {1}", new Object[]{type, currentCount - 1});
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
                LOGGER.log(Level.SEVERE, "Error removing from accessory type ''{0}'': {1}", new Object[]{type, ex.getMessage()});
            }
        }
    }
}