package log_cables.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import log_cables.CablesDAO;
import log_cables.LogCablesTab;
import utils.UIComponentUtils;

public class AddCableAction implements ActionListener {
    private final LogCablesTab tab;

    public AddCableAction(LogCablesTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tab.getLocationTree().getLastSelectedPathComponent();
        if (node == null) {
            tab.setStatus("Error: Select a location first");
            return;
        }
        String location = (String) node.getUserObject();

        JDialog dialog = new JDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, tab), "Add New Cable Type", true);
        dialog.setSize(300, 200);
        dialog.setLayout(new java.awt.BorderLayout());
        dialog.setLocationRelativeTo(tab);

        JPanel inputPanel = new JPanel(new java.awt.BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel cableLabel = new JLabel("Enter new cable type:");
        JTextField cableField = UIComponentUtils.createFormattedTextField();
        JLabel countLabel = new JLabel("Enter count:");
        JTextField countField = UIComponentUtils.createFormattedTextField();
        countField.setText("0");

        // Prevent spaces in cable type
        cableField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    e.consume();
                }
            }
        });

        JPanel fieldsPanel = new JPanel(new java.awt.GridLayout(2, 2, 5, 5));
        fieldsPanel.add(cableLabel);
        fieldsPanel.add(cableField);
        fieldsPanel.add(countLabel);
        fieldsPanel.add(countField);
        inputPanel.add(fieldsPanel, java.awt.BorderLayout.CENTER);

        JButton addButton = UIComponentUtils.createFormattedButton("Add");
        addButton.addActionListener(e1 -> {
            String newType = cableField.getText().trim();
            String countText = countField.getText().trim();
            if (newType.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Cable type cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!newType.matches("[a-zA-Z0-9-_]+")) {
                JOptionPane.showMessageDialog(dialog, "Invalid characters. Use letters, numbers, -, or _ only", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int count;
            try {
                count = Integer.parseInt(countText);
                if (count < 0) {
                    JOptionPane.showMessageDialog(dialog, "Count cannot be negative", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid count format", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                if (CablesDAO.cableExistsAtLocation(newType, location)) {
                    JOptionPane.showMessageDialog(dialog, "Cable type already exists at this location", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                CablesDAO.addCable(newType, count, location);
                tab.setStatus("Successfully added new cable type '" + newType + "' at " + 
                              (location.equals(tab.getUnassignedLocation()) ? tab.getUnassignedLocation() : location));
                tab.refreshTable(location);
                dialog.dispose();
            } catch (SQLException ex) {
                tab.setStatus("Error adding cable type: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "Error adding cable type: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(inputPanel, java.awt.BorderLayout.CENTER);
        dialog.add(addButton, java.awt.BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}