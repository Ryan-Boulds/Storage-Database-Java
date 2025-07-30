package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class FileUtils {
    public static ArrayList<HashMap<String, String>> readCSVFile(File file) throws IOException {
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) throw new IOException("Empty CSV file");
            String[] headers = headerLine.split(",");
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length == headers.length) {
                    HashMap<String, String> row = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        row.put(headers[i].trim(), values[i].trim());
                    }
                    data.add(row);
                }
            }
        }
        return data;
    }

    public static ArrayList<HashMap<String, String>> loadCables() throws SQLException {
        return DatabaseUtils.loadPeripherals("Cable");
    }

    public static ArrayList<HashMap<String, String>> loadDevices() throws SQLException {
        return DatabaseUtils.loadDevices("Inventory");
    }

    public static ArrayList<HashMap<String, String>> loadAccessories() throws SQLException {
        return DatabaseUtils.loadPeripherals("Accessory");
    }

    public static ArrayList<HashMap<String, String>> loadAdapters() throws SQLException {
        return DatabaseUtils.loadPeripherals("Adapter");
    }

    public static ArrayList<String> loadTemplates() throws SQLException {
        ArrayList<String> templateNames = new ArrayList<>();
        for (HashMap<String, String> template : DatabaseUtils.loadTemplates()) {
            String templateName = template.getOrDefault("Template_Name", "");
            if (!templateName.isEmpty()) {
                templateNames.add(templateName);
            }
        }
        return templateNames;
    }

    public static HashMap<String, String> loadTemplateDetails(String templateName) throws SQLException {
        return DatabaseUtils.loadTemplateDetails(templateName);
    }
}