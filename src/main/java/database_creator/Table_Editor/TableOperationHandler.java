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
        } else if (tableName.equalsIgnoreCase("Cables") || tableName.equalsIgnoreCase("Accessories") ||
                   tableName.equalsIgnoreCase("Chargers") || tableName.equalsIgnoreCase("Adapters")) {
            Map<String, String> idField = new HashMap<>();
            idField.put("name", "ID");
            idField.put("type", "AUTOINCREMENT");
            idField.put("primaryKey", "Yes");
            fields.add(idField);
            tableModel.addRow(new Object[]{"ID", "AUTOINCREMENT", "Yes"});
            Map<String, String> typeField = new HashMap<>();
            typeField.put("name", tableName.equalsIgnoreCase("Cables") ? "Cable_Type" :
                                  tableName.equalsIgnoreCase("Accessories") ? "Peripheral_Type" :
                                  tableName.equalsIgnoreCase("Chargers") ? "Charger_Type" : "Adapter_Type");
            typeField.put("type", "VARCHAR(255)");
            typeField.put("primaryKey", "No");
            fields.add(typeField);
            tableModel.addRow(new Object[]{typeField.get("name"), "VARCHAR(255)", "No"});
            Map<String, String> countField = new HashMap<>();
            countField.put("name", "Count");
            countField.put("type", "INTEGER");
            countField.put("primaryKey", "No");
            fields.add(countField);
            tableModel.addRow(new Object[]{"Count", "INTEGER", "No"});
            Map<String, String> locField = new HashMap<>();
            locField.put("name", "Location");
            locField.put("type", "VARCHAR(255)");
            locField.put("primaryKey", "No");
            fields.add(locField);
            tableModel.addRow(new Object[]{"Location", "VARCHAR(255)", "No"});
            Map<String, String> prevLocField = new HashMap<>();
            prevLocField.put("name", "Previous_Location");
            prevLocField.put("type", "VARCHAR(255)");
            prevLocField.put("primaryKey", "No");
            fields.add(prevLocField);
            tableModel.addRow(new Object[]{"Previous_Location", "VARCHAR(255)", "No"});
            editor.showMessageDialog("Status", "Default fields for " + tableName + " loaded. Click 'Create New Table' to confirm.", 1);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            if (tableExists(metaData, tableName)) {
                editor.showMessageDialog("Error", "Table '" + tableName + "' already exists.", 0);
                return;
            }
            StringBuilder sql = new StringBuilder("CREATE TABLE " + tableName + " (");
            for (int i = 0; i < fields.size(); i++) {
                Map<String, String> field = fields.get(i);
                sql.append(field.get("name")).append(" ").append(field.get("type"));
                if (field.get("primaryKey").equals("Yes")) {
                    sql.append(" PRIMARY KEY");
                }
                if (i < fields.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql.toString());
                editor.showMessageDialog("Success", "Table '" + tableName + "' created successfully.", 1);
                schemaManager.loadTableList();
                tableComboBox.setSelectedItem(tableName);
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to create table: " + e.getMessage(), 0);
        }
    }

    public void addField(JTextField fieldNameField, JComboBox<String> fieldTypeComboBox, JComboBox<String> primaryKeyComboBox) {
        String fieldName = fieldNameField.getText().trim();
        String fieldType = (String) fieldTypeComboBox.getSelectedItem();
        String primaryKey = (String) primaryKeyComboBox.getSelectedItem();
        if (fieldName.isEmpty()) {
            editor.showMessageDialog("Error", "Field name cannot be empty.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        if (isProtectedTable(tableName)) {
            editor.showMessageDialog("Error", "Cannot modify fields of protected table '" + tableName + "'.", 0);
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, tableName, fieldName);
            if (rs.next()) {
                editor.showMessageDialog("Error", "Field '" + fieldName + "' already exists in table '" + tableName + "'.", 0);
                return;
            }
            Map<String, String> field = new HashMap<>();
            field.put("name", fieldName);
            field.put("type", fieldType);
            field.put("primaryKey", primaryKey);
            fields.add(field);
            tableModel.addRow(new Object[]{fieldName, fieldType, primaryKey});
            fieldNameField.setText("");
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to add field: " + e.getMessage(), 0);
        }
    }

    public void moveColumnLeft() {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow <= 0) {
            editor.showMessageDialog("Error", "Cannot move field left.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        if (isProtectedTable(tableName)) {
            editor.showMessageDialog("Error", "Cannot modify fields of protected table '" + tableName + "'.", 0);
            return;
        }
        // Swap fields in the list
        Map<String, String> field = fields.remove(selectedRow);
        fields.add(selectedRow - 1, field);
        // Update table model
        tableModel.moveRow(selectedRow, selectedRow, selectedRow - 1);
        fieldsTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
        editor.showMessageDialog("Success", "Field moved left.", 1);
    }

    public void moveColumnRight() {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow >= fieldsTable.getRowCount() - 1) {
            editor.showMessageDialog("Error", "Cannot move field right.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        if (isProtectedTable(tableName)) {
            editor.showMessageDialog("Error", "Cannot modify fields of protected table '" + tableName + "'.", 0);
            return;
        }
        // Swap fields in the list
        Map<String, String> field = fields.remove(selectedRow);
        fields.add(selectedRow + 1, field);
        // Update table model
        tableModel.moveRow(selectedRow, selectedRow, selectedRow + 1);
        fieldsTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
        editor.showMessageDialog("Success", "Field moved right.", 1);
    }

    public void loadTableSchema() {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null) return;
        fields.clear();
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, tableName, null);
            ResultSet pkRs = metaData.getPrimaryKeys(null, null, tableName);
            List<String> primaryKeys = new ArrayList<>();
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
            while (rs.next()) {
                Map<String, String> field = new HashMap<>();
                String columnName = rs.getString("COLUMN_NAME");
                String columnType = rs.getString("TYPE_NAME");
                field.put("name", columnName);
                field.put("type", columnType);
                field.put("primaryKey", primaryKeys.contains(columnName) ? "Yes" : "No");
                fields.add(field);
                tableModel.addRow(new Object[]{columnName, columnType, field.get("primaryKey")});
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to load table schema: " + e.getMessage(), 0);
        }
    }

    public void deleteTable() {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null) {
            editor.showMessageDialog("Error", "No table selected.", 0);
            return;
        }
        if (isProtectedTable(tableName)) {
            editor.showMessageDialog("Error", "Cannot delete protected table '" + tableName + "'.", 0);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(editor, "Are you sure you want to delete table '" + tableName + "'?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE " + tableName);
            editor.showMessageDialog("Success", "Table '" + tableName + "' deleted successfully.", 1);
            schemaManager.loadTableList();
            fields.clear();
            tableModel.setRowCount(0);
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to delete table: " + e.getMessage(), 0);
        }
    }

    public void renameTable(JTextField newTableNameField) {
        String oldTableName = (String) tableComboBox.getSelectedItem();
        String newTableName = newTableNameField.getText().trim();
        if (newTableName.isEmpty()) {
            editor.showMessageDialog("Error", "New table name cannot be empty.", 0);
            return;
        }
        if (isProtectedTable(oldTableName)) {
            editor.showMessageDialog("Error", "Cannot rename protected table '" + oldTableName + "'.", 0);
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            if (tableExists(metaData, newTableName)) {
                editor.showMessageDialog("Error", "Table '" + newTableName + "' already exists.", 0);
                return;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE " + oldTableName + " RENAME TO " + newTableName);
                editor.showMessageDialog("Success", "Table renamed to '" + newTableName + "' successfully.", 1);
                schemaManager.loadTableList();
                tableComboBox.setSelectedItem(newTableName);
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to rename table: " + e.getMessage(), 0);
        }
    }

    public void renameColumn() {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow == -1) {
            editor.showMessageDialog("Error", "No field selected.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        String oldColumnName = (String) tableModel.getValueAt(selectedRow, 0);
        if (isProtectedColumn(tableName, oldColumnName)) {
            editor.showMessageDialog("Error", "Cannot rename protected column '" + oldColumnName + "'.", 0);
            return;
        }
        FieldDialog dialog = new FieldDialog();
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }
        String newColumnName = dialog.getField().getName();
        if (newColumnName.isEmpty()) {
            editor.showMessageDialog("Error", "New column name cannot be empty.", 0);
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, tableName, newColumnName);
            if (rs.next()) {
                editor.showMessageDialog("Error", "Column '" + newColumnName + "' already exists.", 0);
                return;
            }
            String sql = String.format("ALTER TABLE %s RENAME COLUMN %s TO %s", tableName, oldColumnName, newColumnName);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                fields.get(selectedRow).put("name", newColumnName);
                tableModel.setValueAt(newColumnName, selectedRow, 0);
                editor.showMessageDialog("Success", "Column renamed successfully.", 1);
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to rename column: " + e.getMessage(), 0);
        }
    }

    public void changeColumnType() {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow == -1) {
            editor.showMessageDialog("Error", "No field selected.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        if (isProtectedColumn(tableName, columnName)) {
            editor.showMessageDialog("Error", "Cannot change type of protected column '" + columnName + "'.", 0);
            return;
        }
        FieldDialog dialog = new FieldDialog();
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }
        String newType = dialog.getField().getType();
        try (Connection conn = DatabaseUtils.getConnection()) {
            String sql = String.format("ALTER TABLE %s ALTER COLUMN %s %s", tableName, columnName, newType);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                fields.get(selectedRow).put("type", newType);
                tableModel.setValueAt(newType, selectedRow, 1);
                editor.showMessageDialog("Success", "Column type changed successfully.", 1);
            }
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Failed to change column type: " + e.getMessage(), 0);
        }
    }

    public void setPrimaryKey() {
        int selectedRow = fieldsTable.getSelectedRow();
        if (selectedRow == -1) {
            editor.showMessageDialog("Error", "No field selected.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        if (isProtectedTable(tableName)) {
            editor.showMessageDialog("Error", "Cannot modify primary key of protected table '" + tableName + "'.", 0);
            return;
        }
        for (Map<String, String> field : fields) {
            if (field.get("primaryKey").equals("Yes")) {
                editor.showMessageDialog("Error", "Table already has a primary key.", 0);
                return;
            }
        }
        try (Connection conn = DatabaseUtils.getConnection()) {
            String sql = String.format("ALTER TABLE %s ADD CONSTRAINT PK_%s PRIMARY KEY (%s)", tableName, tableName, columnName);
            try (Statement stmt = conn.createStatement()) {
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
            editor.showMessageDialog("Error", "No field selected.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        if (isProtectedTable(tableName)) {
            editor.showMessageDialog("Error", "Cannot remove primary key of protected table '" + tableName + "'.", 0);
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getPrimaryKeys(null, null, tableName);
            String pkName = null;
            while (rs.next()) {
                if (rs.getString("COLUMN_NAME").equals(columnName)) {
                    pkName = rs.getString("PK_NAME");
                    break;
                }
            }
            if (pkName == null) {
                editor.showMessageDialog("Error", "No primary key found for column '" + columnName + "'.", 0);
                return;
            }
            String sql = String.format("ALTER TABLE %s DROP CONSTRAINT %s", tableName, pkName);
            try (Statement stmt = conn.createStatement()) {
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
            editor.showMessageDialog("Error", "No field selected.", 0);
            return;
        }
        String tableName = (String) tableComboBox.getSelectedItem();
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        if (isProtectedColumn(tableName, columnName)) {
            editor.showMessageDialog("Error", "Cannot delete protected column '" + columnName + "'.", 0);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(editor, 
                "Are you sure you want to delete column '" + columnName + "' from table '" + tableName + "'?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
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

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private boolean isProtectedColumn(String tableName, String columnName) {
        if (tableName == null) return false;
        if (tableName.equalsIgnoreCase("TableInformation") && 
            (columnName.equals("ID") || columnName.equals("InventoryTables") || columnName.equals("SoftwareTables"))) return true;
        if (tableName.equalsIgnoreCase("Templates") && columnName.equals("Template_Name")) return true;
        if (tableName.equalsIgnoreCase("Accessories") && 
            (columnName.equals("ID") || columnName.equals("Peripheral_Type") || columnName.equals("Count") || 
             columnName.equals("Location") || columnName.equals("Previous_Location"))) return true;
        if (tableName.equalsIgnoreCase("Cables") && 
            (columnName.equals("ID") || columnName.equals("Cable_Type") || columnName.equals("Count") || 
             columnName.equals("Location") || columnName.equals("Previous_Location"))) return true;
        if (tableName.equalsIgnoreCase("Chargers") && 
            (columnName.equals("ID") || columnName.equals("Charger_Type") || columnName.equals("Count") || 
             columnName.equals("Location") || columnName.equals("Previous_Location"))) return true;
        if (tableName.equalsIgnoreCase("Adapters") && 
            (columnName.equals("ID") || columnName.equals("Adapter_Type") || columnName.equals("Count") || 
             columnName.equals("Location") || columnName.equals("Previous_Location"))) return true;
        return tableName.equalsIgnoreCase("LicenseKeyRules") && 
                (columnName.equals("Rule_ID") || columnName.equals("Rule_Name") || columnName.equals("Rule_Description"));
    }

    private boolean isProtectedTable(String tableName) {
        return tableName.equalsIgnoreCase("TableInformation") || 
               tableName.equalsIgnoreCase("Templates") || 
               tableName.equalsIgnoreCase("Accessories") || 
               tableName.equalsIgnoreCase("Cables") || 
               tableName.equalsIgnoreCase("Chargers") || 
               tableName.equalsIgnoreCase("Adapters") || 
               tableName.equalsIgnoreCase("LicenseKeyRules");
    }

}