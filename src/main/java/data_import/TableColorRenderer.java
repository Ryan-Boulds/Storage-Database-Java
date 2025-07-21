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
        
        if (!tab.isShowDuplicates() && isExactDuplicate(row)) {
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
                // Check if the row is resolved
                boolean isResolved = tab.getOriginalData().get(row).isResolved();
                if (isResolved) {
                    c.setBackground(Color.GREEN); // Resolved rows are green
                    c.setForeground(Color.BLACK);
                } else if (assetName != null && !assetName.trim().isEmpty()) {
                    HashMap<String, String> existingDevice = new DatabaseHandler().getDeviceByAssetNameFromDB(assetName);
                    if (existingDevice != null) {
                        boolean isExactMatch = true;
                        boolean hasNewData = false;
                        boolean hasConflict = false;
                        for (java.util.Map.Entry<String, String> entry : device.entrySet()) {
                            String field = entry.getKey();
                            String newValue = entry.getValue();
                            String oldValue = existingDevice.get(field);
                            String newVal = (newValue == null || newValue.trim().isEmpty()) ? "" : newValue;
                            String oldVal = (oldValue == null || oldValue.trim().isEmpty()) ? "" : oldValue;
                            if (!newVal.equals(oldVal)) {
                                isExactMatch = false;
                                if (oldVal.isEmpty()) {
                                    hasNewData = true;
                                } else {
                                    hasConflict = true;
                                }
                            }
                        }
                        if (isExactMatch) {
                            c.setBackground(Color.RED); // Exact duplicate
                            c.setForeground(Color.BLACK);
                        } else if (hasConflict) {
                            c.setBackground(Color.ORANGE); // Conflicting data
                            c.setForeground(Color.BLACK);
                        } else if (hasNewData) {
                            c.setBackground(Color.YELLOW); // New data for blank fields
                            c.setForeground(Color.BLACK);
                        } else {
                            c.setBackground(table.getBackground()); // Should not occur (fallback)
                            c.setForeground(table.getForeground());
                        }
                    } else {
                        c.setBackground(table.getBackground()); // New entry, use default (white)
                        c.setForeground(table.getForeground());
                    }
                } else {
                    c.setBackground(table.getBackground()); // No AssetName, treat as new (white)
                    c.setForeground(table.getForeground());
                }
            } catch (SQLException e) {
                java.util.logging.Logger.getLogger(TableColorRenderer.class.getName()).log(Level.INFO, "Error rendering row color: {0}", e.getMessage());
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }
        }
        return c;
    }

    public boolean isExactDuplicate(int row) {
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
                if (existingDevice != null) {
                    for (java.util.Map.Entry<String, String> entry : device.entrySet()) {
                        String field = entry.getKey();
                        String newValue = entry.getValue();
                        String oldValue = existingDevice.get(field);
                        String newVal = (newValue == null || newValue.trim().isEmpty()) ? "" : newValue;
                        String oldVal = (oldValue == null || oldValue.trim().isEmpty()) ? "" : oldValue;
                        if (!newVal.equals(oldVal)) {
                            return false;
                        }
                    }
                    return true; // Exact match
                }
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TableColorRenderer.class.getName()).log(Level.INFO, "Error checking duplicate status: {0}", e.getMessage());
        }
        return false;
    }

    public boolean isYellowOrOrange(int row) {
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
                if (existingDevice != null) {
                    boolean hasNewData = false;
                    boolean hasConflict = false;
                    for (java.util.Map.Entry<String, String> entry : device.entrySet()) {
                        String field = entry.getKey();
                        String newValue = entry.getValue();
                        String oldValue = existingDevice.get(field);
                        String newVal = (newValue == null || newValue.trim().isEmpty()) ? "" : newValue;
                        String oldVal = (oldValue == null || oldValue.trim().isEmpty()) ? "" : oldValue;
                        if (!newVal.equals(oldVal)) {
                            if (oldVal.isEmpty()) {
                                hasNewData = true;
                            } else {
                                hasConflict = true;
                            }
                        }
                    }
                    return hasNewData || hasConflict; // Yellow or orange
                }
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TableColorRenderer.class.getName()).log(Level.INFO, "Error checking row status: {0}", e.getMessage());
        }
        return false;
    }
}