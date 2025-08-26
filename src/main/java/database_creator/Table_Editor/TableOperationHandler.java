package database_creator.Table_Editor;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import database_creator.FieldDialog;
import utils.DatabaseUtils;

public class TableOperationHandler {
    private final TableEditor editor;
    private final JComboBox<String> tableComboBox;
    private final JTable fieldsTable;
    private final DefaultTableModel tableModel;
    private final List<Map<String, String>> fields;
    private final TableSchemaManager schemaManager;

    public TableOperationHandler(TableEditor editor, JComboBox<String> tableComboBox, JTable fieldsTable, 
            DefaultTableModel tableModel, List<Map<String, String>> fields, TableSchemaManager schemaManager) {
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

        if (tableName.equalsIgnoreCase("Templates")) {
            Map<String, String> field = new HashMap<>();
            field.put("name", "Template_Name");
            field.put("type", "TEXT");
            field.put("primaryKey", "Yes");
            fields.add(field);
            tableModel.addRow(new Object[]{"Template_Name", "TEXT", "Yes"});
            editor.showMessageDialog("Status", "Default fields for Templates loaded. Click 'Create New Table' to confirm.", 1);
            return;
        } else if (tableName.equalsIgnoreCase("TableInformation")) {
            Map<String, String> idField = new HashMap<>();
            idField.put("name", "ID");
            idField.put("type", "INTEGER");
            idField.put("primaryKey", "Yes");
            fields.add(idField);
            tableModel.addRow(new Object[]{"ID", "INTEGER", "Yes"});
            Map<String, String> invField = new HashMap<>();
            invField.put("name", "InventoryTables");
            invField.put("type", "TEXT");
            invField.put("primaryKey", "No");
            fields.add(invField);
            tableModel.addRow(new Object[]{"InventoryTables", "TEXT", "No"});
            Map<String, String> softField = new HashMap<>();
            softField.put("name", "SoftwareTables");
            softField.put("type", "TEXT");
            softField.put("primaryKey", "No");
            fields.add(softField);
            tableModel.addRow(new Object[]{"SoftwareTables", "TEXT", "No"});
            editor.showMessageDialog("Status", "Default fields for TableInformation loaded. Click 'Create New Table' to confirm.", 1);
            return;
        } else if (tableName.equalsIgnoreCase("Accessories") || tableName.equalsIgnoreCase("Cables") || tableName.equalsIgnoreCase("Adapters")) {
            String typeField = tableName.equalsIgnoreCase("Accessories") ? "Peripheral_Type" : 
                              tableName.equalsIgnoreCase("Cables") ? "Cable_Type" : "Adapter_Type";
            Map<String, String> field = new HashMap<>();
            field.put("name", typeField);
            field.put("type", "TEXT");
            field.put("primaryKey", "Yes");
            fields.add(field);
            tableModel.addRow(new Object[]{typeField, "TEXT", "Yes"});
            Map<String, String> countField = new HashMap<>();
            countField.put("name", "Count");
            countField.put("type", "INTEGER");
            countField.put("primaryKey", "No");
            fields.add(countField);
            tableModel.addRow(new Object[]{"Count", "INTEGER", "No"});
            editor.showMessageDialog("Status", "Default fields for " + tableName + " loaded. Click 'Create New Table' to confirm.", 1);
            return;
        } else if (tableName.equalsIgnoreCase("LicenseKeyRules")) {
            Map<String, String> idField = new HashMap<>();
            idField.put("name", "Rule_ID");
            idField.put("type", "INTEGER");
            idField.put("primaryKey", "Yes");
            fields.add(idField);
            tableModel.addRow(new Object[]{"Rule_ID", "INTEGER", "Yes"});
            Map<String, String> nameField = new HashMap<>();
            nameField.put("name", "Rule_Name");
            nameField.put("type", "TEXT");
            nameField.put("primaryKey", "No");
            fields.add(nameField);
            tableModel.addRow(new Object[]{"Rule_Name", "TEXT", "No"});
            Map<String, String> descField = new HashMap<>();
            descField.put("name", "Rule_Description");
            descField.put("type", "TEXT");
            descField.put("primaryKey", "No");
            fields.add(descField);
            tableModel.addRow(new Object[]{"Rule_Description", "TEXT", "No"});
            editor.showMessageDialog("Status", "Default fields for LicenseKeyRules loaded. Click 'Create New Table' to confirm.", 1);
            return;
        }

        tableComboBox.addItem(tableName);
        tableComboBox.setSelectedItem(tableName);
        editor.showMessageDialog("Status", "Enter fields for table: " + tableName, 1);
    }

    public void saveTable() throws SQLException {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null || tableName.isEmpty()) {
            editor.showMessageDialog("Error", "No table selected.", 0);
            return;
        }
        if (fields.isEmpty()) {
            editor.showMessageDialog("Error", "No fields defined for the table.", 0);
            return;
        }

