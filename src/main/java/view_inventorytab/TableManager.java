package view_inventorytab;

import java.awt.FontMetrics;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import utils.DefaultColumns;
import utils.InventoryData;

public class TableManager {
    private final JTable table;
    private final DefaultTableModel model;
    private final String[] columns = DefaultColumns.getInventoryColumns();
    private TableRowSorter<DefaultTableModel> sorter;

    public TableManager(JTable table) {
        this.table = table;
        this.model = new DefaultTableModel();
        if (table != null) {
            table.setModel(model);
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Allows manual resizing
            initializeColumns();
        }
    }

    private void initializeColumns() {
        if (model != null) {
            for (String column : columns) {
                model.addColumn(column);
            }
        }
    }

    private void adjustColumnWidths() {
        if (table == null || table.getColumnModel() == null) {
            return;
        }
        DefaultTableColumnModel columnModel = (DefaultTableColumnModel) table.getColumnModel();
        FontMetrics fontMetrics = table.getFontMetrics(table.getFont());
        int padding = 20; // Extra padding for readability

        for (int i = 0; i < columnModel.getColumnCount() && i < columns.length; i++) {
            TableColumn column = columnModel.getColumn(i);
            String header = columns[i];
            int maxWidth = fontMetrics.stringWidth(header) + padding;

            // Calculate max width based on cell content
            for (int row = 0; row < table.getRowCount(); row++) {
                Object value = table.getValueAt(row, i);
                String text = value != null ? value.toString() : "";
                int textWidth = fontMetrics.stringWidth(text) + padding;
                maxWidth = Math.max(maxWidth, textWidth);
            }

            column.setPreferredWidth(maxWidth);
            // Remove minWidth and maxWidth to allow manual resizing
        }
    }

    public void refreshDataAndTabs() {
        if (table == null || model == null) {
            System.err.println("Table or model is null during refresh");
            return;
        }
        model.setRowCount(0);
        ArrayList<HashMap<String, String>> devices = InventoryData.getDevices();
        if (devices == null) {
            System.err.println("No devices retrieved from InventoryData");
            return;
        }
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        inputFormat.setLenient(false);
        for (Map<String, String> device : devices) {
            if (device == null) continue;
            Object[] row = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                String column = columns[i];
                if (column.equals("Created_at") || column.equals("Last_Successful_Scan")) {
                    try {
                        row[i] = device.get(column) != null ? outputFormat.format(inputFormat.parse(device.get(column))) : "";
                    } catch (ParseException e) {
                        row[i] = device.getOrDefault(column, "");
                    }
                } else {
                    row[i] = device.getOrDefault(column, "");
                }
            }
            model.addRow(row);
        }
        adjustColumnWidths();
    }

    public void sortTable(int columnIndex) {
        java.util.List<RowSorter.SortKey> sortKeys = new java.util.ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(columnIndex, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.sort();
    }
}