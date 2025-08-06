package view_software_list_tab;

import java.awt.Component;
import java.util.HashMap;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

public class RowEditButtonEditor extends AbstractCellEditor implements TableCellEditor {
    private final JButton button;
    private int row;

    public RowEditButtonEditor(JTable table, TableManager tableManager) {
        button = new JButton("Edit");
        button.addActionListener(e -> {
            fireEditingStopped();
            HashMap<String, String> device = new HashMap<>();
            for (int i = 1; i < table.getColumnCount(); i++) { // Skip Edit column
                String columnName = table.getColumnName(i);
                Object value = table.getValueAt(row, i);
                device.put(columnName, value != null ? value.toString() : "");
            }
            ModifyDialog.showModifyDialog((javax.swing.JFrame) SwingUtilities.getWindowAncestor(table), device, tableManager);
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.row = row;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return "Edit";
    }
}