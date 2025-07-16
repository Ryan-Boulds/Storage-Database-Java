package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class FileUtils {
    public static ArrayList<HashMap<String, String>> readCSVFile(java.io.File file) throws IOException {
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(file.getPath()), StandardCharsets.UTF_8)) {
            String[] headers = br.readLine().split(",");
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                HashMap<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], i < values.length ? values[i] : "");
                }
                data.add(row);
            }
        }
        return data;
    }

    public static ArrayList<HashMap<String, String>> loadDevices() throws SQLException {
        return DatabaseUtils.loadDevices();
    }

    public static ArrayList<HashMap<String, String>> loadCables() throws SQLException {
        return DatabaseUtils.loadPeripherals("Cable");
    }

    public static ArrayList<HashMap<String, String>> loadAccessories() throws SQLException {
        return DatabaseUtils.loadPeripherals("Accessory");
    }

    public static ArrayList<String> loadTemplates() throws SQLException {
        ArrayList<HashMap<String, String>> templateData = DatabaseUtils.loadTemplates();
        ArrayList<String> templateNames = new ArrayList<>();
        for (HashMap<String, String> template : templateData) {
            String templateName = template.get("Template_Name");
            if (templateName != null && !templateName.isEmpty()) {
                templateNames.add(templateName);
            }
        }
        return templateNames;
    }

    public static HashMap<String, String> loadTemplateDetails(String templateName) throws SQLException {
        return DatabaseUtils.loadTemplateDetails(templateName);
    }
}