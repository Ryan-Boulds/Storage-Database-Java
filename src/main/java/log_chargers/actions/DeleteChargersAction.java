package log_chargers.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import log_chargers.ChargersDAO;
import log_chargers.LogChargersTab;

public class DeleteChargersAction implements ActionListener {
    private final LogChargersTab tab;

    public DeleteChargersAction(LogChargersTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedRow = tab.getChargerTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a charger type first");
            return;
        }
        String chargerType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
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
            "Are you sure you want to delete " + chargerType + " at " + location + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int id = ChargersDAO.getChargerId(chargerType, location);
                if (id == -1) {
                    tab.setStatus("Error: Charger type '" + chargerType + "' not found at " +
                                  location);
                    return;
                }
                ChargersDAO.deleteCharger(id);
                tab.setStatus("Successfully deleted " + chargerType + " at " +
                              location);
                tab.refresh();
            } catch (SQLException ex) {
                tab.setStatus("Error deleting charger: " + ex.getMessage());
                JOptionPane.showMessageDialog(tab, "Error deleting charger: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                fullPath.append(LogChargersTab.getPathSeparator());
            }
            fullPath.append(nodes[i].toString());
        }
        return fullPath.toString();
    }
}