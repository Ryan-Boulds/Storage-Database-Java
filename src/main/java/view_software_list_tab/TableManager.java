package view_software_list_tab;

import java.awt.FontMetrics;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import view_software_list_tab.Add_And_Edit_Entries.RowEditButtonEditor;
import view_software_list_tab.Add_And_Edit_Entries.RowEditButtonRenderer;
import view_software_list_tab.license_key_tracker.LicenseKeyTracker;

public final class TableManager {

    private final JTable table;
    protected DefaultTableModel model;
    private String[] columns;
    private final Map<String, Integer> columnTypes;
    private TableRowSorter<DefaultTableModel> sorter = null;
    private final List<Integer> sortColumnIndices = new ArrayList<>();
    private final List<SortOrder> sortOrders = new ArrayList<>();
    private String tableName;
    private String whereClause;
    private boolean isInitialized = false;
    private static final Logger LOGGER = Logger.getLogger(TableManager.class.getName());

    public TableManager(JTable table, String tableName) {
        this.table = table;
        this.tableName = tableName;
        this.whereClause = "";
        this.columnTypes = new HashMap<>();
        createNewModel();
        if (table != null) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            if (this.tableName != null && !this.tableName.isEmpty()) {
                initializeColumns();
            }
        }
    }

    public TableManager(JTable table) {
        this(table, null);
    }

    private void createNewModel() {
        model = new DefaultTableModel();
        if (table != null) {
            table.setModel(model);
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
        }
    }

    public void setParentTab(ViewSoftwareListTab parentTab) {
        LOGGER.log(Level.INFO, "Set parentTab for TableManager, tableName='{0}'", tableName == null ? "null" : tableName);
    }

    public void setLicenseKeyTracker(LicenseKeyTracker tracker) {
        LOGGER.log(Level.INFO, "Set LicenseKeyTracker for TableManager, tableName='{0}'", tableName == null ? "null" : tableName);
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        if (!Objects.equals(this.tableName, tableName)) {
            this.tableName = tableName;
            createNewModel();
            isInitialized = false;
            LOGGER.log(Level.INFO, "Table name changed to '{0}'", tableName == null ? "null" : tableName);
        }
        if (this.tableName != null && !this.tableName.isEmpty()) {
            initializeColumns();
        }
    }

    public void setWhereClause(String searchText, String statusFilter, String deptFilter) {
        StringBuilder where = new StringBuilder();
        if (searchText != null && !searchText.trim().isEmpty()) {
            where.append(" WHERE (");
            for (String column : columns) {
                where.append("[").append(column).append("] LIKE '%").append(searchText.replace("'", "''")).append("%'");
                where.append(" OR ");
            }
            where.setLength(where.length() - 4); // Remove last " OR "
            where.append(")");
        }
        if (statusFilter != null && !statusFilter.equals("All")) {
            if (where.length() > 0) {
                where.append(" AND ");
            } else {
                where.append(" WHERE ");
            }
            where.append("[Status] = '").append(statusFilter.replace("'", "''")).append("'");
        }
        if (deptFilter != null && !deptFilter.equals("All")) {
            if (where.length() > 0) {
                where.append(" AND ");
            } else {
                where.append(" WHERE ");
            }
            where.append("[Department] = '").append(deptFilter.replace("'", "''")).append("'");
        }
        this.whereClause = where.toString();
        LOGGER.log(Level.INFO, "Set whereClause for table '{0}': {1}", new Object[]{tableName, whereClause});
    }

    public void refreshDataAndTabs() {
        if (tableName == null || tableName.isEmpty()) {
            LOGGER.log(Level.WARNING, "No table name set for refreshDataAndTabs");
            model.setRowCount(0);
            model.setColumnCount(0);
            if (table != null) {
                table.revalidate();
                table.repaint();
            }
            return;
        }
        if (!isInitialized) {
            initializeColumns();
        }
        try (Connection conn = DatabaseUtils.getConnection()) {
            String sql = "SELECT * FROM [" + tableName + "]" + whereClause;
            try (Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                 ResultSet rs = stmt.executeQuery(sql)) {
                String[] newColumns = getColumnsFromResultSet(rs);
                if (!columnsMatch(newColumns)) {
                    initializeColumns();
                }
                model.setRowCount(0);
                Object[] row = new Object[columns.length + 1];
                int rowCount = 0;
                while (rs.next()) {
                    row[0] = "Edit";
                    for (int i = 0; i < columns.length; i++) {
                        row[i + 1] = rs.getObject(i + 1);
                    }
                    model.addRow(row);
                    rowCount++;
                }
                LOGGER.log(Level.INFO, "Loaded {0} rows for table '{1}'", new Object[]{rowCount, tableName});
                if (rowCount > 0 && !model.getDataVector().isEmpty()) {
                    LOGGER.log(Level.INFO, "Sample row data for '{0}': {1}", new Object[]{tableName, model.getDataVector().get(0)});
                }
            }
            if (table != null) {
                table.setModel(model);
                table.revalidate();
                table.repaint();
                LOGGER.log(Level.INFO, "Table UI refreshed for '{0}'", tableName);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing data for table '{0}': {1}", new Object[]{tableName, e.getMessage()});
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(table, "Error loading data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    public void initializeColumns() {
        try (Connection conn = DatabaseUtils.getConnection()) {
            String sql = "SELECT * FROM [" + tableName + "] WHERE 1=0";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                String[] newColumns = getColumnsFromResultSet(rs);
                model.setColumnIdentifiers(new Object[]{"Edit"});
                columnTypes.clear();
                for (String column : newColumns) {
                    model.addColumn(column);
                    columnTypes.put(column, Types.VARCHAR);
                }
                columns = newColumns;
                TableColumn editColumn = table.getColumnModel().getColumn(0);
                editColumn.setCellRenderer(new RowEditButtonRenderer());
                editColumn.setCellEditor(new RowEditButtonEditor(table, this));
                editColumn.setPreferredWidth(100);
                adjustColumnWidths();
            }
            isInitialized = true;
            LOGGER.log(Level.INFO, "Initialized columns for table '{0}': {1}", new Object[]{tableName, String.join(", ", columns)});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initializing columns for table '{0}': {1}", new Object[]{tableName, e.getMessage()});
            columns = new String[0];
            columnTypes.clear();
            isInitialized = false;
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(table, "Error initializing columns: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private boolean columnsMatch(String[] newColumns) {
        if (columns == null || newColumns.length != columns.length + 1) {
            return false;
        }
        for (int i = 0; i < columns.length; i++) {
            if (!columns[i].equals(newColumns[i + 1])) {
                return false;
            }
        }
        return true;
    }

    private String[] getColumnsFromResultSet(ResultSet rs) throws SQLException {
        List<String> columnList = new ArrayList<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            columnList.add(rs.getMetaData().getColumnName(i));
        }
        return columnList.toArray(new String[0]);
    }

    private void adjustColumnWidths() {
        if (table == null) {
            return;
        }
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
        if (columnIndex == 0) {
            return;
        }
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
        LOGGER.log(Level.INFO, "Sorted table '{0}' on column index {1}", new Object[]{tableName, columnIndex + 1});
    }

    public String[] getColumns() {
        return columns;
    }

    public Map<String, Integer> getColumnTypes() {
        return columnTypes;
    }

    public JTable getTable() {
        return table;
    }
}