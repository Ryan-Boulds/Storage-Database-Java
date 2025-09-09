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
        String fullPath = tab.buildPathFromNode(node);
        if (fullPath == null) {
            tab.setStatus("Error: Invalid location selected");
            return;
        }
        fullPath = fullPath.replace(LogCablesTab.DISPLAY_SEPARATOR, LogCablesTab.getPathSeparator());
        if (fullPath.equals(LogCablesTab.getUnassignedLocation()) || fullPath.endsWith(LogCablesTab.getPathSeparator() + LogCablesTab.getUnassignedLocation())) {
            tab.setStatus("Error: Cannot delete the Unassigned location");
            return;
        }
        if (node.getChildCount() > 0) {
            tab.setStatus("Error: Cannot delete location with sublocations");
            JOptionPane.showMessageDialog(tab, "Cannot delete location that has sublocations", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            tab,
            "Are you sure you want to delete location " + fullPath.replace(LogCablesTab.getPathSeparator(), LogCablesTab.DISPLAY_SEPARATOR) + "? All cables will be moved to parent or Unassigned.",
            "Confirm Delete Location",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                CablesDAO.deleteLocation(fullPath);
                tab.setStatus("Successfully deleted location " + fullPath.replace(LogCablesTab.getPathSeparator(), LogCablesTab.DISPLAY_SEPARATOR) + " and moved cables to parent or Unassigned");
                tab.refreshTree();
                tab.refresh();
            } catch (SQLException ex) {
                tab.setStatus("Error deleting location: " + ex.getMessage());
                JOptionPane.showMessageDialog(tab, "Error deleting location: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}