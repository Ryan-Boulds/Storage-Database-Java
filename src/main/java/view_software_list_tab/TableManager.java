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
import view_software_list_tab.Add_And_Edit_Entries.RowEditButtonEditor;
import view_software_list_tab.Add_And_Edit_Entries.RowEditButtonRenderer;
import view_software_list_tab.license_key_tracker.LicenseKeyTracker;

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
    private LicenseKeyTracker licenseKeyTracker;
    private boolean isInitialized = false; // Track initialization state
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

    public void setLicenseKeyTracker(LicenseKeyTracker tracker) {
        this.licenseKeyTracker = tracker;
        LOGGER.log(Level.INFO, "Set LicenseKeyTracker for TableManager, tableName='{0}'", tableName);
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
        if (this.tableName != null && !this.tableName.isEmpty()) {
            initializeColumns();
        }
    }

    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause != null ? whereClause : "";
        LOGGER.log(Level.INFO, "Set whereClause='{0}' for table '{1}'", new Object[]{whereClause, tableName});
    }

    public String getWhereClause() {
        return whereClause;
    }

    public void refreshDataAndTabs() {
        if (tableName == null || tableName.isEmpty()) {
            LOGGER.log(Level.WARNING, "No table name set for refreshDataAndTabs");
            return;
        }
        try {
            StringBuilder query = new StringBuilder("SELECT * FROM [" + tableName + "]");
            if (!whereClause.isEmpty()) {
                query.append(whereClause);
            }
            try (Connection conn = DatabaseUtils.getConnection();
                 Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                 ResultSet rs = stmt.executeQuery(query.toString())) {

                String[] dbColumns = getColumnsFromResultSet(rs);
                String[] newColumns = new String[dbColumns.length + 1];
                newColumns[0] = "Edit";
                System.arraycopy(dbColumns, 0, newColumns, 1, dbColumns.length);
                
                // Only update model if columns have changed
                if (!isInitialized || !columnsMatch(newColumns)) {
                    model.setColumnIdentifiers(newColumns);
                    Map<String, Integer> newColumnTypes = new HashMap<>();
                    newColumnTypes.put("Edit", Types.VARCHAR);
                    for (String column : dbColumns) {
                        newColumnTypes.put(column, Types.VARCHAR);
                    }
                    columnTypes.clear();
                    columnTypes.putAll(newColumnTypes);
                    columns = dbColumns;
                    isInitialized = true;
                }
                
                model.setRowCount(0);

                while (rs.next()) {
                    Object[] row = new Object[newColumns.length];
                    row[0] = "Edit";
                    for (int i = 0; i < dbColumns.length; i++) {
                        row[i + 1] = rs.getString(dbColumns[i]);
                    }
                    model.addRow(row);
                }

                if (table != null) {
                    TableColumn editColumn = table.getColumnModel().getColumn(0);
                    editColumn.setCellRenderer(new RowEditButtonRenderer());
                    editColumn.setCellEditor(new RowEditButtonEditor(table, this));
                    editColumn.setPreferredWidth(100);
                }
                adjustColumnWidths();
                SwingUtilities.invokeLater(() -> {
                    table.revalidate();
                    table.repaint();
                    LOGGER.log(Level.INFO, "refreshDataAndTabs: Completed table refresh for {0} with columns: {1}", 
                        new Object[]{tableName, String.join(", ", newColumns)});
                });

                if (licenseKeyTracker != null) {
                    LOGGER.log(Level.INFO, "Notifying LicenseKeyTracker to refresh key list for table '{0}'", tableName);
                    licenseKeyTracker.loadLicenseKeys();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing data for table {0}: {1}", new Object[]{tableName, e.getMessage()});
        }
    }

    public void initializeColumns() {
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM [" + tableName + "] WHERE 1=0")) {
            String[] dbColumns = getColumnsFromResultSet(rs);
            String[] newColumns = new String[dbColumns.length + 1];
            newColumns[0] = "Edit";
            System.arraycopy(dbColumns, 0, newColumns, 1, dbColumns.length);
            model.setColumnIdentifiers(newColumns);
            Map<String, Integer> newColumnTypes = new HashMap<>();
            newColumnTypes.put("Edit", Types.VARCHAR);
            for (String column : dbColumns) {
                newColumnTypes.put(column, Types.VARCHAR);
            }
            columnTypes.clear();
            columnTypes.putAll(newColumnTypes);
            columns = dbColumns;

            if (table != null) {
                TableColumn editColumn = table.getColumnModel().getColumn(0);
                editColumn.setCellRenderer(new RowEditButtonRenderer());
                editColumn.setCellEditor(new RowEditButtonEditor(table, this));
                editColumn.setPreferredWidth(100);
            }
            adjustColumnWidths();
            isInitialized = true;
            LOGGER.log(Level.INFO, "Initialized columns for table '{0}': {1}", 
                new Object[]{tableName, String.join(", ", newColumns)});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initializing columns for table {0}: {1}", new Object[]{tableName, e.getMessage()});
        }
    }

    private boolean columnsMatch(String[] newColumns) {
        if (columns == null || newColumns.length != columns.length + 1) return false;
        for (int i = 0; i < columns.length; i++) {
            if (!columns[i].equals(newColumns[i + 1])) return false;
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

    public JTable getTable() {
        return table;
    }
}