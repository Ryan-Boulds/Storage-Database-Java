package view_software_list_tab;

import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class PopupHandler {
    private static final String PRIMARY_KEY_COLUMN = "AssetName";

    public static void addTablePopup(JTable table, ViewSoftwareListTab viewInventoryTab, TableManager tableManager) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem detailsItem = new JMenuItem("View Details");
        JMenuItem modifyItem = new JMenuItem("Modify/Rename");

        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                detailsItem.setEnabled(table.getSelectedRowCount() == 1);
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        detailsItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int primaryKeyColumnIndex = -1;
                for (int i = 0; i < table.getColumnCount(); i++) {
                    if (PRIMARY_KEY_COLUMN.equals(table.getColumnName(i))) {
                        primaryKeyColumnIndex = i;
                        break;
                    }
                }
                if (primaryKeyColumnIndex == -1) {
                    JOptionPane.showMessageDialog(table, "Error: " + PRIMARY_KEY_COLUMN + " column not found", "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("PopupHandler: " + PRIMARY_KEY_COLUMN + " column not found");
                    return;
                }
                Object primaryKeyValue = table.getValueAt(selectedRow, primaryKeyColumnIndex);
                if (primaryKeyValue == null || primaryKeyValue.toString().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(table, "Error: " + PRIMARY_KEY_COLUMN + " is missing for selected row", "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("PopupHandler: Missing " + PRIMARY_KEY_COLUMN + " for row " + (selectedRow + 1));
                    return;
                }
                String assetName = primaryKeyValue.toString().trim();
                viewInventoryTab.showDeviceDetails(assetName);
            } else {
                JOptionPane.showMessageDialog(table, "Please select a row first", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        modifyItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            int selectedColumn = table.getSelectedColumn();
            if (selectedRow >= 0 && selectedColumn >= 1) {
                String columnName = table.getColumnName(selectedColumn);
                ArrayList<String> tableColumns = new ArrayList<>();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    tableColumns.add(table.getColumnName(i));
                }
                System.out.println("PopupHandler: Table columns: " + tableColumns);
                int primaryKeyColumnIndex = -1;
                for (int i = 0; i < table.getColumnCount(); i++) {
                    if (PRIMARY_KEY_COLUMN.equals(table.getColumnName(i))) {
                        primaryKeyColumnIndex = i;
                        break;
                    }
                }
                if (primaryKeyColumnIndex == -1) {
                    JOptionPane.showMessageDialog(table, "Error: " + PRIMARY_KEY_COLUMN + " column not found in table", "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("PopupHandler: " + PRIMARY_KEY_COLUMN + " column not found in table columns: " + tableColumns);
                    return;
                }
                int[] selectedRows = table.getSelectedRows();
                if (selectedRows.length > 1) {
                    ArrayList<String> cellValues = new ArrayList<>();
                    ArrayList<String> primaryKeys = new ArrayList<>();
                    for (int row : selectedRows) {
                        Object cellValue = table.getValueAt(row, selectedColumn);
                        cellValues.add(cellValue != null ? cellValue.toString().trim() : "");
                        Object primaryKeyValue = table.getValueAt(row, primaryKeyColumnIndex);
                        if (primaryKeyValue == null || primaryKeyValue.toString().trim().isEmpty()) {
                            JOptionPane.showMessageDialog(table, "Error: " + PRIMARY_KEY_COLUMN + " is missing for row " + (row + 1), "Error", JOptionPane.ERROR_MESSAGE);
                            System.err.println("PopupHandler: Missing " + PRIMARY_KEY_COLUMN + " for row " + (row + 1) + ": " + primaryKeyValue);
                            return;
                        }
                        String primaryKey = primaryKeyValue.toString().trim();
                        primaryKeys.add(primaryKey);
                    }
                    System.out.println("PopupHandler: Opening MultiRenameDialog for column=" + columnName + ", primaryKeys=" + primaryKeys + ", cellValues=" + cellValues);
                    MultiRenameDialog.showRenameDialog(
                        (javax.swing.JFrame) SwingUtilities.getWindowAncestor(table),
                        table,
                        cellValues,
                        columnName,
                        primaryKeys,
                        tableManager
                    );
                } else {
                    Object cellValue = table.getValueAt(selectedRow, selectedColumn);
                    Object primaryKeyValue = table.getValueAt(selectedRow, primaryKeyColumnIndex);
                    if (primaryKeyValue == null || primaryKeyValue.toString().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(table, "Error: " + PRIMARY_KEY_COLUMN + " is missing for selected row", "Error", JOptionPane.ERROR_MESSAGE);
                        System.err.println("PopupHandler: Missing " + PRIMARY_KEY_COLUMN + " for row " + (selectedRow + 1) + ": " + primaryKeyValue);
                        return;
                    }
                    String primaryKey = primaryKeyValue.toString().trim();
                    System.out.println("PopupHandler: Opening SingleRenameDialog for column=" + columnName + ", primaryKey='" + primaryKey + "', cellValue=" + cellValue);
                    SingleRenameDialog.showRenameDialog(
                        (javax.swing.JFrame) SwingUtilities.getWindowAncestor(table),
                        table,
                        cellValue != null ? cellValue.toString().trim() : "",
                        columnName,
                        primaryKey,
                        tableManager
                    );
                }
            } else {
                JOptionPane.showMessageDialog(table, "Please select a cell first", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        popupMenu.add(detailsItem);
        popupMenu.add(modifyItem);
        table.setComponentPopupMenu(popupMenu);
    }
}