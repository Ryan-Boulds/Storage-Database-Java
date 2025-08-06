package view_inventorytab;

import java.awt.FontMetrics;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import utils.DatabaseUtils;
import utils.InventoryData;

public class TableManager {
    private final JTable table;
    private final DefaultTableModel model;
    private String[] columns;
    private final Map<String, Integer> columnTypes;
    private TableRowSorter<DefaultTableModel> sorter;
    private final List<Integer> sortColumnIndices = new ArrayList<>();
    private final List<SortOrder> sortOrders = new ArrayList<>();

    public TableManager(JTable table) {
        this.table = table;
        this.model = new DefaultTableModel();
        this.columnTypes = new HashMap<>();
        if (table != null) {
            table.setModel(model);
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            initializeColumns();
            TableColumn editColumn = table.getColumnModel().getColumn(0);
            editColumn.setCellRenderer(new RowEditButtonRenderer());
            editColumn.setCellEditor(new RowEditButtonEditor(table, this));
        }
    }

    private void initializeColumns() {
        if (model != null) {
            model.addColumn("Edit");
            try (Connection conn = DatabaseUtils.getConnection();
                 ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM Inventory WHERE 1=0")) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<String> columnList = new ArrayList<>();
                columnTypes.clear();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    int sqlType = metaData.getColumnType(i);
                    columnList.add(columnName);
                    columnTypes.put(columnName, sqlType);
                }
                columns = columnList.toArray(new String[0]);
                for (String column : columns) {
                    model.addColumn(column);
                }
                System.out.println("TableManager: Initialized columns from database: " + String.join(", ", columns));
                System.out.println("TableManager: Column types: " + columnTypes);
            } catch (SQLException e) {
                System.err.println("TableManager: Error fetching columns from database: " + e.getMessage());
                JOptionPane.showMessageDialog(table, "Error fetching table columns: " + e.getMessage() + ". Please check database connection and Inventory table.", "Database Error", JOptionPane.ERROR_MESSAGE);
                columns = new String[0];
                columnTypes.clear();
            }
        }
    }

    public String[] getColumns() {
        return columns != null ? columns.clone() : new String[0];
    }

    public Map<String, Integer> getColumnTypes() {
        return new HashMap<>(columnTypes);
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
                maxWidth = fontMetrics.stringWidth("Edit") + padding + 20;
            } else {
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
        if (columns == null || columns.length == 0) {
            System.err.println("TableManager: No columns defined, skipping data refresh");
            return;
        }
        int[] selectedRows = table.getSelectedRows();
        model.setRowCount(0);
        ArrayList<HashMap<String, String>> devices = InventoryData.getDevices();
        if (devices == null || devices.isEmpty()) {
            System.err.println("TableManager: No devices retrieved from InventoryData");
            return;
        }
        System.out.println("TableManager: Retrieved " + devices.size() + " devices from InventoryData");
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        inputFormat.setLenient(false);
        for (Map<String, String> device : devices) {
            if (device == null) continue;
            Object[] row = new Object[columns.length + 1];
            row[0] = "Edit";
            for (int i = 0; i < columns.length; i++) {
                String column = columns[i];
                Integer sqlType = columnTypes.getOrDefault(column, Types.VARCHAR);
                String value = device.getOrDefault(column, "");
                if (sqlType == Types.DATE || sqlType == Types.TIMESTAMP || column.equals("Warranty_Expiry_Date") || 
                    column.equals("Last_Maintenance") || column.equals("Maintenance_Due") || column.equals("Date_Of_Purchase")) {
                    try {
                        row[i + 1] = value != null && !value.isEmpty() ? outputFormat.format(inputFormat.parse(value)) : "";
                    } catch (ParseException e) {
                        row[i + 1] = value;
                    }
                } else {
                    row[i + 1] = value;
                }
            }
            model.addRow(row);
        }
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
        if (columnIndex == 0) return;
        columnIndex -= 1;

        int index = sortColumnIndices.indexOf(columnIndex);
        SortOrder newOrder;
        if (index >= 0) {
            newOrder = sortOrders.get(index) == SortOrder.ASCENDING ? SortOrder.DESCENDING : SortOrder.ASCENDING;
            sortOrders.set(index, newOrder);
        } else {
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