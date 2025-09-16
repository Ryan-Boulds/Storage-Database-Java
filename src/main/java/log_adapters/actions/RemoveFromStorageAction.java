package log_adapters.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.tree.DefaultMutableTreeNode;

import log_adapters.AdaptersDAO;
import log_adapters.LogAdaptersTab;

public class RemoveFromStorageAction implements ActionListener {
    private final LogAdaptersTab tab;
    private static final Logger LOGGER = Logger.getLogger(RemoveFromStorageAction.class.getName());

    public RemoveFromStorageAction(LogAdaptersTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (tab.isSummaryView()) {
            JOptionPane.showMessageDialog(tab, "Cannot perform this action in summary view", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int selectedRow = tab.getAdapterTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select an adapter to remove from storage");
            JOptionPane.showMessageDialog(null, "Please select an adapter to remove from storage", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String adapterType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        final String selectedAdapterType = adapterType; // Save for reselection, declared final
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
            int adapterId = AdaptersDAO.getAdapterId(adapterType, location);
            if (adapterId == -1) {
                tab.setStatus("Error: Adapter type '" + adapterType + "' not found at " + location);
                JOptionPane.showMessageDialog(null, "Adapter not found", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!AdaptersDAO.canRemoveAdapter(adapterId)) {
                tab.setStatus("Error: No adapters of type '" + adapterType + "' available to remove at " + location);
                JOptionPane.showMessageDialog(null, "No adapters available to remove", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            AdaptersDAO.removeAdapter(adapterId, 1); // Remove 1 adapter
            tab.setStatus("Removed 1 " + adapterType + " from " + location);
            // Refresh the table and restore selection
            tab.refresh();
            if (selectedAdapterType != null) {
                JTable table = tab.getAdapterTable();
                for (int i = 0; i < table.getRowCount(); i++) {
                    if (selectedAdapterType.equals(table.getValueAt(i, 0))) {
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
                fullPath.append(LogAdaptersTab.getPathSeparator());
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}