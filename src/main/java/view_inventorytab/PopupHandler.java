package view_inventorytab;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import utils.DefaultColumns;

public class PopupHandler {
    public static void showDetailsPopup(JFrame parent, HashMap<String, String> device) {
        JDialog dialog = new JDialog(parent, "Device Details", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 300);

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));

        for (String key : DefaultColumns.getInventoryColumns()) {
            String value = device.getOrDefault(key, "");
            if (!value.isEmpty()) {
                JLabel label = new JLabel(key + ": " + value);
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                detailsPanel.add(label);
            }
        }

        JButton okayButton = new JButton("Okay");
        okayButton.addActionListener(e -> dialog.dispose());

        JScrollPane scrollPane = new JScrollPane(detailsPanel);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(okayButton, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public static void addTablePopup(JTable table, JTabbedPane tabbedPane, TableManager tableManager) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem detailsItem = new JMenuItem("View Details");
        JMenuItem modifyItem = new JMenuItem("Modify/Rename");

        // Enable/disable View Details based on selection count
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
                HashMap<String, String> device = getDeviceData(table, selectedRow);
                showDetailsPopup((JFrame) SwingUtilities.getWindowAncestor(table), device);
            } else {
                JOptionPane.showMessageDialog(table, "Please select a row first", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        modifyItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            int selectedColumn = table.getSelectedColumn();
            if (selectedRow >= 0 && selectedColumn >= 1) { // Skip Edit column
                String columnName = table.getColumnName(selectedColumn);
                // Log table columns for debugging
                ArrayList<String> tableColumns = new ArrayList<>();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    tableColumns.add(table.getColumnName(i));
                }
                System.out.println("PopupHandler: Table columns: " + tableColumns); // Debug
                // Find AssetName column index using table column names
                int assetNameColumnIndex = -1;
                for (int i = 0; i < table.getColumnCount(); i++) {
                    if ("AssetName".equals(table.getColumnName(i))) {
                        assetNameColumnIndex = i;
                        break;
                    }
                }
                if (assetNameColumnIndex == -1) {
                    JOptionPane.showMessageDialog(table, "Error: AssetName column not found in table", "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("PopupHandler: AssetName column not found in table columns: " + tableColumns); // Debug
                    return;
                }
                int[] selectedRows = table.getSelectedRows();
                if (selectedRows.length > 1) {
                    ArrayList<String> cellValues = new ArrayList<>();
                    ArrayList<String> assetNames = new ArrayList<>();
                    for (int row : selectedRows) {
                        Object cellValue = table.getValueAt(row, selectedColumn);
                        cellValues.add(cellValue != null ? cellValue.toString().trim() : "");
                        Object assetNameValue = table.getValueAt(row, assetNameColumnIndex);
                        if (assetNameValue == null || assetNameValue.toString().trim().isEmpty()) {
                            JOptionPane.showMessageDialog(table, "Error: AssetName is missing for row " + (row + 1), "Error", JOptionPane.ERROR_MESSAGE);
                            System.err.println("PopupHandler: Missing AssetName for row " + (row + 1) + ": " + assetNameValue); // Debug
                            return;
                        }
                        String assetName = assetNameValue.toString().trim();
                        assetNames.add(assetName);
                    }
                    System.out.println("PopupHandler: Opening MultiRenameDialog for column=" + columnName + ", assetNames=" + assetNames + ", cellValues=" + cellValues); // Debug
                    MultiRenameDialog.showRenameDialog(
                        (JFrame) SwingUtilities.getWindowAncestor(table),
                        table,
                        cellValues,
                        columnName,
                        assetNames,
                        tableManager
                    );
                } else {
                    Object cellValue = table.getValueAt(selectedRow, selectedColumn);
                    Object assetNameValue = table.getValueAt(selectedRow, assetNameColumnIndex);
                    if (assetNameValue == null || assetNameValue.toString().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(table, "Error: AssetName is missing for selected row", "Error", JOptionPane.ERROR_MESSAGE);
                        System.err.println("PopupHandler: Missing AssetName for row " + (selectedRow + 1) + ": " + assetNameValue); // Debug
                        return;
                    }
                    String assetName = assetNameValue.toString().trim();
                    System.out.println("PopupHandler: Opening SingleRenameDialog for column=" + columnName + ", assetName='" + assetName + "', cellValue=" + cellValue); // Debug
                    SingleRenameDialog.showRenameDialog(
                        (JFrame) SwingUtilities.getWindowAncestor(table),
                        table,
                        cellValue != null ? cellValue.toString().trim() : "",
                        columnName,
                        assetName,
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

    private static HashMap<String, String> getDeviceData(JTable table, int row) {
        HashMap<String, String> device = new HashMap<>();
        for (int i = 1; i < table.getColumnCount(); i++) { // Skip Edit column
            String columnName = table.getColumnName(i);
            Object value = table.getValueAt(row, i);
            device.put(columnName, value != null ? value.toString().trim() : "");
        }
        return device;
    }
}