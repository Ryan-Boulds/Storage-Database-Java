package log_cables.actions;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import log_cables.CablesDAO;
import log_cables.LogCablesTab;
import utils.UIComponentUtils;

public class AddCableAction implements ActionListener {
    private final LogCablesTab tab;
    private static final Logger LOGGER = Logger.getLogger(AddCableAction.class.getName());

    public AddCableAction(LogCablesTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Save the currently selected cable type, if any, as final
        final String selectedCableType;
        int selectedRow = tab.getCableTable().getSelectedRow();
        if (selectedRow != -1) {
            selectedCableType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        } else {
            selectedCableType = null;
        }

        JDialog dialog = new JDialog((JFrame) tab.getTopLevelAncestor(), "Add Cable", true);
        dialog.setSize(400, 250);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Cable Type Selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Cable Type:"), gbc);

        gbc.gridx = 1;
        JLabel cableTypeLabel = new JLabel("None selected");
        panel.add(cableTypeLabel, gbc);

        gbc.gridx = 2;
        JButton selectCableTypeButton = UIComponentUtils.createFormattedButton("Select");
        selectCableTypeButton.addActionListener(e1 -> {
            SelectCableTypeDialog selectDialog = new SelectCableTypeDialog((JFrame) tab.getTopLevelAncestor());
            String selectedType = selectDialog.showDialog();
            if (selectedType != null && !selectedType.isEmpty()) {
                cableTypeLabel.setText(selectedType);
            }
        });
        panel.add(selectCableTypeButton, gbc);

        // Quantity
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Quantity:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField quantityField = UIComponentUtils.createFormattedTextField();
        panel.add(quantityField, gbc);
        gbc.gridwidth = 1;

        // Location Selection
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Location:"), gbc);

        gbc.gridx = 1;
        JLabel locationLabel = new JLabel("None selected");
        panel.add(locationLabel, gbc);

        gbc.gridx = 2;
        JButton selectLocationButton = UIComponentUtils.createFormattedButton("Select");
        selectLocationButton.addActionListener(e1 -> {
            SelectLocationDialog selectDialog = new SelectLocationDialog((JFrame) tab.getTopLevelAncestor());
            String selectedLocation = selectDialog.showDialog();
            if (selectedLocation != null && !selectedLocation.isEmpty()) {
                locationLabel.setText(selectedLocation);
            }
        });
        panel.add(selectLocationButton, gbc);

        // Add Button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton addButton = UIComponentUtils.createFormattedButton("Add Cable");
        addButton.addActionListener(e1 -> {
            String cableType = cableTypeLabel.getText();
            if ("None selected".equals(cableType)) {
                JOptionPane.showMessageDialog(dialog, "Please select a cable type", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String quantityText = quantityField.getText().trim();
            int quantity;
            try {
                quantity = Integer.parseInt(quantityText);
                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Quantity must be positive", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid quantity format", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String selectedLocation = locationLabel.getText();
            if ("None selected".equals(selectedLocation)) {
                JOptionPane.showMessageDialog(dialog, "Please select a location", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                CablesDAO.addCable(cableType, quantity, selectedLocation);
                tab.setStatus("Added " + quantity + " " + cableType + " to " + selectedLocation);
                tab.refreshTree(); // Refresh the tree to update Unassigned nodes
                tab.refresh();
                if (selectedCableType != null) {
                    JTable table = tab.getCableTable();
                    for (int i = 0; i < table.getRowCount(); i++) {
                        if (selectedCableType.equals(table.getValueAt(i, 0))) {
                            table.setRowSelectionInterval(i, i);
                            break;
                        }
                    }
                }
                dialog.dispose();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error adding cable: {0}", ex.getMessage());
                tab.setStatus("Error adding cable: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "Error adding cable: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(addButton, gbc);

        dialog.add(panel);
        dialog.setLocationRelativeTo(tab);
        dialog.setVisible(true);
    }
}