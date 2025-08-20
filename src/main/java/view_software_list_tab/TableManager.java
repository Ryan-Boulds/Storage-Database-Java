package view_software_list_tab;

import java.awt.FontMetrics;
import java.sql.Connection;
import java.sql.ResultSet;
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

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import utils.DatabaseUtils;

public final class TableManager {
    private final JTable table;
    protected final DefaultTableModel model;
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
        this.tableName = tableName;
        this.whereClause = "";
        this.model = new DefaultTableModel();
        this.columnTypes = new HashMap<>();
        if (table != null) {
            table.setModel(model);
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            if (this.tableName != null && !this.tableName.isEmpty()) {
                initializeColumns();
            }
        }
    }

    public TableManager(JTable table) {
        this(table, null);
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
        if (this.tableName != null && !this.tableName.isEmpty()) {
            initializeColumns();
            refreshDataAndTabs();
        } else {
            model.setRowCount(0);
            model.setColumnCount(0);
            if (table != null) {
                table.revalidate();
                table.repaint();
            }
        }
    }

    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause == null ? "" : whereClause.trim();
    }

    private void initializeColumns() {
        if (tableName == null || tableName.isEmpty()) {
            model.setRowCount(0);
            model.setColumnCount(0);
            if (table != null) {
                table.revalidate();
                table.repaint();
            }
            return;
        }
        model.setRowCount(0);
        model.setColumnCount(0);
        model.addColumn("Edit");
        columns = getTableColumns(tableName);
        for (String column : columns) {
            model.addColumn(column);
        }
    }

    private String[] getTableColumns(String tableName) {
        List<String> columnList = new ArrayList<>();
        try (Connection conn = DatabaseUtils.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                int sqlType = rs.getInt("DATA_TYPE");
                columnList.add(columnName);
                columnTypes.put(columnName, sqlType);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching columns for table {0}: {1}", new Object[]{tableName, e.getMessage()});
        }
        return columnList.toArray(new String[0]);
    }

    public void refreshDataAndTabs() {
        initializeColumns();
        String sql = "SELECT * FROM [" + tableName + "]" + (whereClause.isEmpty() ? "" : " WHERE " + whereClause);
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");
            while (rs.next()) {
                Object[] row = new Object[columns.length + 1];
                row[0] = "Edit";
                for (int i = 0; i < columns.length; i++) {
                    String column = columns[i];
                    Integer sqlType = columnTypes.getOrDefault(column, Types.VARCHAR);
                    if (sqlType == Types.BOOLEAN) {
                        row[i + 1] = rs.getBoolean(column);
                    } else if (sqlType == Types.DATE || sqlType == Types.TIMESTAMP || column.equals("Warranty_Expiry_Date") || 
                        column.equals("Last_Maintenance") || column.equals("Maintenance_Due") || column.equals("Date_Of_Purchase")) {
                        String dateStr = rs.getString(column);
                        if (dateStr != null && !dateStr.isEmpty()) {
                            try {
                                row[i + 1] = outputFormat.format(inputFormat.parse(dateStr));
                            } catch (ParseException e) {
                                row[i + 1] = dateStr;
                            }
                        } else {
                            row[i + 1] = "";
                        }
                    } else {
                        row[i + 1] = rs.getString(column);
                    }
                }
                model.addRow(row);
            }
            adjustColumnWidths();
            if (table.getColumnModel().getColumnCount() > 0) {
                TableColumn editColumn = table.getColumnModel().getColumn(0);
                editColumn.setCellRenderer(new RowEditButtonRenderer());
                editColumn.setCellEditor(new RowEditButtonEditor(table, this));
                editColumn.setPreferredWidth(100);
            }
            SwingUtilities.invokeLater(() -> {
                table.revalidate();
                table.repaint();
                LOGGER.log(Level.INFO, "refreshDataAndTabs: Completed table refresh for %s with columns: %s", new Object[]{tableName, String.join(", ", columns)});
            });
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing data for table {0}: {1}", new Object[]{tableName, e.getMessage()});
        }
    }

    private void adjustColumnWidths() {
        if (table == null) return;
        FontMetrics fm = table.getFontMetrics(table.getFont());
        for (int col = 0; col < table.getColumnCount(); col++) {
            TableColumn column = table.getColumnModel().getColumn(col);
            int maxWidth = fm.stringWidth(column.getHeaderValue().toString()) + 20;
            for (int row = 0; row < table.getRowCount(); row++) {
                Object value = table.getValueAt(row, col);
                if (value != null) {
                    maxWidth = Math.max(maxWidth, fm.stringWidth(value.toString()) + 20);
                }
            }
            column.setPreferredWidth(maxWidth);
        }
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

    public String[] getColumns() {
        return columns;
    }

    public Map<String, Integer> getColumnTypes() {
        return columnTypes;
    }
}