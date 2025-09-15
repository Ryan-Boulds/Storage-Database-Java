package log_cables.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.tree.DefaultMutableTreeNode;

import log_cables.CablesDAO;
import log_cables.LogCablesTab;

public class RemoveFromStorageAction implements ActionListener {
    private final LogCablesTab tab;
    private static final Logger LOGGER = Logger.getLogger(RemoveFromStorageAction.class.getName());

    public RemoveFromStorageAction(LogCablesTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (tab.isSummaryView()) {
            JOptionPane.showMessageDialog(tab, "Cannot perform this action in summary view", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int selectedRow = tab.getCableTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a cable to remove from storage");
            JOptionPane.showMessageDialog(null, "Please select a cable to remove from storage", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String cableType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        final String selectedCableType = cableType; // Save for reselection, declared final
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
            int cableId = CablesDAO.getCableId(cableType, location);
            if (cableId == -1) {
                tab.setStatus("Error: Cable type '" + cableType + "' not found at " + location);
                JOptionPane.showMessageDialog(null, "Cable not found", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!CablesDAO.canRemoveCable(cableId)) {
                tab.setStatus("Error: No cables of type '" + cableType + "' available to remove at " + location);
                JOptionPane.showMessageDialog(null, "No cables available to remove", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            CablesDAO.removeCable(cableId, 1); // Remove 1 cable
            tab.setStatus("Removed 1 " + cableType + " from " + location);
            // Refresh the table and restore selection
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
                fullPath.append(LogCablesTab.getPathSeparator());
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}