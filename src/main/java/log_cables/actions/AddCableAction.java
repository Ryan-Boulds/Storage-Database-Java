package log_cables.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.tree.TreePath;

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
        String location = node.getUserObject().equals("Unassigned in this location")
                ? buildPathFromNode((DefaultMutableTreeNode) node.getParent())
                : buildPathFromNode(node);
        String separator = LogCablesTab.getPathSeparator();
        String parentLocation = location.contains(separator) 
                ? location.substring(0, location.lastIndexOf(separator)) 
                : null;

        JDialog dialog = new JDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, tab), "Add New Cable Type", true);
        dialog.setSize(300, 200);
        dialog.setLayout(new java.awt.BorderLayout());
        dialog.setLocationRelativeTo(tab);

        JPanel inputPanel = new JPanel(new java.awt.BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel typeLabel = new JLabel("Enter cable type:");
        JTextField typeField = UIComponentUtils.createFormattedTextField();
        JLabel quantityLabel = new JLabel("Enter quantity:");
        JTextField quantityField = UIComponentUtils.createFormattedTextField();
        inputPanel.add(typeLabel, java.awt.BorderLayout.NORTH);
        inputPanel.add(typeField, java.awt.BorderLayout.CENTER);
        inputPanel.add(quantityLabel, java.awt.BorderLayout.SOUTH);
        inputPanel.add(quantityField, java.awt.BorderLayout.SOUTH);

        JButton addButton = UIComponentUtils.createFormattedButton("Add");
        addButton.addActionListener(e1 -> {
            String cableType = typeField.getText().trim();
            String quantityText = quantityField.getText().trim();
            if (cableType.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Cable type cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int quantity;
            try {
                quantity = Integer.parseInt(quantityText);
                if (quantity < 0) {
                    JOptionPane.showMessageDialog(dialog, "Quantity cannot be negative", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid quantity format", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                if (CablesDAO.cableExistsAtLocation(cableType, location)) {
                    JOptionPane.showMessageDialog(dialog, "Cable type already exists at this location", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                CablesDAO.addCable(cableType, quantity, location, parentLocation);
                tab.setStatus("Successfully added " + quantity + " " + cableType + " to " + location);
                tab.refresh();
                dialog.dispose();
            } catch (SQLException ex) {
                tab.setStatus("Error adding cable: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "Error adding cable: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(inputPanel, java.awt.BorderLayout.CENTER);
        dialog.add(addButton, java.awt.BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private String buildPathFromNode(DefaultMutableTreeNode node) {
        if (node == null || node.isRoot()) {
            return null;
        }
        TreePath path = new TreePath(node.getPath());
        Object[] nodes = path.getPath();
        StringBuilder fullPath = new StringBuilder();
        for (int i = 1; i < nodes.length; i++) { // Skip root
            if (i > 1) {
                fullPath.append(LogCablesTab.getPathSeparator());
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}