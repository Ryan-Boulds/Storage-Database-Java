package log_cables.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import log_cables.CablesDAO;
import log_cables.LogCablesTab;

public class DeleteLocationAction implements ActionListener {
    private final LogCablesTab tab;

    public DeleteLocationAction(LogCablesTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tab.getLocationTree().getLastSelectedPathComponent();
        if (node == null) {
            tab.setStatus("Error: Select a location first");
            return;
        }
        String location = (String) node.getUserObject();
        if (location.equals(tab.getUnassignedLocation())) {
            tab.setStatus("Error: Cannot delete the Unassigned location");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            tab,
            "Are you sure you want to delete location " + location + "? All cables will be moved to Unassigned.",
            "Confirm Delete Location",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                CablesDAO.deleteLocation(location);
                tab.setStatus("Successfully deleted location " + location + " and moved cables to Unassigned");
                tab.refreshTree();
                tab.refreshTable(tab.getUnassignedLocation(), false);
            } catch (SQLException ex) {
                tab.setStatus("Error deleting location: " + ex.getMessage());
                JOptionPane.showMessageDialog(tab, "Error deleting location: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}