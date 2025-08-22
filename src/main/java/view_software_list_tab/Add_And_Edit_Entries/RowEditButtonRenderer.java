package view_software_list_tab.Add_And_Edit_Entries;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class RowEditButtonRenderer extends JButton implements TableCellRenderer {
    public RowEditButtonRenderer() {
        setText("Edit");
        setOpaque(true);
        setFocusPainted(false); // Improve button appearance
        setBorderPainted(true); // Ensure button border is visible
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        System.out.println("RowEditButtonRenderer: Rendering button for row " + row + ", column " + column); // Debug
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            setBackground(table.getBackground());
            setForeground(table.getForeground());
        }
        setText("Edit"); // Ensure text is always set
        return this;
    }
}