package log_cables.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import log_cables.CablesDAO;
import log_cables.LogCablesTab;

public class DeleteCableAction implements ActionListener {
    private final LogCablesTab tab;

    public DeleteCableAction(LogCablesTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedRow = tab.getCableTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a cable type first");
            return;
        }
        String cableType = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tab.getLocationTree().getLastSelectedPathComponent();
        if (node == null) {
            tab.setStatus("Error: Select a location first");
            return;
        }
        String location = (String) node.getUserObject();

        int confirm = JOptionPane.showConfirmDialog(
            tab,
            "Are you sure you want to delete " + cableType + " at " + location + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int id = CablesDAO.getCableId(cableType, location);
                if (id == -1) {
                    tab.setStatus("Error: Cable type '" + cableType + "' not found at " + 
                                  (location.equals(tab.getUnassignedLocation()) ? tab.getUnassignedLocation() : location));
                    return;
                }
                CablesDAO.deleteCable(id);
                tab.setStatus("Successfully deleted " + cableType + " at " + 
                              (location.equals(tab.getUnassignedLocation()) ? tab.getUnassignedLocation() : location));
                tab.refreshTable(location);
            } catch (SQLException ex) {
                tab.setStatus("Error deleting cable: " + ex.getMessage());
                JOptionPane.showMessageDialog(tab, "Error deleting cable: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}