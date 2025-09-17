package accessories_count.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import accessories_count.AccessoriesDAO;
import accessories_count.LogAccessoriesTab;

public class DeleteLocationAction implements ActionListener {
    private final LogAccessoriesTab tab;

    public DeleteLocationAction(LogAccessoriesTab tab) {
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
        fullPath = fullPath.replace(LogAccessoriesTab.DISPLAY_SEPARATOR, LogAccessoriesTab.getPathSeparator());
        if (fullPath.equals(LogAccessoriesTab.getUnassignedLocation()) || fullPath.endsWith(LogAccessoriesTab.getPathSeparator() + LogAccessoriesTab.getUnassignedLocation())) {
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
            "Are you sure you want to delete location " + fullPath.replace(LogAccessoriesTab.getPathSeparator(), LogAccessoriesTab.DISPLAY_SEPARATOR) + "? All accessories will be moved to parent or Unassigned.",
            "Confirm Delete Location",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                AccessoriesDAO.deleteLocation(fullPath);
                tab.setStatus("Successfully deleted location " + fullPath.replace(LogAccessoriesTab.getPathSeparator(), LogAccessoriesTab.DISPLAY_SEPARATOR) + " and moved accessories to parent or Unassigned");
                tab.refreshTree();
                tab.refresh();
            } catch (SQLException ex) {
                tab.setStatus("Error deleting location: " + ex.getMessage());
                JOptionPane.showMessageDialog(tab, "Error deleting location: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}