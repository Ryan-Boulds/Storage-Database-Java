package log_chargers.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.tree.DefaultMutableTreeNode;

import log_chargers.ChargersDAO;
import log_chargers.LogChargersTab;

public class AddToStorageAction implements ActionListener {
    private final LogChargersTab tab;
    private static final Logger LOGGER = Logger.getLogger(AddToStorageAction.class.getName());

    public AddToStorageAction(LogChargersTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedRow = tab.getChargerTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a charger to add to storage");
            JOptionPane.showMessageDialog(null, "Please select a charger to add to storage", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String chargerType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        final String selectedChargerType = chargerType; // Save for reselection, declared final
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tab.getLocationTree().getLastSelectedPathComponent();
        if (node == null) {
            tab.setStatus("Error: Select a location first");
            JOptionPane.showMessageDialog(null, "Please select a location first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String nodeValue = (String) node.getUserObject();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        String location;
        if (nodeValue.equals("Unassigned") && parent instanceof DefaultMutableTreeNode && !((DefaultMutableTreeNode) parent).isRoot()) {
            location = buildPathFromNode(parent);
        } else {
            location = buildPathFromNode(node);
        }

        try {
            ChargersDAO.addCharger(chargerType, 1, location); // Add 1 charger
            tab.setStatus("Added 1 " + chargerType + " to " + location);
            LOGGER.log(Level.INFO, "Added charger: {0} at {1}", new Object[]{chargerType, location});
            // Refresh the table and restore selection
            tab.refresh();
            if (selectedChargerType != null) {
                JTable table = tab.getChargerTable();
                LOGGER.log(Level.INFO, "Attempting to restore selection for charger type: {0}", selectedChargerType);
                boolean selectionRestored = false;
                for (int i = 0; i < table.getRowCount(); i++) {
                    Object value = table.getValueAt(i, 0);
                    if (value != null && selectedChargerType.equals(value.toString())) {
                        table.setRowSelectionInterval(i, i);
                        selectionRestored = true;
                        LOGGER.log(Level.INFO, "Selection restored for charger type: {0} at row {1}", new Object[]{selectedChargerType, i});
                        break;
                    }
                }
                if (!selectionRestored) {
                    LOGGER.log(Level.WARNING, "Could not restore selection for charger type: {0}", selectedChargerType);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error adding to storage: {0}", ex.getMessage());
            tab.setStatus("Error adding to storage: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, "Error adding to storage: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildPathFromNode(DefaultMutableTreeNode node) {
        if (node == null || node.isRoot()) {
            return null;
        }
        Object userObject = node.getUserObject();
        if (userObject.equals("Unassigned") && node.getParent() instanceof DefaultMutableTreeNode && ((DefaultMutableTreeNode) node.getParent()).isRoot()) {
            return "Unassigned";
        }
        javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(node.getPath());
        Object[] nodes = path.getPath();
        StringBuilder fullPath = new StringBuilder();
        for (int i = 1; i < nodes.length; i++) { // Skip root
            if (i > 1) {
                fullPath.append(LogChargersTab.getPathSeparator());
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}