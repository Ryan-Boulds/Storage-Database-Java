package view_inventorytab;

import java.awt.FontMetrics;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import utils.DatabaseUtils;
import utils.DefaultColumns;
import utils.InventoryData;

public class TableManager {
    private final JTable table;
    private final DefaultTableModel model;
    private String[] columns;
    private TableRowSorter<DefaultTableModel> sorter;
    private final List<Integer> sortColumnIndices = new ArrayList<>();
    private final List<SortOrder> sortOrders = new ArrayList<>();

    public TableManager(JTable table) {
        this.table = table;
        this.model = new DefaultTableModel();
        if (table != null) {
            table.setModel(model);
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            initializeColumns();
            // Set renderer and editor for Edit column
            TableColumn editColumn = table.getColumnModel().getColumn(0);
            editColumn.setCellRenderer(new RowEditButtonRenderer());
            editColumn.setCellEditor(new RowEditButtonEditor(table, this));
        }
    }

    private void initializeColumns() {
        if (model != null) {
            model.addColumn("Edit"); // Add Edit column
            // Get columns dynamically from database
            try (Connection conn = DatabaseUtils.getConnection();
                 ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM Inventory WHERE 1=0")) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<String> columnList = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    columnList.add(columnName);
                }
                columns = columnList.toArray(new String[0]);
                for (String column : columns) {
                    model.addColumn(column);
                }
                System.out.println("TableManager: Initialized columns from database: " + String.join(", ", columns)); // Debug
            } catch (SQLException e) {
                System.err.println("TableManager: Error fetching columns from database: " + e.getMessage()); // Debug
                // Fallback to DefaultColumns
                columns = DefaultColumns.getInventoryColumns();
                for (String column : columns) {
                    model.addColumn(column);
                }
                System.out.println("TableManager: Fallback to DefaultColumns: " + String.join(", ", columns)); // Debug
            }
        }
    }

    private void adjustColumnWidths() {
        if (table == null || table.getColumnModel() == null) {
            return;
        }
        DefaultTableColumnModel columnModel = (DefaultTableColumnModel) table.getColumnModel();
        FontMetrics fontMetrics = table.getFontMetrics(table.getFont());
        int padding = 20;

        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            String header = i == 0 ? "Edit" : columns[i - 1];
            int maxWidth = fontMetrics.stringWidth(header) + padding;

            if (i == 0) {
                // Fixed width for Edit button
                maxWidth = fontMetrics.stringWidth("Edit") + padding + 20;
            } else {
                // Calculate max width for data columns
                for (int row = 0; row < table.getRowCount(); row++) {
                    Object value = table.getValueAt(row, i);
                    String text = value != null ? value.toString() : "";
                    int textWidth = fontMetrics.stringWidth(text) + padding;
                    maxWidth = Math.max(maxWidth, textWidth);
                }
            }

            column.setPreferredWidth(maxWidth);
        }
    }

    public void refreshDataAndTabs() {
        if (table == null || model == null) {
            System.err.println("TableManager: Table or model is null during refresh");
            return;
        }
        // Preserve selection
        int[] selectedRows = table.getSelectedRows();
        model.setRowCount(0);
        ArrayList<HashMap<String, String>> devices = InventoryData.getDevices();
        if (devices == null) {
            System.err.println("TableManager: No devices retrieved from InventoryData");
            return;
        }
        System.out.println("TableManager: Retrieved " + devices.size() + " devices from InventoryData"); // Debug
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        inputFormat.setLenient(false);
        for (Map<String, String> device : devices) {
            if (device == null) continue;
            Object[] row = new Object[columns.length + 1];
            row[0] = "Edit"; // Placeholder for button
            for (int i = 0; i < columns.length; i++) {
                String column = columns[i];
                if (column.equals("Created_at") || column.equals("Last_Successful_Scan")) {
                    try {
                        row[i + 1] = device.get(column) != null ? outputFormat.format(inputFormat.parse(device.get(column))) : "";
                    } catch (ParseException e) {
                        row[i + 1] = device.getOrDefault(column, "");
                    }
                } else {
                    row[i + 1] = device.getOrDefault(column, "");
                }
            }
            model.addRow(row);
        }
        // Restore selection
        for (int row : selectedRows) {
            if (row < table.getRowCount()) {
                table.addRowSelectionInterval(row, row);
            }
        }
        adjustColumnWidths();
        table.revalidate();
        table.repaint();
    }

    public void sortTable(int columnIndex) {
        // Adjust for Edit column
        if (columnIndex == 0) return; // Skip sorting on Edit column
        columnIndex -= 1; // Adjust for data columns

        int index = sortColumnIndices.indexOf(columnIndex);
        SortOrder newOrder;
        if (index >= 0) {
            // Toggle sort order
            newOrder = sortOrders.get(index) == SortOrder.ASCENDING ? SortOrder.DESCENDING : SortOrder.ASCENDING;
            sortOrders.set(index, newOrder);
        } else {
            // New sort column
            sortColumnIndices.add(columnIndex);
            newOrder = SortOrder.ASCENDING;
            sortOrders.add(newOrder);
        }

        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        for (int i = 0; i < sortColumnIndices.size(); i++) {
            sortKeys.add(new RowSorter.SortKey(sortColumnIndices.get(i) + 1, sortOrders.get(i)));
        }
        sorter.setSortKeys(sortKeys);
        sorter.sort();
    }
}