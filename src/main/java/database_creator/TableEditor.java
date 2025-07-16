package database_creator;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import utils.DatabaseUtils;
import utils.UIComponentUtils;

public class TableEditor extends JPanel {
    private final JComboBox<String> tableComboBox;
    private final JTable fieldsTable;
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[]{"Field Name", "Field Type", "Primary Key"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final List<Map<String, String>> fields = new ArrayList<>();
    private JLabel statusLabel = null;

    public TableEditor() {
        setLayout(new BorderLayout(10, 10));

        // Table selection
        JPanel topPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        tableComboBox = new JComboBox<>();
        tableComboBox.addActionListener(e -> loadTableSchema());
        topPanel.add(new JLabel("Select Table:"));
        topPanel.add(tableComboBox);

        // New table input
        JTextField newTableNameField = UIComponentUtils.createFormattedTextField();
        topPanel.add(new JLabel("New Table Name:"));
        topPanel.add(newTableNameField);

        // Field input for new tables
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField fieldNameField = UIComponentUtils.createFormattedTextField();
        JTextField fieldTypeField = UIComponentUtils.createFormattedTextField();
        inputPanel.add(new JLabel("New Field Name:"));
        inputPanel.add(fieldNameField);
        inputPanel.add(new JLabel("Field Type (e.g., VARCHAR(255), INTEGER):"));
        inputPanel.add(fieldTypeField);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton addFieldButton = UIComponentUtils.createFormattedButton("Add Field");
        addFieldButton.addActionListener(e -> {
            String fieldName = fieldNameField.getText().trim();
            String fieldType = fieldTypeField.getText().trim();
            if (fieldName.isEmpty() || fieldType.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Field name and type cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Map<String, String> field = new HashMap<>();
            field.put("name", fieldName);
            field.put("type", fieldType);
            field.put("primaryKey", "No");
            fields.add(field);
            tableModel.addRow(new Object[]{fieldName, fieldType, "No"});
            fieldNameField.setText("");
            fieldTypeField.setText("");
            statusLabel.setText("Field added to new table definition.");
        });
        JButton createTableButton = UIComponentUtils.createFormattedButton("Create New Table");
        createTableButton.addActionListener(e -> createNewTable(newTableNameField));
        JButton renameColumnButton = UIComponentUtils.createFormattedButton("Rename Column");
        renameColumnButton.addActionListener(e -> renameColumn());
        JButton changeTypeButton = UIComponentUtils.createFormattedButton("Change Column Type");
        changeTypeButton.addActionListener(e -> changeColumnType());
        JButton moveLeftButton = UIComponentUtils.createFormattedButton("Move Left");
        moveLeftButton.addActionListener(e -> moveColumn(-1));
        JButton moveRightButton = UIComponentUtils.createFormattedButton("Move Right");
        moveRightButton.addActionListener(e -> moveColumn(1));
        JButton setPrimaryKeyButton = UIComponentUtils.createFormattedButton("Set Primary Key");
        setPrimaryKeyButton.addActionListener(e -> setPrimaryKey());
        JButton removePrimaryKeyButton = UIComponentUtils.createFormattedButton("Remove Primary Key");
        removePrimaryKeyButton.addActionListener(e -> removePrimaryKey());
        JButton addColumnButton = UIComponentUtils.createFormattedButton("Add Column to Table");
        addColumnButton.addActionListener(e -> addColumn());
        JButton deleteColumnButton = UIComponentUtils.createFormattedButton("Delete Column");
        deleteColumnButton.addActionListener(e -> deleteColumn());
        JButton deleteTableButton = UIComponentUtils.createFormattedButton("Delete Table");
        deleteTableButton.addActionListener(e -> deleteTable());

        buttonPanel.add(addFieldButton);
        buttonPanel.add(createTableButton);
        buttonPanel.add(renameColumnButton);
        buttonPanel.add(changeTypeButton);
        buttonPanel.add(moveLeftButton);
        buttonPanel.add(moveRightButton);
        buttonPanel.add(setPrimaryKeyButton);
        buttonPanel.add(removePrimaryKeyButton);
        buttonPanel.add(addColumnButton);
        buttonPanel.add(deleteColumnButton);
        buttonPanel.add(deleteTableButton);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(topPanel, BorderLayout.NORTH);
        controlPanel.add(inputPanel, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(controlPanel, BorderLayout.NORTH);

        // Fields table
        fieldsTable = new JTable(tableModel);
        JScrollPane tableScrollPane = UIComponentUtils.createScrollableContentPanel(fieldsTable);
        add(tableScrollPane, BorderLayout.CENTER);

        // Status panel
        statusLabel = new JLabel("Select a table to view or edit its schema.");
        JPanel statusPanel = new JPanel();
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        // Load table list
        loadTableList();
    }

    private void loadTableList() {
        tableComboBox.removeAllItems();
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
                while (rs.next()) {
                    tableComboBox.addItem(rs.getString("TABLE_NAME"));
                }
            }
            statusLabel.setText("Table list loaded.");
        } catch (SQLException e) {
            statusLabel.setText("Error loading tables: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error loading tables: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTableSchema() {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null) {
            tableModel.setRowCount(0);
            fields.clear();
            statusLabel.setText("No table selected.");
            return;
        }

        fields.clear();
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            // Get columns
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
            // Get primary keys
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
            statusLabel.setText("Schema loaded for table: " + tableName);
        } catch (SQLException e) {
            statusLabel.setText("Error loading schema: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error loading schema: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createNewTable(JTextField newTableNameField) {
        String tableName = newTableNameField.getText().trim();
        if (tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Table name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (fields.isEmpty()) {
            JOptionPane.showMessageDialog(this, "At least one field is required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE " + tableName + " (");
        for (int i = 0; i < fields.size(); i++) {
            Map<String, String> field = fields.get(i);
            String fieldName = field.get("name");
            String fieldType = field.get("type");
            createTableSQL.append(fieldName).append(" ").append(fieldType);
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
            JOptionPane.showMessageDialog(this, "Table '" + tableName + "' created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            newTableNameField.setText("");
            fields.clear();
            tableModel.setRowCount(0);
            loadTableList();
            statusLabel.setText("Table created: " + tableName);
        } catch (SQLException e) {
            statusLabel.setText("Error creating table: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error creating table: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renameColumn() {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a table and column to rename.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String oldName = (String) tableModel.getValueAt(selectedRow, 0);
        String newName = JOptionPane.showInputDialog(this, "Enter new column name for '" + oldName + "':");
        if (newName == null || newName.trim().isEmpty()) {
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ALTER COLUMN " + oldName + " RENAME TO " + newName);
            loadTableSchema();
            statusLabel.setText("Column renamed to: " + newName);
        } catch (SQLException e) {
            statusLabel.setText("Error renaming column: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error renaming column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changeColumnType() {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a table and column to change type.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        String newType = JOptionPane.showInputDialog(this, "Enter new type for '" + columnName + "' (e.g., VARCHAR(255), INTEGER):");
        if (newType == null || newType.trim().isEmpty()) {
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " " + newType);
            loadTableSchema();
            statusLabel.setText("Column type changed for: " + columnName);
        } catch (SQLException e) {
            statusLabel.setText("Error changing column type: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error changing column type: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void moveColumn(int direction) {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a table and column to move.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int newIndex = selectedRow + direction;
        if (newIndex < 0 || newIndex >= fields.size()) {
            JOptionPane.showMessageDialog(this, "Cannot move column beyond table bounds.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // MS Access doesn't support direct column reordering, so recreate the table
        try (Connection conn = DatabaseUtils.getConnection()) {
            // Get current schema
            List<Map<String, String>> newFields = new ArrayList<>(fields);
            Map<String, String> temp = newFields.get(selectedRow);
            newFields.set(selectedRow, newFields.get(newIndex));
            newFields.set(newIndex, temp);

            // Create new table with reordered columns
            String tempTable = tableName + "_temp";
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
                // Copy data
                StringBuilder columns = new StringBuilder();
                for (int i = 0; i < fields.size(); i++) {
                    columns.append(fields.get(i).get("name"));
                    if (i < fields.size() - 1) {
                        columns.append(", ");
                    }
                }
                stmt.executeUpdate("INSERT INTO " + tempTable + " (" + columns + ") SELECT " + columns + " FROM " + tableName);
                // Drop old table and rename new table
                stmt.executeUpdate("DROP TABLE " + tableName);
                stmt.executeUpdate("ALTER TABLE " + tempTable + " RENAME TO " + tableName);
            }
            loadTableSchema();
            loadTableList();
            statusLabel.setText("Column moved successfully.");
        } catch (SQLException e) {
            statusLabel.setText("Error moving column: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error moving column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setPrimaryKey() {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a table and column to set as primary key.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement()) {
            // Remove existing primary key if any
            try (ResultSet rs = conn.getMetaData().getPrimaryKeys(null, null, tableName)) {
                if (rs.next()) {
                    stmt.executeUpdate("ALTER TABLE " + tableName + " DROP CONSTRAINT PrimaryKey");
                }
            }
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD CONSTRAINT PrimaryKey PRIMARY KEY (" + columnName + ")");
            loadTableSchema();
            statusLabel.setText("Primary key set on: " + columnName);
        } catch (SQLException e) {
            statusLabel.setText("Error setting primary key: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error setting primary key: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removePrimaryKey() {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a table and column to remove primary key.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + tableName + " DROP CONSTRAINT PrimaryKey");
            loadTableSchema();
            statusLabel.setText("Primary key removed.");
        } catch (SQLException e) {
            statusLabel.setText("Error removing primary key: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error removing primary key: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addColumn() {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null) {
            JOptionPane.showMessageDialog(this, "Select a table to add a column.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String columnName = JOptionPane.showInputDialog(this, "Enter new column name:");
        if (columnName == null || columnName.trim().isEmpty()) {
            return;
        }
        String columnType = JOptionPane.showInputDialog(this, "Enter column type (e.g., VARCHAR(255), INTEGER):");
        if (columnType == null || columnType.trim().isEmpty()) {
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
            loadTableSchema();
            statusLabel.setText("Column added: " + columnName);
        } catch (SQLException e) {
            statusLabel.setText("Error adding column: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error adding column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteColumn() {
        String tableName = (String) tableComboBox.getSelectedItem();
        int selectedRow = fieldsTable.getSelectedRow();
        if (tableName == null || selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a table and column to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String columnName = (String) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete column '" + columnName + "'? This cannot be undone.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + tableName + " DROP COLUMN " + columnName);
            loadTableSchema();
            statusLabel.setText("Column deleted: " + columnName);
        } catch (SQLException e) {
            statusLabel.setText("Error deleting column: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error deleting column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteTable() {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null) {
            JOptionPane.showMessageDialog(this, "Select a table to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete table '" + tableName + "'? This cannot be undone.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE " + tableName);
            loadTableList();
            tableModel.setRowCount(0);
            fields.clear();
            statusLabel.setText("Table deleted: " + tableName);
        } catch (SQLException e) {
            statusLabel.setText("Error deleting table: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error deleting table: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}