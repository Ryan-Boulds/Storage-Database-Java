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

public class AddToStorageAction implements ActionListener {
    private final LogCablesTab tab;
    private static final Logger LOGGER = Logger.getLogger(AddToStorageAction.class.getName());

    public AddToStorageAction(LogCablesTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedRow = tab.getCableTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a cable to add to storage");
            JOptionPane.showMessageDialog(null, "Please select a cable to add to storage", "Error", JOptionPane.ERROR_MESSAGE);
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
        String location = node.getUserObject().equals("Unassigned in this location")
                ? buildPathFromNode((DefaultMutableTreeNode) node.getParent())
                : buildPathFromNode(node);

        try {
            int cableId = CablesDAO.getCableId(cableType, location);
            if (cableId == -1) {
                // No existing record, add a new one
                CablesDAO.addCable(cableType, 1, location);
                tab.setStatus("Added 1 " + cableType + " to " + location);
                LOGGER.log(Level.INFO, "Added new cable: {0} at {1}", new Object[]{cableType, location});
            } else {
                // Existing record, increment count
                CablesDAO.updateCount(cableId, 1);
                tab.setStatus("Added 1 " + cableType + " to " + location);
                LOGGER.log(Level.INFO, "Incremented cable count: {0} at {1}", new Object[]{cableType, location});
            }
            // Refresh the table and restore selection
            tab.refresh();
            if (selectedCableType != null) {
                JTable table = tab.getCableTable();
                LOGGER.log(Level.INFO, "Attempting to restore selection for cable type: {0}", selectedCableType);
                boolean selectionRestored = false;
                for (int i = 0; i < table.getRowCount(); i++) {
                    Object value = table.getValueAt(i, 0);
                    if (value != null && selectedCableType.equals(value.toString())) {
                        table.setRowSelectionInterval(i, i);
                        selectionRestored = true;
                        LOGGER.log(Level.INFO, "Selection restored for cable type: {0} at row {1}", new Object[]{selectedCableType, i});
                        break;
                    }
                }
                if (!selectionRestored) {
                    LOGGER.log(Level.WARNING, "Could not restore selection for cable type: {0}", selectedCableType);
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
        if (node.getUserObject().equals("Unassigned")) {
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