package utils;

import java.util.Map;

public class SQLGenerator {
    public static String generateInsertSQL(String tableName, Map<String, String> data) {
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        for (String column : data.keySet()) {
            if (columns.length() > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append("[").append(column).append("]");
            placeholders.append("?");
        }
        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
    }

    public static String formatDeviceSQL(Map<String, String> data) {
        return generateInsertSQL("Inventory", data);
    }
}