package accessories_count.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import accessories_count.AccessoriesDAO;
import accessories_count.LogAccessoriesTab;

public class DeleteAccessoryAction implements ActionListener {
    private final LogAccessoriesTab tab;

    public DeleteAccessoryAction(LogAccessoriesTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedRow = tab.getAccessoryTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select an accessory type first");
            return;
        }
        String accessoryType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tab.getLocationTree().getLastSelectedPathComponent();
        if (node == null) {
            tab.setStatus("Error: Select a location first");
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

        int confirm = JOptionPane.showConfirmDialog(
            tab,
            "Are you sure you want to delete " + accessoryType + " at " + location + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int id = AccessoriesDAO.getAccessoryId(accessoryType, location);
                if (id == -1) {
                    tab.setStatus("Error: Accessory type '" + accessoryType + "' not found at " +
                                  location);
                    return;
                }
                AccessoriesDAO.deleteAccessory(id);
                tab.setStatus("Successfully deleted " + accessoryType + " at " +
                              location);
                tab.refresh();
            } catch (SQLException ex) {
                tab.setStatus("Error deleting accessory: " + ex.getMessage());
                JOptionPane.showMessageDialog(tab, "Error deleting accessory: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
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
                fullPath.append(LogAccessoriesTab.getPathSeparator());
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}