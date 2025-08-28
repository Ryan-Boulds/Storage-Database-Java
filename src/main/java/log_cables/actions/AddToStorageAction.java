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

public class AddToStorageAction implements ActionListener {
    private final LogCablesTab tab;

    public AddToStorageAction(LogCablesTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedRow = tab.getCableTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a cable to add to storage");
            return;
        }

        String cableType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tab.getLocationTree().getLastSelectedPathComponent();
        if (node == null) {
            tab.setStatus("Error: Select a location first");
            return;
        }
        String location = node.getUserObject().equals("Unassigned in this location")
                ? buildPathFromNode((DefaultMutableTreeNode) node.getParent())
                : buildPathFromNode(node);
        final String parentLocation = node.getUserObject().equals("Unassigned in this location")
                ? location
                : (node.getParent() != null && !((DefaultMutableTreeNode) node.getParent()).isRoot()
                        ? buildPathFromNode((DefaultMutableTreeNode) node.getParent())
                        : null);

        JDialog dialog = new JDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, tab), "Add to Storage", true);
        dialog.setSize(300, 150);
        dialog.setLayout(new java.awt.BorderLayout());
        dialog.setLocationRelativeTo(tab);

        JPanel inputPanel = new JPanel(new java.awt.BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel label = new JLabel("Enter quantity to add:");
        JTextField quantityField = UIComponentUtils.createFormattedTextField();
        inputPanel.add(label, java.awt.BorderLayout.NORTH);
        inputPanel.add(quantityField, java.awt.BorderLayout.CENTER);

        JButton addButton = UIComponentUtils.createFormattedButton("Add");
        addButton.addActionListener(e1 -> {
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
            try {
                int cableId = CablesDAO.getCableId(cableType, location);
                if (cableId == -1) {
                    CablesDAO.addCable(cableType, quantity, location, parentLocation);
                    tab.setStatus("Successfully added " + quantity + " " + cableType + " to " + location);
                } else {
                    CablesDAO.updateCount(cableId, quantity);
                    tab.setStatus("Successfully added " + quantity + " to existing " + cableType + " at " + location);
                }
                tab.refresh();
                dialog.dispose();
            } catch (SQLException ex) {
                tab.setStatus("Error adding to storage: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "Error adding to storage: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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