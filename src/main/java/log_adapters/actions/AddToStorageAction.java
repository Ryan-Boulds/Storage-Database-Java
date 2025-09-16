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

public class AddToStorageAction implements ActionListener {
    private final LogAdaptersTab tab;
    private static final Logger LOGGER = Logger.getLogger(AddToStorageAction.class.getName());

    public AddToStorageAction(LogAdaptersTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedRow = tab.getAdapterTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a adapter to add to storage");
            JOptionPane.showMessageDialog(null, "Please select a adapter to add to storage", "Error", JOptionPane.ERROR_MESSAGE);
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
            AdaptersDAO.addAdapter(adapterType, 1, location); // Add 1 adapter
            tab.setStatus("Added 1 " + adapterType + " to " + location);
            LOGGER.log(Level.INFO, "Added adapter: {0} at {1}", new Object[]{adapterType, location});
            // Refresh the table and restore selection
            tab.refresh();
            if (selectedAdapterType != null) {
                JTable table = tab.getAdapterTable();
                LOGGER.log(Level.INFO, "Attempting to restore selection for adapter type: {0}", selectedAdapterType);
                boolean selectionRestored = false;
                for (int i = 0; i < table.getRowCount(); i++) {
                    Object value = table.getValueAt(i, 0);
                    if (value != null && selectedAdapterType.equals(value.toString())) {
                        table.setRowSelectionInterval(i, i);
                        selectionRestored = true;
                        LOGGER.log(Level.INFO, "Selection restored for adapter type: {0} at row {1}", new Object[]{selectedAdapterType, i});
                        break;
                    }
                }
                if (!selectionRestored) {
                    LOGGER.log(Level.WARNING, "Could not restore selection for adapter type: {0}", selectedAdapterType);
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
                fullPath.append(LogAdaptersTab.getPathSeparator());
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}