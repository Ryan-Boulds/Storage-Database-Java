package database_creator.Table_Editor;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JComboBox;

import utils.DatabaseUtils;

public class TableSchemaManager {
    private final TableEditor editor;
    private final JComboBox<String> tableComboBox;

    public TableSchemaManager(TableEditor editor, JComboBox<String> tableComboBox) {
        this.editor = editor;
        this.tableComboBox = tableComboBox;
    }

    public void loadTableList() {
        String selectedTable = (String) tableComboBox.getSelectedItem();
        tableComboBox.removeAllItems();
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
                while (rs.next()) {
                    tableComboBox.addItem(rs.getString("TABLE_NAME"));
                }
            }
            if (selectedTable != null && tableComboBox.getItemCount() > 0) {
                tableComboBox.setSelectedItem(selectedTable);
            }
            editor.showMessageDialog("Status", "Table list loaded.", 1);
        } catch (SQLException e) {
            editor.showMessageDialog("Error", "Error loading tables: " + e.getMessage(), 0);
        }
    }
}