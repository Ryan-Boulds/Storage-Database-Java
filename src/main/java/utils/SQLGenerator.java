package utils;

import java.util.Map;

public class SQLGenerator {
    public static String generateInsertSQL(String tableName, Map<String, String> data) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        boolean first = true;
        for (String key : data.keySet()) {
            if (!first) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append("[").append(key).append("]");
            values.append("?");
            first = false;
        }
        return "INSERT INTO [" + tableName + "] (" + columns + ") VALUES (" + values + ")";
    }

    public static String generateUpdateSQL(String tableName, Map<String, String> data) {
        StringBuilder setClause = new StringBuilder();
        boolean first = true;
        for (String key : data.keySet()) {
            if (!key.equals("AssetName")) {
                if (!first) {
                    setClause.append(", ");
                }
                setClause.append("[").append(key).append("] = ?");
                first = false;
            }
        }
        return "UPDATE [" + tableName + "] SET " + setClause + " WHERE AssetName = ?";
    }
}