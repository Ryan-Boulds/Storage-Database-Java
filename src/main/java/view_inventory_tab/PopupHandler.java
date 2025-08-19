package view_inventory_tab;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class PopupHandler {
    private static final Logger LOGGER = Logger.getLogger(PopupHandler.class.getName());

    public static void addTablePopup(JTable table, ViewInventoryTab viewInventoryTab) {
        JPopupMenu popupMenu = new JPopupMenu();
        TableManager tableManager = viewInventoryTab.getTableManager();

        JMenuItem addRowItem = new JMenuItem("Add Row");
        addRowItem.addActionListener(e -> {
            String tableName = tableManager.getTableName();
            if (tableName != null && !tableName.isEmpty()) {
                AddRowEntry.showAddDialog((javax.swing.JFrame) SwingUtilities.getWindowAncestor(table), tableManager);
                LOGGER.log(Level.INFO, "Opened AddRowEntry dialog for table '{0}'", tableName);
            } else {
                JOptionPane.showMessageDialog(table, "Please select a valid table first", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, "Attempted to add row without selecting a valid table");
            }
        });
        popupMenu.add(addRowItem);

        JMenuItem deleteRowItem = new JMenuItem("Delete Row");
        deleteRowItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(table, "No rows selected", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(
                table,
                "Are you sure you want to delete the selected row(s)?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                try (java.sql.Connection conn = utils.DatabaseUtils.getConnection()) {
                    String tableName = tableManager.getTableName();
                    String sql = "DELETE FROM [" + tableName + "] WHERE [AssetName] = ?";
                    try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                        for (int row : selectedRows) {
                            String assetName = (String) table.getValueAt(row, table.getColumn("AssetName").getModelIndex());
                            ps.setString(1, assetName);
                            ps.executeUpdate();
                        }
                    }
                    tableManager.refreshDataAndTabs();
                    JOptionPane.showMessageDialog(table, "Row(s) deleted successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (java.sql.SQLException ex) {
                    JOptionPane.showMessageDialog(table, "Error deleting row(s): " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.log(Level.SEVERE, "SQLException deleting rows: {0}", ex.getMessage());
                }
            }
        });
        popupMenu.add(deleteRowItem);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && !table.isRowSelected(row)) {
                    table.setRowSelectionInterval(row, row);
                }
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }
}