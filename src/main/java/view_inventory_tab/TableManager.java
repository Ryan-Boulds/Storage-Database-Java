package view_inventory_tab;

import java.awt.FontMetrics;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import utils.DatabaseUtils;

public class TableManager {
    private final JTable table;
    private final DefaultTableModel model;
    private String[] columns;
    private final Map<String, Integer> columnTypes;
    private TableRowSorter<DefaultTableModel> sorter = null;
    private final List<Integer> sortColumnIndices = new ArrayList<>();
    private final List<SortOrder> sortOrders = new ArrayList<>();
    private String tableName;
    private String whereClause;
    private static final Logger LOGGER = Logger.getLogger(TableManager.class.getName());

    public TableManager(JTable table, String tableName) {
        this.table = table;
        this.tableName = tableName != null ? tableName : "Inventory";
        this.whereClause = "";
        this.model = new DefaultTableModel();
        this.columnTypes = new HashMap<>();
        if (table != null) {
            table.setModel(model);
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            initializeColumns();
        }
    }

    public TableManager(JTable table) {
        this(table, "Inventory");
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName != null ? tableName : "Inventory";
        initializeColumns();
    }

    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause == null ? "" : whereClause.trim();
    }

    private void initializeColumns() {
        if (model != null) {
            model.setRowCount(0);
            model.setColumnCount(0);
            model.addColumn("Edit");
            columns = new String[0]; // Clear existing columns
            columnTypes.clear(); // Clear existing column types
            try (Connection conn = DatabaseUtils.getConnection();
                 ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM [" + tableName + "] WHERE 1=0")) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<String> columnList = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    int sqlType = metaData.getColumnType(i);
                    columnList.add(columnName);
                    columnTypes.put(columnName, sqlType);
                }
                columns = columnList.toArray(new String[0]);
                for (String column : columnList) {
                    model.addColumn(column);
                }
                LOGGER.log(Level.INFO, "Initialized columns from database table %s: %s", tableName);
                LOGGER.log(Level.INFO, "Column types: %s", columnTypes);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error fetching columns from database for table %s: %s", new Object[]{tableName, e.getMessage()});
                JOptionPane.showMessageDialog(table, String.format("Error fetching table columns: %s. Please check database connection and table %s.", e.getMessage(), tableName), "Database Error", JOptionPane.ERROR_MESSAGE);
                columns = new String[0];
                columnTypes.clear();
            }

            if (table.getColumnModel().getColumnCount() > 0) {
                TableColumn editColumn = table.getColumnModel().getColumn(0);
                editColumn.setCellRenderer(new RowEditButtonRenderer());
                editColumn.setCellEditor(new RowEditButtonEditor(table, this));
                editColumn.setPreferredWidth(100);
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
        FontMetrics fontMetrics = table.getFontMetrics(table.getFont());
        int padding = 20;
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            String header = table.getColumnName(i);
            int maxWidth = fontMetrics.stringWidth(header) + padding;
            if (i == 0) {
                maxWidth = Math.max(maxWidth, 100);
                column.setPreferredWidth(maxWidth);
                continue;
            }
            for (int row = 0; row < table.getRowCount(); row++) {
                Object value = table.getValueAt(row, i);
                String text = value != null ? value.toString() : "";
                int textWidth = fontMetrics.stringWidth(text) + padding;
                maxWidth = Math.max(maxWidth, textWidth);
            }
            column.setPreferredWidth(maxWidth);
        }
    }

    public void refreshDataAndTabs() {
        if (table == null || model == null) {
            LOGGER.log(Level.SEVERE, "Table or model is null during refresh");
            return;
        }
        // Reinitialize columns to reflect any schema changes
        initializeColumns();

        if (columns == null || columns.length == 0) {
            LOGGER.log(Level.SEVERE, "No columns defined for table %s, skipping data refresh", tableName);
            JOptionPane.showMessageDialog(table, "No columns defined for table " + tableName, "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int[] selectedRows = table.getSelectedRows();
        model.setRowCount(0);
        ArrayList<HashMap<String, String>> devices = new ArrayList<>();
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM [" + tableName + "]" + (whereClause.isEmpty() ? "" : " WHERE " + whereClause))) {
            while (rs.next()) {
                HashMap<String, String> device = new HashMap<>();
                for (String column : columns) {
                    String value = rs.getString(column);
                    device.put(column, value != null ? value : "");
                }
                devices.add(device);
            }
            LOGGER.log(Level.INFO, "Retrieved %d devices from table %s", new Object[]{devices.size(), tableName});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching devices from table %s: %s", new Object[]{tableName, e.getMessage()});
            JOptionPane.showMessageDialog(table, "Error fetching data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            devices = null;
        }

        if (devices == null || devices.isEmpty()) {
            LOGGER.log(Level.INFO, "No devices retrieved from table %s", tableName);
            JOptionPane.showMessageDialog(table, "No data available in table " + tableName, "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

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
        // Ensure cell renderer and editor are reapplied
        if (table.getColumnModel().getColumnCount() > 0) {
            TableColumn editColumn = table.getColumnModel().getColumn(0);
            editColumn.setCellRenderer(new RowEditButtonRenderer());
            editColumn.setCellEditor(new RowEditButtonEditor(table, this));
            editColumn.setPreferredWidth(100);
        }
        SwingUtilities.invokeLater(() -> {
            table.revalidate();
            table.repaint();
            LOGGER.log(Level.INFO, "refreshDataAndTabs: Completed table refresh for %s with columns: %s", tableName);
        });
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