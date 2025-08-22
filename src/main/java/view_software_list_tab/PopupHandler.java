package view_software_list_tab;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import utils.DatabaseUtils;
import view_software_list_tab.Add_And_Edit_Entries.AddRowEntry;
import view_software_list_tab.Add_And_Edit_Entries.ModifyRowEntry;
import view_software_list_tab.Add_And_Edit_Entries.MultiRenameDialog;
import view_software_list_tab.Add_And_Edit_Entries.SingleRenameDialog;

public class PopupHandler {
    private static final Logger LOGGER = Logger.getLogger(PopupHandler.class.getName());

    public static void addTablePopup(JTable table, ViewSoftwareListTab viewInventoryTab) {
        JPopupMenu popupMenu = new JPopupMenu();
        TableManager tableManager = viewInventoryTab.getTableManager();

        // Add Row menu item
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

        // Delete Row menu item
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
                try (Connection conn = DatabaseUtils.getConnection()) {
                    String tableName = tableManager.getTableName();
                    String sql = "DELETE FROM [" + tableName + "] WHERE [AssetName] = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        for (int row : selectedRows) {
                            String assetName = (String) table.getValueAt(row, table.getColumn("AssetName").getModelIndex());
                            ps.setString(1, assetName);
                            ps.executeUpdate();
                        }
                    }
                    tableManager.refreshDataAndTabs();
                    JOptionPane.showMessageDialog(table, "Row(s) deleted successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(table, "Error deleting row(s): " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.log(Level.SEVERE, "SQLException deleting rows: {0}", ex.getMessage());
                }
            }
        });
        popupMenu.add(deleteRowItem);

        // Rename Cell menu item
        JMenuItem renameCellItem = new JMenuItem("Rename Cell");
        renameCellItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            int selectedColumn = table.getSelectedColumn();
            if (selectedRows.length == 0 || selectedColumn == -1) {
                JOptionPane.showMessageDialog(table, "Please select one or more cells to rename", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, "Attempted to rename cell without selecting a cell");
                return;
            }
            String tableName = tableManager.getTableName();
            if ("Inventory".equals(tableName)) {
                JOptionPane.showMessageDialog(table, "Error: Editing is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.severe("Attempted to rename cell in Inventory table, which is not allowed");
                return;
            }
            String columnName = table.getColumnName(selectedColumn);
            if (selectedColumn == 0 || columnName.equals("Edit")) {
                JOptionPane.showMessageDialog(table, "Cannot rename the Edit column", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, "Attempted to rename Edit column");
                return;
            }
            ArrayList<String> cellValues = new ArrayList<>();
            ArrayList<String> assetNames = new ArrayList<>();
            for (int row : selectedRows) {
                String cellValue = table.getValueAt(row, selectedColumn) != null ? 
                    table.getValueAt(row, selectedColumn).toString() : "";
                String assetName = (String) table.getValueAt(row, table.getColumn("AssetName").getModelIndex());
                cellValues.add(cellValue);
                assetNames.add(assetName);
            }
            if (selectedRows.length == 1) {
                SingleRenameDialog.showRenameDialog(
                    (javax.swing.JFrame) SwingUtilities.getWindowAncestor(table),
                    table,
                    cellValues.get(0),
                    columnName,
                    assetNames.get(0),
                    tableManager
                );
                LOGGER.log(Level.INFO, "Opened SingleRenameDialog for table '{0}', AssetName='{1}', column='{2}'", 
                    new Object[]{tableName, assetNames.get(0), columnName});
            } else {
                MultiRenameDialog.showRenameDialog(
                    (javax.swing.JFrame) SwingUtilities.getWindowAncestor(table),
                    table,
                    cellValues,
                    columnName,
                    assetNames,
                    tableManager
                );
                LOGGER.log(Level.INFO, "Opened MultiRenameDialog for table '{0}', column='{1}', assetNames={2}", 
                    new Object[]{tableName, columnName, assetNames});
            }
        });
        popupMenu.add(renameCellItem);

        // Edit Row menu item
        JMenuItem editRowItem = new JMenuItem("Edit Row");
        editRowItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(table, "Please select a row to edit", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, "Attempted to edit row without selecting a row");
                return;
            }
            String tableName = tableManager.getTableName();
            if ("Inventory".equals(tableName)) {
                JOptionPane.showMessageDialog(table, "Error: Editing is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.severe("Attempted to edit row in Inventory table, which is not allowed");
                return;
            }
            String assetName = (String) table.getValueAt(selectedRow, table.getColumn("AssetName").getModelIndex());
            HashMap<String, String> device = new HashMap<>();
            for (int i = 1; i < table.getColumnCount(); i++) { // Skip Edit column
                String columnName = table.getColumnName(i);
                Object value = table.getValueAt(selectedRow, i);
                device.put(columnName, value != null ? value.toString() : "");
            }
            ModifyRowEntry.showModifyDialog(
                (javax.swing.JFrame) SwingUtilities.getWindowAncestor(table),
                device,
                tableManager
            );
            LOGGER.log(Level.INFO, "Opened ModifyRowEntry dialog for table '{0}', AssetName='{1}'", 
                new Object[]{tableName, assetName});
        });
        popupMenu.add(editRowItem);

        // Device Details menu item
        JMenuItem deviceDetailsItem = new JMenuItem("Device Details");
        deviceDetailsItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(table, "Please select a row to view details", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, "Attempted to view device details without selecting a row");
                return;
            }
            String tableName = tableManager.getTableName();
            String assetName = (String) table.getValueAt(selectedRow, table.getColumn("AssetName").getModelIndex());
            viewInventoryTab.showDeviceDetails(assetName);
            LOGGER.log(Level.INFO, "Opened DeviceDetailsPanel for table '{0}', AssetName='{1}'", 
                new Object[]{tableName, assetName});
        });
        popupMenu.add(deviceDetailsItem);

        // Mouse listener to show popup on right-click
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            private void handlePopup(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                if (row >= 0 && row < table.getRowCount() && column >= 0 && column < table.getColumnCount()) {
                    if (!table.isRowSelected(row)) {
                        table.setRowSelectionInterval(row, row);
                    }
                    table.setColumnSelectionInterval(column, column);
                }
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    public static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setText("Edit");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(UIManager.getColor("Button.background"));
            }
            return this;
        }
    }

    public static class ButtonEditor extends javax.swing.DefaultCellEditor {
        private final JButton button;
        private final JTable table;
        private final TableManager tableManager;
        private String label;
        private boolean isPushed;
        private static final Logger LOGGER = Logger.getLogger(ButtonEditor.class.getName());

        public ButtonEditor(JTable table, TableManager tableManager) {
            super(new javax.swing.JCheckBox());
            this.table = table;
            this.tableManager = tableManager;
            this.button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(UIManager.getColor("Button.background"));
            }
            label = (value == null) ? "Edit" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int modelRow = table.convertRowIndexToModel(table.getEditingRow());
                String tableName = tableManager.getTableName();
                String[] columns = tableManager.getColumns();
                Object[] rowData = new Object[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    rowData[i] = table.getModel().getValueAt(modelRow, i + 1); // Skip "Edit" column
                }

                JTextField[] fields = new JTextField[columns.length];
                JPanel panel = new JPanel(new java.awt.GridLayout(columns.length, 2));
                for (int i = 0; i < columns.length; i++) {
                    panel.add(new JLabel(columns[i] + ":"));
                    fields[i] = new JTextField(rowData[i] != null ? rowData[i].toString() : "");
                    panel.add(fields[i]);
                }

                int result = JOptionPane.showConfirmDialog(button, panel, "Edit Row", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    try (Connection conn = DatabaseUtils.getConnection()) {
                        if (conn == null) {
                            LOGGER.log(Level.SEVERE, "Failed to establish database connection");
                            JOptionPane.showMessageDialog(button, "Failed to connect to the database", "Database Error", JOptionPane.ERROR_MESSAGE);
                            return label;
                        }
                        StringBuilder updateSql = new StringBuilder("UPDATE [" + tableName + "] SET ");
                        for (int i = 0; i < columns.length; i++) {
                            updateSql.append("[").append(columns[i]).append("] = ?");
                            if (i < columns.length - 1) updateSql.append(", ");
                        }
                        updateSql.append(" WHERE ");
                        for (int i = 0; i < columns.length; i++) {
                            updateSql.append("[").append(columns[i]).append("] = ?");
                            if (i < columns.length - 1) updateSql.append(" AND ");
                        }
                        try (PreparedStatement pstmt = conn.prepareStatement(updateSql.toString())) {
                            for (int i = 0; i < columns.length; i++) {
                                pstmt.setString(i + 1, fields[i].getText());
                            }
                            for (int i = 0; i < columns.length; i++) {
                                pstmt.setString(columns.length + i + 1, rowData[i] != null ? rowData[i].toString() : null);
                            }
                            pstmt.executeUpdate();
                            LOGGER.log(Level.INFO, "Updated row in table '{0}'", tableName);
                            tableManager.refreshDataAndTabs();
                        }
                    } catch (SQLException e) {
                        LOGGER.log(Level.SEVERE, "Error updating row in table '{0}': {1}", new Object[]{tableName, e.getMessage()});
                        JOptionPane.showMessageDialog(button, String.format("Error updating row: %s", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        @Override
        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }
}