        StringBuilder sql = new StringBuilder("CREATE TABLE " + tableName + " (");
        for (int i = 0; i < fields.size(); i++) {
            Map<String, String> field = fields.get(i);
            String name = field.get("name");
            String type = field.get("type");
            String primaryKey = field.get("primaryKey");

            sql.append(name).append(" ").append(type);
            if ("Yes".equals(primaryKey)) {
                sql.append(" PRIMARY KEY");
            }
            if (i < fields.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");

        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql.toString());
            editor.showMessageDialog("Success", "Table " + tableName + " created successfully.", 1);
            schemaManager.loadTableList();
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to create table: " + e.getMessage(), 0);
            throw e;
        }
    }

    public void addField(JTextField fieldNameField, JComboBox<String> fieldTypeComboBox, JComboBox<String> primaryKeyComboBox) {
        String fieldName = fieldNameField.getText().trim();
        String fieldType = (String) fieldTypeComboBox.getSelectedItem();
        String primaryKey = (String) primaryKeyComboBox.getSelectedItem();

        if (fieldName.isEmpty() || fieldType == null) {
            editor.showMessageDialog("Error", "Field name and type must be specified.", 0);
            return;
        }

        Map<String, String> field = new HashMap<>();
        field.put("name", fieldName);
        field.put("type", fieldType);
        field.put("primaryKey", primaryKey != null ? primaryKey : "No");
        fields.add(field);
        tableModel.addRow(new Object[]{fieldName, fieldType, primaryKey != null ? primaryKey : "No"});
        fieldNameField.setText("");
    }

    public void deleteField() {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow != -1) {
            String tableName = (String) tableComboBox.getSelectedItem();
            String columnName = (String) tableModel.getValueAt(selectedRow, 0);
            if (isProtectedColumn(tableName, columnName)) {
                editor.showMessageDialog("Error", "Cannot delete protected column: " + columnName, 0);
                return;
            }
            fields.remove(selectedRow);
            tableModel.removeRow(selectedRow);
        } else {
            editor.showMessageDialog("Error", "No field selected.", 0);
        }
    }

    public void loadTableSchema() {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null) {
            return;
        }
        fields.clear();
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, tableName, null);
            while (rs.next()) {
                Map<String, String> field = new HashMap<>();
                String columnName = rs.getString("COLUMN_NAME");
                String columnType = rs.getString("TYPE_NAME");
                field.put("name", columnName);
                field.put("type", columnType);
                field.put("primaryKey", isPrimaryKey(tableName, columnName) ? "Yes" : "No");
                fields.add(field);
                tableModel.addRow(new Object[]{columnName, columnType, field.get("primaryKey")});
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to load table schema: " + e.getMessage(), 0);
        }
    }

    public void addColumn() {
        FieldDialog dialog = new FieldDialog();
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            database_creator.Field field = dialog.getField();
            String tableName = (String) tableComboBox.getSelectedItem();
            if (tableName == null) {
                editor.showMessageDialog("Error", "No table selected.", 0);
                return;
            }
            try {
                String sql = String.format("ALTER TABLE %s ADD %s %s", tableName, field.getName(), field.getType());
                try (Connection conn = DatabaseUtils.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(sql);
                    loadTableSchema();
                    editor.showMessageDialog("Success", "Column added successfully.", 1);
                }
            } catch (SQLException e) {
                editor.showMessageDialog("Error", "Failed to add column: " + e.getMessage(), 0);
            }
        }
    }

    public void deleteTable() {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null) {
            editor.showMessageDialog("Error", "No table selected.", 0);
            return;
        }
        if (isProtectedTable(tableName)) {
            editor.showMessageDialog("Error", "Cannot delete protected table: " + tableName, 0);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(editor, "Are you sure you want to delete table " + tableName + "?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String sql = "DROP TABLE " + tableName;
            try (Connection conn = DatabaseUtils.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                schemaManager.loadTableList();
                editor.showMessageDialog("Success", "Table deleted successfully.", 1);
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to delete table: " + e.getMessage(), 0);
        }
    }

    public void renameColumn() {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow == -1) {
            editor.showMessageDialog("Error", "No column selected.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        if (isProtectedColumn(tableName, columnName)) {
            editor.showMessageDialog("Error", "Cannot rename protected column: " + columnName, 0);
            return;
        }
        String newName = JOptionPane.showInputDialog(editor, "Enter new column name:", columnName);
        if (newName == null || newName.trim().isEmpty()) {
            return;
        }
        try {
            String sql = String.format("ALTER TABLE %s RENAME COLUMN %s TO %s", tableName, columnName, newName);
            try (Connection conn = DatabaseUtils.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                fields.get(selectedRow).put("name", newName);
                tableModel.setValueAt(newName, selectedRow, 0);
                editor.showMessageDialog("Success", "Column renamed successfully.", 1);
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to rename column: " + e.getMessage(), 0);
        }
    }

    public void changeColumnType() {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow == -1) {
            editor.showMessageDialog("Error", "No column selected.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        if (isProtectedColumn(tableName, columnName)) {
            editor.showMessageDialog("Error", "Cannot modify protected column: " + columnName, 0);
            return;
        }
        String[] types = {"TEXT", "INTEGER", "DOUBLE", "DATE", "VARCHAR(255)"};
        String newType = (String) JOptionPane.showInputDialog(editor, "Select new type:", "Change Column Type", 
                JOptionPane.PLAIN_MESSAGE, null, types, types[0]);
        if (newType == null) {
            return;
        }
        try {
            String sql = String.format("ALTER TABLE %s ALTER COLUMN %s %s", tableName, columnName, newType);
            try (Connection conn = DatabaseUtils.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                fields.get(selectedRow).put("type", newType);
                tableModel.setValueAt(newType, selectedRow, 1);
                editor.showMessageDialog("Success", "Column type changed successfully.", 1);
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to change column type: " + e.getMessage(), 0);
        }
    }

    public void moveColumn(int direction) {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow == -1) {
            editor.showMessageDialog("Error", "No column selected.", 0);
            return;
        }
        if ((direction == -1 && selectedRow == 0) || (direction == 1 && selectedRow == fields.size() - 1)) {
            return;
        }
        int newIndex = selectedRow + direction;
        Map<String, String> field = fields.remove(selectedRow);
        fields.add(newIndex, field);
        tableModel.removeRow(selectedRow);
        tableModel.insertRow(newIndex, new Object[]{field.get("name"), field.get("type"), field.get("primaryKey")});
        fieldsTable.setRowSelectionInterval(newIndex, newIndex);
    }

    public void setPrimaryKey() {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow == -1) {
            editor.showMessageDialog("Error", "No column selected.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        if (isProtectedColumn(tableName, columnName)) {
            editor.showMessageDialog("Error", "Cannot modify protected column: " + columnName, 0);
            return;
        }
        try {
            String sql = String.format("ALTER TABLE %s ADD PRIMARY KEY (%s)", tableName, columnName);
            try (Connection conn = DatabaseUtils.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                fields.get(selectedRow).put("primaryKey", "Yes");
                tableModel.setValueAt("Yes", selectedRow, 2);
                editor.showMessageDialog("Success", "Primary key set successfully.", 1);
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to set primary key: " + e.getMessage(), 0);
        }
    }

    public void removePrimaryKey() {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow == -1) {
            editor.showMessageDialog("Error", "No column selected.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        if (!isPrimaryKey(tableName, columnName)) {
            editor.showMessageDialog("Error", "Column is not a primary key.", 0);
            return;
        }
        try {
            String sql = String.format("ALTER TABLE %s DROP PRIMARY KEY", tableName);
            try (Connection conn = DatabaseUtils.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                fields.get(selectedRow).put("primaryKey", "No");
                tableModel.setValueAt("No", selectedRow, 2);
                editor.showMessageDialog("Success", "Primary key removed successfully.", 1);
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to remove primary key: " + e.getMessage(), 0);
        }
    }

    public void deleteColumn() {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow == -1) {
            editor.showMessageDialog("Error", "No column selected.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        if (isProtectedColumn(tableName, columnName)) {
            editor.showMessageDialog("Error", "Cannot delete protected column: " + columnName, 0);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(editor, "Are you sure you want to delete column " + columnName + "?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String sql = String.format("ALTER TABLE %s DROP COLUMN %s", tableName, columnName);
            try (Connection conn = DatabaseUtils.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                fields.remove(selectedRow);
                tableModel.removeRow(selectedRow);
                editor.showMessageDialog("Success", "Column deleted successfully.", 1);
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to delete column: " + e.getMessage(), 0);
        }
    }

    private boolean isProtectedColumn(String tableName, String columnName) {
        if (tableName == null) return false;
        if (tableName.equalsIgnoreCase("TableInformation") && 
            (columnName.equals("ID") || columnName.equals("InventoryTables") || columnName.equals("SoftwareTables"))) return true;
        if (tableName.equalsIgnoreCase("Templates") && columnName.equals("Template_Name")) return true;
        if (tableName.equalsIgnoreCase("Accessories") && columnName.equals("Peripheral_Type")) return true;
        if (tableName.equalsIgnoreCase("Cables") && columnName.equals("Cable_Type")) return true;
        if (tableName.equalsIgnoreCase("Adapters") && columnName.equals("Adapter_Type")) return true;
        return tableName.equalsIgnoreCase("LicenseKeyRules") && 
                (columnName.equals("Rule_ID") || columnName.equals("Rule_Name") || columnName.equals("Rule_Description"));
    }

    private boolean isProtectedTable(String tableName) {
        return tableName.equalsIgnoreCase("TableInformation") || 
               tableName.equalsIgnoreCase("Templates") || 
               tableName.equalsIgnoreCase("Accessories") || 
               tableName.equalsIgnoreCase("Cables") || 
               tableName.equalsIgnoreCase("Adapters") || 
               tableName.equalsIgnoreCase("LicenseKeyRules");
    }

    private boolean isPrimaryKey(String tableName, String columnName) {
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getPrimaryKeys(null, null, tableName);
            while (rs.next()) {
                if (rs.getString("COLUMN_NAME").equals(columnName)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to check primary key: " + e.getMessage(), 0);
        }
        return false;
    }
}