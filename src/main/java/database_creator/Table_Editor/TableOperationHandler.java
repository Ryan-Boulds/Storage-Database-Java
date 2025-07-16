package database_creator.Table_Editor;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import utils.DatabaseUtils;

public class TableOperationHandler {
    private final TableEditor editor;
    private final JComboBox<String> tableComboBox;
    private final JTable fieldsTable;
    private final DefaultTableModel tableModel;
    private final List<Map<String, String>> fields;
    private final TableSchemaManager schemaManager;

    public TableOperationHandler(TableEditor editor, JComboBox<String> tableComboBox, JTable fieldsTable, DefaultTableModel tableModel, List<Map<String, String>> fields, TableSchemaManager schemaManager) {
        this.editor = editor;
        this.tableComboBox = tableComboBox;
        this.fieldsTable = fieldsTable;
        this.tableModel = tableModel;
        this.fields = fields;
        this.schemaManager = schemaManager;
    }

    public void createNewTable(JTextField newTableNameField) {
        String tableName = newTableNameField.getText().trim();
        if (tableName.isEmpty()) {
            editor.showMessageDialog("Error", "Table name cannot be empty.", 0);
            return;
        }
        fields.clear();
        tableModel.setRowCount(0);
        editor.showMessageDialog("Status", "New table definition started. Add fields and create table.", 1);

        String fieldName = javax.swing.JOptionPane.showInputDialog(editor, "Enter first field name for '" + tableName + "':");
        if (fieldName == null || fieldName.trim().isEmpty()) {
            editor.showMessageDialog("Error", "At least one field is required.", 0);
            return;
        }
        String fieldType = javax.swing.JOptionPane.showInputDialog(editor, "Enter field type for '" + fieldName + "' (e.g., VARCHAR(255), INTEGER):");
        if (fieldType == null || fieldType.trim().isEmpty() || !isValidDataType(fieldType)) {
            editor.showMessageDialog("Error", "Invalid or empty field type. Use VARCHAR(255), INTEGER, DATE, DOUBLE, etc.", 0);
            return;
        }
        Map<String, String> field = new HashMap<>();
        field.put("name", fieldName);
        field.put("type", fieldType);
        field.put("primaryKey", "No");
        fields.add(field);
        tableModel.addRow(new Object[]{fieldName, fieldType, "No"});

        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE " + tableName + " (");
        for (int i = 0; i < fields.size(); i++) {
            field = fields.get(i);
            String name = field.get("name");
            String type = field.get("type");
            createTableSQL.append(name).append(" ").append(type);
            if (field.get("primaryKey").equals("Yes")) {
                createTableSQL.append(" PRIMARY KEY");
            }
            if (i < fields.size() - 1) {
                createTableSQL.append(", ");
            }
        }
        createTableSQL.append(")");

        try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSQL.toString());
            editor.showMessageDialog("Success", "Table '" + tableName + "' created successfully.", 1);
            newTableNameField.setText("");
            fields.clear();
            tableModel.setRowCount(0);
            schemaManager.loadTableList();
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Error creating table: " + e.getMessage(), 0);
        }
    }

    public void renameColumn() {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            editor.showMessageDialog("Error", "Select a table and column to rename.", 0);
            return;
        }
        String oldName = (String) tableModel.getValueAt(selectedRow, 0);
        String newName = javax.swing.JOptionPane.showInputDialog(editor, "Enter new column name for '" + oldName + "':");
        if (newName == null || newName.trim().isEmpty()) {
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            List<Map<String, String>> newFields = new ArrayList<>(fields);
            newFields.get(selectedRow).put("name", newName);
            String tempTable = tableName + "_temp_" + System.currentTimeMillis();
            StringBuilder createSQL = new StringBuilder("CREATE TABLE " + tempTable + " (");
            for (int i = 0; i < newFields.size(); i++) {
                Map<String, String> field = newFields.get(i);
                createSQL.append(field.get("name")).append(" ").append(field.get("type"));
                if (field.get("primaryKey").equals("Yes")) {
                    createSQL.append(" PRIMARY KEY");
                }
                if (i < newFields.size() - 1) {
                    createSQL.append(", ");
                }
            }
            createSQL.append(")");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createSQL.toString());
                StringBuilder columns = new StringBuilder();
                StringBuilder newColumns = new StringBuilder();
                for (int i = 0; i < fields.size(); i++) {
                    columns.append(fields.get(i).get("name"));
                    newColumns.append(i == selectedRow ? newName : fields.get(i).get("name"));
                    if (i < fields.size() - 1) {
                        columns.append(", ");
                        newColumns.append(", ");
                    }
                }
                stmt.executeUpdate("INSERT INTO " + tempTable + " (" + newColumns + ") SELECT " + columns + " FROM " + tableName);
                stmt.executeUpdate("DROP TABLE " + tableName);
                stmt.executeUpdate("ALTER TABLE " + tempTable + " RENAME TO " + tableName);
            }
            loadTableSchema();
            schemaManager.loadTableList();
            editor.showMessageDialog("Status", "Column renamed to: " + newName, 1);
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Error renaming column: " + e.getMessage(), 0);
        }
    }

    public void changeColumnType() {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            editor.showMessageDialog("Error", "Select a table and column to change type.", 0);
            return;
        }
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        String newType = javax.swing.JOptionPane.showInputDialog(editor, "Enter new type for '" + columnName + "' (e.g., VARCHAR(255), INTEGER):");
        if (newType == null || newType.trim().isEmpty() || !isValidDataType(newType)) {
            editor.showMessageDialog("Error", "Invalid or empty field type. Use VARCHAR(255), INTEGER, DATE, DOUBLE, etc.", 0);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            List<Map<String, String>> newFields = new ArrayList<>(fields);
            newFields.get(selectedRow).put("type", newType);
            String tempTable = tableName + "_temp_" + System.currentTimeMillis();
            StringBuilder createSQL = new StringBuilder("CREATE TABLE " + tempTable + " (");
            for (int i = 0; i < newFields.size(); i++) {
                Map<String, String> field = newFields.get(i);
                createSQL.append(field.get("name")).append(" ").append(field.get("type"));
                if (field.get("primaryKey").equals("Yes")) {
                    createSQL.append(" PRIMARY KEY");
                }
                if (i < newFields.size() - 1) {
                    createSQL.append(", ");
                }
            }
            createSQL.append(")");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createSQL.toString());
                StringBuilder columns = new StringBuilder();
                for (int i = 0; i < fields.size(); i++) {
                    columns.append(fields.get(i).get("name"));
                    if (i < fields.size() - 1) {
                        columns.append(", ");
                    }
                }
                stmt.executeUpdate("INSERT INTO " + tempTable + " (" + columns + ") SELECT " + columns + " FROM " + tableName);
                stmt.executeUpdate("DROP TABLE " + tableName);
                stmt.executeUpdate("ALTER TABLE " + tempTable + " RENAME TO " + tableName);
            }
            loadTableSchema();
            schemaManager.loadTableList();
            editor.showMessageDialog("Status", "Column type changed for: " + columnName, 1);
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Error changing column type: " + e.getMessage(), 0);
        }
    }

    public void moveColumn(int direction) {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            editor.showMessageDialog("Error", "Select a table and column to move.", 0);
            return;
        }
        int newIndex = selectedRow + direction;
        if (newIndex < 0 || newIndex >= fields.size()) {
            editor.showMessageDialog("Error", "Cannot move column beyond table bounds.", 0);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            List<Map<String, String>> newFields = new ArrayList<>(fields);
            Map<String, String> temp = newFields.get(selectedRow);
            newFields.set(selectedRow, newFields.get(newIndex));
            newFields.set(newIndex, temp);
            String tempTable = tableName + "_temp_" + System.currentTimeMillis();
            StringBuilder createSQL = new StringBuilder("CREATE TABLE " + tempTable + " (");
            for (int i = 0; i < newFields.size(); i++) {
                Map<String, String> field = newFields.get(i);
                createSQL.append(field.get("name")).append(" ").append(field.get("type"));
                if (field.get("primaryKey").equals("Yes")) {
                    createSQL.append(" PRIMARY KEY");
                }
                if (i < newFields.size() - 1) {
                    createSQL.append(", ");
                }
            }
            createSQL.append(")");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createSQL.toString());
                StringBuilder columns = new StringBuilder();
                for (int i = 0; i < fields.size(); i++) {
                    columns.append(fields.get(i).get("name"));
                    if (i < fields.size() - 1) {
                        columns.append(", ");
                    }
                }
                stmt.executeUpdate("INSERT INTO " + tempTable + " (" + columns + ") SELECT " + columns + " FROM " + tableName);
                stmt.executeUpdate("DROP TABLE " + tableName);
                stmt.executeUpdate("ALTER TABLE " + tempTable + " RENAME TO " + tableName);
            }
            loadTableSchema();
            schemaManager.loadTableList();
            editor.showMessageDialog("Status", "Column moved successfully.", 1);
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Error moving column: " + e.getMessage(), 0);
        }
    }

    public void setPrimaryKey() {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            editor.showMessageDialog("Error", "Select a table and column to set as primary key.", 0);
            return;
        }
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);

        try (Connection conn = DatabaseUtils.getConnection()) {
            List<Map<String, String>> newFields = new ArrayList<>(fields);
            for (Map<String, String> field : newFields) {
                field.put("primaryKey", field.get("name").equals(columnName) ? "Yes" : "No");
            }
            String tempTable = tableName + "_temp_" + System.currentTimeMillis();
            StringBuilder createSQL = new StringBuilder("CREATE TABLE " + tempTable + " (");
            for (int i = 0; i < newFields.size(); i++) {
                Map<String, String> field = newFields.get(i);
                createSQL.append(field.get("name")).append(" ").append(field.get("type"));
                if (field.get("primaryKey").equals("Yes")) {
                    createSQL.append(" PRIMARY KEY");
                }
                if (i < newFields.size() - 1) {
                    createSQL.append(", ");
                }
            }
            createSQL.append(")");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createSQL.toString());
                StringBuilder columns = new StringBuilder();
                for (int i = 0; i < fields.size(); i++) {
                    columns.append(fields.get(i).get("name"));
                    if (i < fields.size() - 1) {
                        columns.append(", ");
                    }
                }
                stmt.executeUpdate("INSERT INTO " + tempTable + " (" + columns + ") SELECT " + columns + " FROM " + tableName);
                stmt.executeUpdate("DROP TABLE " + tableName);
                stmt.executeUpdate("ALTER TABLE " + tempTable + " RENAME TO " + tableName);
            }
            loadTableSchema();
            schemaManager.loadTableList();
            editor.showMessageDialog("Status", "Primary key set on: " + columnName, 1);
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Error setting primary key: " + e.getMessage(), 0);
        }
    }

    public void removePrimaryKey() {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            editor.showMessageDialog("Error", "Select a table and column to remove primary key.", 0);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            List<Map<String, String>> newFields = new ArrayList<>(fields);
            for (Map<String, String> field : newFields) {
                field.put("primaryKey", "No");
            }
            String tempTable = tableName + "_temp_" + System.currentTimeMillis();
            StringBuilder createSQL = new StringBuilder("CREATE TABLE " + tempTable + " (");
            for (int i = 0; i < newFields.size(); i++) {
                Map<String, String> field = newFields.get(i);
                createSQL.append(field.get("name")).append(" ").append(field.get("type"));
                if (i < newFields.size() - 1) {
                    createSQL.append(", ");
                }
            }
            createSQL.append(")");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createSQL.toString());
                StringBuilder columns = new StringBuilder();
                for (int i = 0; i < fields.size(); i++) {
                    columns.append(fields.get(i).get("name"));
                    if (i < fields.size() - 1) {
                        columns.append(", ");
                    }
                }
                stmt.executeUpdate("INSERT INTO " + tempTable + " (" + columns + ") SELECT " + columns + " FROM " + tableName);
                stmt.executeUpdate("DROP TABLE " + tableName);
                stmt.executeUpdate("ALTER TABLE " + tempTable + " RENAME TO " + tableName);
            }
            loadTableSchema();
            schemaManager.loadTableList();
            editor.showMessageDialog("Status", "Primary key removed.", 1);
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Error removing primary key: " + e.getMessage(), 0);
        }
    }

    public void addColumn() {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null) {
            editor.showMessageDialog("Error", "Select a table to add a column.", 0);
            return;
        }
        String columnName = javax.swing.JOptionPane.showInputDialog(editor, "Enter new column name:");
        if (columnName == null || columnName.trim().isEmpty()) {
            return;
        }
        String columnType = javax.swing.JOptionPane.showInputDialog(editor, "Enter column type (e.g., VARCHAR(255), INTEGER):");
        if (columnType == null || columnType.trim().isEmpty() || !isValidDataType(columnType)) {
            editor.showMessageDialog("Error", "Invalid or empty field type. Use VARCHAR(255), INTEGER, DATE, DOUBLE, etc.", 0);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
            loadTableSchema();
            schemaManager.loadTableList();
            editor.showMessageDialog("Status", "Column added: " + columnName, 1);
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Error adding column: " + e.getMessage(), 0);
        }
    }

    public void deleteColumn() {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            editor.showMessageDialog("Error", "Select a table and column to delete.", 0);
            return;
        }
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        int confirm = javax.swing.JOptionPane.showConfirmDialog(editor, "Delete column '" + columnName + "'? This cannot be undone.", "Confirm Delete", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            List<Map<String, String>> newFields = new ArrayList<>(fields);
            newFields.remove(selectedRow);
            String tempTable = tableName + "_temp_" + System.currentTimeMillis();
            StringBuilder createSQL = new StringBuilder("CREATE TABLE " + tempTable + " (");
            for (int i = 0; i < newFields.size(); i++) {
                Map<String, String> field = newFields.get(i);
                createSQL.append(field.get("name")).append(" ").append(field.get("type"));
                if (field.get("primaryKey").equals("Yes")) {
                    createSQL.append(" PRIMARY KEY");
                }
                if (i < newFields.size() - 1) {
                    createSQL.append(", ");
                }
            }
            createSQL.append(")");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createSQL.toString());
                StringBuilder columns = new StringBuilder();
                for (int i = 0; i < newFields.size(); i++) {
                    columns.append(newFields.get(i).get("name"));
                    if (i < newFields.size() - 1) {
                        columns.append(", ");
                    }
                }
                if (!newFields.isEmpty()) {
                    stmt.executeUpdate("INSERT INTO " + tempTable + " (" + columns + ") SELECT " + columns + " FROM " + tableName);
                }
                stmt.executeUpdate("DROP TABLE " + tableName);
                stmt.executeUpdate("ALTER TABLE " + tempTable + " RENAME TO " + tableName);
            }
            loadTableSchema();
            schemaManager.loadTableList();
            editor.showMessageDialog("Status", "Column deleted: " + columnName, 1);
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Error deleting column: " + e.getMessage(), 0);
        }
    }

    public void deleteTable() {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null) {
            editor.showMessageDialog("Error", "Select a table to delete.", 0);
            return;
        }
        int confirm = javax.swing.JOptionPane.showConfirmDialog(editor, "Delete table '" + tableName + "'? This cannot be undone.", "Confirm Delete", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE " + tableName);
            schemaManager.loadTableList();
            tableModel.setRowCount(0);
            fields.clear();
            editor.showMessageDialog("Status", "Table deleted: " + tableName, 1);
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Error deleting table: " + e.getMessage(), 0);
        }
    }

    public void loadTableSchema() {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null) {
            tableModel.setRowCount(0);
            fields.clear();
            editor.showMessageDialog("Status", "No table selected.", 1);
            return;
        }

        fields.clear();
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    Map<String, String> field = new HashMap<>();
                    String columnName = rs.getString("COLUMN_NAME");
                    String columnType = rs.getString("TYPE_NAME");
                    field.put("name", columnName);
                    field.put("type", columnType);
                    field.put("primaryKey", "No");
                    fields.add(field);
                    tableModel.addRow(new Object[]{columnName, columnType, "No"});
                }
            }
            try (ResultSet rs = metaData.getPrimaryKeys(null, null, tableName)) {
                while (rs.next()) {
                    String pkColumn = rs.getString("COLUMN_NAME");
                    for (Map<String, String> field : fields) {
                        if (field.get("name").equals(pkColumn)) {
                            field.put("primaryKey", "Yes");
                            int row = fields.indexOf(field);
                            tableModel.setValueAt("Yes", row, 2);
                        }
                    }
                }
            }
            editor.showMessageDialog("Status", "Schema loaded for table: " + tableName, 1);
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Error loading schema: " + e.getMessage(), 0);
        }
    }

    public boolean isValidDataType(String type) {
        String normalizedType = type.toUpperCase().trim();
        return normalizedType.startsWith("VARCHAR") ||
               normalizedType.equals("INTEGER") ||
               normalizedType.equals("DATE") ||
               normalizedType.equals("DOUBLE") ||
               normalizedType.equals("TEXT") ||
               normalizedType.equals("BOOLEAN") ||
               normalizedType.equals("LONG") ||
               normalizedType.equals("MEMO") ||
               normalizedType.equals("CURRENCY");
    }
}