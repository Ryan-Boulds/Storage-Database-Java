package log_cables.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.tree.DefaultMutableTreeNode;

import log_cables.CablesDAO;
import log_cables.LogCablesTab;

public class AddToStorageAction implements ActionListener {
    private final LogCablesTab tab;

    public AddToStorageAction(LogCablesTab tab) {
        this.tab = tab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedRow = tab.getCableTable().getSelectedRow();
        if (selectedRow == -1) {
            tab.setStatus("Error: Select a cable type first");
            return;
        }
        String type = (String) tab.getTableModel().getValueAt(selectedRow, 0);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tab.getLocationTree().getLastSelectedPathComponent();
        if (node == null) {
            tab.setStatus("Error: Select a location first");
            return;
        }
        String location = (String) node.getUserObject();
        try {
            int id = CablesDAO.getCableId(type, location);
            if (id == -1) {
                tab.setStatus("Error: Cable type '" + type + "' not found at " + 
                              (location.equals(tab.getUnassignedLocation()) ? tab.getUnassignedLocation() : location));
                return;
            }
            CablesDAO.updateCount(id, 1);
            tab.setStatus("Successfully added 1 to " + type + " at " + 
                          (location.equals(tab.getUnassignedLocation()) ? tab.getUnassignedLocation() : location));
            tab.refreshTable(location);
        } catch (SQLException ex) {
            tab.setStatus("Error: " + ex.getMessage());
        }
    }
}