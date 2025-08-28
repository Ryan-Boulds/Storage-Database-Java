package log_cables.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;

import log_cables.CablesDAO;
import log_cables.LogCablesTab;
import utils.UIComponentUtils;

public class MoveCableAction implements ActionListener {
    private final LogCablesTab tab;

    public MoveCableAction(LogCablesTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedRow = tab.getCableTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a cable type first");
            return;
        }
        String cableType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        int availableCount = ((Number) tab.getTableModel().getValueAt(selectedRow, 1)).intValue();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tab.getLocationTree().getLastSelectedPathComponent();
        if (node == null) {
            tab.setStatus("Error: Select a location first");
            return;
        }
        String sourceLocation = (String) node.getUserObject();

        Set<String> locations = CablesDAO.getAllLocations();
        locations.remove(sourceLocation);
        if (locations.isEmpty()) {
            tab.setStatus("Error: No other locations to move to");
            return;
        }
        String[] locationArray = locations.toArray(new String[0]);

        JDialog dialog = new JDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, tab), "Move Cable", true);
        dialog.setSize(350, 200);
        dialog.setLayout(new java.awt.BorderLayout());
        dialog.setLocationRelativeTo(tab);

        JPanel inputPanel = new JPanel(new java.awt.BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel label = new JLabel("Move " + cableType + " from " + sourceLocation + " to:");
        JComboBox<String> locationCombo = UIComponentUtils.createFormattedComboBox(locationArray);
        JLabel qtyLabel = new JLabel("Quantity (max " + availableCount + "):");
        JTextField qtyField = UIComponentUtils.createFormattedTextField();
        qtyField.setText("1");
        JButton transferAllButton = UIComponentUtils.createFormattedButton("Transfer All");
        transferAllButton.addActionListener(e1 -> qtyField.setText(String.valueOf(availableCount)));

        JPanel qtyPanel = new JPanel(new java.awt.BorderLayout(5, 0));
        qtyPanel.add(qtyField, java.awt.BorderLayout.CENTER);
        qtyPanel.add(transferAllButton, java.awt.BorderLayout.EAST);

        inputPanel.add(label, java.awt.BorderLayout.NORTH);
        inputPanel.add(locationCombo, java.awt.BorderLayout.CENTER);
        inputPanel.add(qtyPanel, java.awt.BorderLayout.SOUTH);
        qtyPanel.add(qtyLabel, java.awt.BorderLayout.NORTH);

        JButton moveButton = UIComponentUtils.createFormattedButton("Move");
        moveButton.addActionListener(e1 -> {
            String targetLocation = (String) locationCombo.getSelectedItem();
            String qtyText = qtyField.getText().trim();
            try {
                int quantity = Integer.parseInt(qtyText);
                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Quantity must be positive", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (quantity > availableCount) {
                    JOptionPane.showMessageDialog(dialog, "Cannot move more than " + availableCount + " cables", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int id;
                try {
                    id = CablesDAO.getCableId(cableType, sourceLocation);
                    if (id == -1) {
                        tab.setStatus("Error: Cable type '" + cableType + "' not found at " + sourceLocation);
                        return;
                    }
                } catch (SQLException ex) {
                    tab.setStatus("Error retrieving cable ID: " + ex.getMessage());
                    JOptionPane.showMessageDialog(dialog, "Error retrieving cable ID: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Disable UI elements during processing
                moveButton.setEnabled(false);
                locationCombo.setEnabled(false);
                qtyField.setEnabled(false);
                transferAllButton.setEnabled(false);
                tab.setStatus("Processing...");
                // Perform move operation in background
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws SQLException {
                        CablesDAO.moveCable(id, targetLocation, quantity);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get(); // Check for exceptions
                            tab.setStatus("Successfully moved " + quantity + " of " + cableType + " to " + targetLocation);
                            tab.refreshTable(sourceLocation);
                            dialog.dispose();
                        } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
                            String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                            tab.setStatus("Error moving cable: " + errorMsg);
                            JOptionPane.showMessageDialog(dialog, "Error moving cable: " + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            moveButton.setEnabled(true);
                            locationCombo.setEnabled(true);
                            qtyField.setEnabled(true);
                            transferAllButton.setEnabled(true);
                        }
                    }
                };
                worker.execute();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid quantity: " + qtyText, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(inputPanel, java.awt.BorderLayout.CENTER);
        dialog.add(moveButton, java.awt.BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}