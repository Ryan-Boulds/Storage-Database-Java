package database_creator.Table_Editor;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;

import utils.DatabaseUtils;

public class TableSchemaManager {
    private final JComboBox<String> tableComboBox;

    public TableSchemaManager(JComboBox<String> tableComboBox) {
        this.tableComboBox = tableComboBox;
    }

    public void loadTableList() throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, null, new String[]{"TABLE"});
            tableComboBox.removeAllItems();
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tables.add(tableName);
            }
            tables.sort(String.CASE_INSENSITIVE_ORDER);
            for (String tableName : tables) {
                tableComboBox.addItem(tableName);
            }
        }
    }
}