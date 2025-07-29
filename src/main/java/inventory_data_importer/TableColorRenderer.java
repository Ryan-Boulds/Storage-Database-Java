package inventory_data_importer;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class TableColorRenderer extends DefaultTableCellRenderer {
    private HashMap<Integer, String> rowStatus;

    public TableColorRenderer(ImportDataTab parent) {
        this.rowStatus = new HashMap<>();
    }

    public void setRowStatus(HashMap<Integer, String> rowStatus) {
        this.rowStatus = rowStatus != null ? new HashMap<>(rowStatus) : new HashMap<>();
    }

    public boolean isYellowOrOrange(int row) {
        String status = rowStatus.getOrDefault(row, "");
        return status.equals("yellow");
    }

    public boolean isExactDuplicate(int row) {
        return rowStatus.getOrDefault(row, "").equals("red");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                  boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String status = rowStatus.getOrDefault(row, "");

        if (isSelected) {
            c.setBackground(table.getSelectionBackground());
            c.setForeground(table.getSelectionForeground());
        } else {
            switch (status) {
                case "green":
                    c.setBackground(new Color(144, 238, 144)); // Light green for resolved or overwrite
                    c.setForeground(Color.BLACK);
                    break;
                case "yellow":
                    c.setBackground(Color.YELLOW); // Yellow for conflicts
                    c.setForeground(Color.BLACK);
                    break;
                case "red":
                    c.setBackground(Color.RED); // Red for exact duplicates
                    c.setForeground(Color.WHITE);
                    break;
                case "white":
                default:
                    c.setBackground(table.getBackground()); // White (default) for new entries
                    c.setForeground(table.getForeground());
                    break;
            }
        }
        return c;
    }
}