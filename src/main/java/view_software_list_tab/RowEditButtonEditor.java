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
        button.setFocusPainted(false); // Improve button appearance
        button.setBorderPainted(true); // Ensure button border is visible
        button.addActionListener(e -> {
            System.out.println("RowEditButtonEditor: Edit button clicked for row " + row); // Debug
            fireEditingStopped();
            HashMap<String, String> device = new HashMap<>();
            for (int i = 1; i < table.getColumnCount(); i++) { // Skip Edit column
                String columnName = table.getColumnName(i);
                Object value = table.getValueAt(row, i);
                device.put(columnName, value != null ? value.toString() : "");
            }
            System.out.println("RowEditButtonEditor: Opening ModifyDialog for row " + row + ", device: " + device); // Debug
            ModifyDialog.showModifyDialog((javax.swing.JFrame) SwingUtilities.getWindowAncestor(table), device, tableManager);
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.row = row;
        System.out.println("RowEditButtonEditor: Getting editor component for row " + row + ", column " + column); // Debug
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return "Edit";
    }
}