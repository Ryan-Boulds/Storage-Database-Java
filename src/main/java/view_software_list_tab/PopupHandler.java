package view_software_list_tab;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import utils.DatabaseUtils;

public class PopupHandler {
    private static final Logger LOGGER = Logger.getLogger(PopupHandler.class.getName());

    public static void addTablePopup(JTable table, ViewSoftwareListTab viewInventoryTab) {
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

    public static class ButtonRenderer extends JButton implements TableCellRenderer {
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

    public static class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private final JTable table;
        private final TableManager tableManager;
        private String label;
        private boolean isPushed;
        private static final Logger LOGGER = Logger.getLogger(ButtonEditor.class.getName());

        public ButtonEditor(JTable table, TableManager tableManager) {
            super(new JCheckBox());
            this.table = table;
            this.tableManager = tableManager;
            this.button = new JButton();
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
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