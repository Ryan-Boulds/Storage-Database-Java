package data_import;

import java.awt.Color;
import java.awt.Component;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class TableColorRenderer extends DefaultTableCellRenderer {
    private final ImportDataTab tab;

    public TableColorRenderer(ImportDataTab tab) {
        this.tab = tab;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (!tab.isShowDuplicates() && isDuplicateRow(row)) {
            c.setBackground(table.getBackground());
            c.setForeground(table.getForeground());
        } else {
            HashMap<String, String> device = new HashMap<>();
            String[] columns = tab.getTableColumns();
            for (int j = 0; j < columns.length; j++) {
                String dbField = columns[j].replace(" ", "_");
                String valueStr = (String) tab.getTableModel().getValueAt(row, j);
                if (valueStr != null && !valueStr.trim().isEmpty()) {
                    device.put(dbField, valueStr);
                }
            }
            try {
                String assetName = device.get("AssetName");
                if (assetName != null && !assetName.trim().isEmpty()) {
                    HashMap<String, String> existingDevice = new DatabaseHandler().getDeviceByAssetNameFromDB(assetName);
                    if (existingDevice != null) {
                        boolean hasNewData = false;
                        for (java.util.Map.Entry<String, String> entry : device.entrySet()) {
                            String field = entry.getKey();
                            String newValue = entry.getValue();
                            String oldValue = existingDevice.get(field);
                            if (newValue != null && !newValue.trim().isEmpty() && 
                                (oldValue == null || oldValue.trim().isEmpty() || !newValue.equals(oldValue))) {
                                hasNewData = true;
                                break;
                            }
                        }
                        if (hasNewData) {
                            c.setBackground(Color.YELLOW);
                            c.setForeground(Color.BLACK);
                        } else {
                            c.setBackground(Color.GREEN);
                            c.setForeground(Color.RED);
                        }
                    } else {
                        c.setBackground(Color.GREEN);
                        c.setForeground(Color.BLACK);
                    }
                }
            } catch (SQLException e) {
                java.util.logging.Logger.getLogger(TableColorRenderer.class.getName()).log(Level.INFO, "Error rendering row color: {0}", e.getMessage());
            }
        }
        return c;
    }

    private boolean isDuplicateRow(int row) {
        HashMap<String, String> device = new HashMap<>();
        String[] columns = tab.getTableColumns();
        for (int j = 0; j < columns.length; j++) {
            String dbField = columns[j].replace(" ", "_");
            String value = (String) tab.getTableModel().getValueAt(row, j);
            if (value != null && !value.trim().isEmpty()) {
                device.put(dbField, value);
            }
        }
        try {
            String assetName = device.get("AssetName");
            if (assetName != null && !assetName.trim().isEmpty()) {
                HashMap<String, String> existingDevice = new DatabaseHandler().getDeviceByAssetNameFromDB(assetName);
                return existingDevice != null;
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TableColorRenderer.class.getName()).log(Level.INFO, "Error checking duplicate status: {0}", e.getMessage());
        }
        return false;
    }
}