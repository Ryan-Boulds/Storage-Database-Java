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

public class RemoveFromStorageAction implements ActionListener {
    private final LogChargersTab tab;
    private static final Logger LOGGER = Logger.getLogger(RemoveFromStorageAction.class.getName());

    public RemoveFromStorageAction(LogChargersTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (tab.isSummaryView()) {
            JOptionPane.showMessageDialog(tab, "Cannot perform this action in summary view", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int selectedRow = tab.getChargerTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a charger to remove from storage");
            JOptionPane.showMessageDialog(null, "Please select a charger to remove from storage", "Error", JOptionPane.ERROR_MESSAGE);
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
            int chargerId = ChargersDAO.getChargerId(chargerType, location);
            if (chargerId == -1) {
                tab.setStatus("Error: Charger type '" + chargerType + "' not found at " + location);
                JOptionPane.showMessageDialog(null, "Charger not found", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ChargersDAO.canRemoveCharger(chargerId)) {
                tab.setStatus("Error: No chargers of type '" + chargerType + "' available to remove at " + location);
                JOptionPane.showMessageDialog(null, "No chargers available to remove", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ChargersDAO.removeCharger(chargerId, 1); // Remove 1 charger
            tab.setStatus("Removed 1 " + chargerType + " from " + location);
            // Refresh the table and restore selection
            tab.refresh();
            if (selectedChargerType != null) {
                JTable table = tab.getChargerTable();
                for (int i = 0; i < table.getRowCount(); i++) {
                    if (selectedChargerType.equals(table.getValueAt(i, 0))) {
                        table.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error removing from storage: {0}", ex.getMessage());
            tab.setStatus("Error removing from storage: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, "Error removing from storage: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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