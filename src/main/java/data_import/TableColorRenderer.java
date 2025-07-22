package data_import;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import utils.DataEntry;

public class TableColorRenderer extends DefaultTableCellRenderer {
    private final ImportDataTab parent;
    private HashMap<Integer, String> rowStatus; // Maps row index to status (red, yellow, orange, green)

    public TableColorRenderer(ImportDataTab parent) {
        this.parent = parent;
        this.rowStatus = new HashMap<>();
    }

    public void setRowStatus(HashMap<Integer, String> status) {
        this.rowStatus = status;
    }

    public boolean isExactDuplicate(int row) {
        return "red".equals(rowStatus.get(row));
    }

    public boolean isYellowOrOrange(int row) {
        String status = rowStatus.get(row);
        return "yellow".equals(status) || "orange".equals(status);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String status = rowStatus.getOrDefault(row, "green");

        // Set row background color
        switch (status) {
            case "red":
                c.setBackground(Color.RED);
                c.setForeground(Color.BLACK);
                c.setFont(c.getFont().deriveFont(Font.PLAIN)); // No bold for exact duplicates
                break;
            case "yellow":
                c.setBackground(Color.YELLOW);
                c.setForeground(Color.BLACK);
                break;
            case "orange":
                c.setBackground(Color.ORANGE);
                c.setForeground(Color.BLACK);
                break;
            default: // green
                c.setBackground(Color.GREEN);
                c.setForeground(Color.BLACK);
                c.setFont(c.getFont().deriveFont(Font.PLAIN));
                break;
        }

        // Bold conflicting/new cells for yellow/orange rows
        if ("yellow".equals(status) || "orange".equals(status)) {
            List<DataEntry> originalData = parent.getOriginalData();
            if (row < originalData.size()) {
                DataEntry entry = originalData.get(row);
                String assetName = entry.getData().get("AssetName");
                try {
                    HashMap<String, String> existingDevice = parent.getDatabaseHandler().getDeviceByAssetNameFromDB(assetName);
                    if (existingDevice != null) {
                        String dbColumn = parent.getTableColumns()[column];
                        String importedValue = value != null ? value.toString().trim() : "";
                        String dbValue = existingDevice.getOrDefault(dbColumn, "");
                        dbValue = dbValue != null ? dbValue.trim() : "";
                        // Bold if imported value is non-empty and differs from database
                        if (!importedValue.isEmpty() && !importedValue.equals(dbValue)) {
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                        } else {
                            c.setFont(c.getFont().deriveFont(Font.PLAIN));
                        }
                    } else {
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    }
                } catch (SQLException e) {
                    java.util.logging.Logger.getLogger(TableColorRenderer.class.getName()).log(
                        Level.SEVERE, "Error checking database for row {0}, column {1}: {2}",
                        new Object[]{row, column, e.getMessage()});
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }
            }
        } else {
            c.setFont(c.getFont().deriveFont(Font.PLAIN));
        }

        if (isSelected) {
            c.setBackground(table.getSelectionBackground());
            c.setForeground(table.getSelectionForeground());
        }

        return c;
    }
}