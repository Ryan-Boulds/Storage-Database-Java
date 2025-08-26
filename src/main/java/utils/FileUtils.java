package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUtils {
    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

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
        ArrayList<HashMap<String, String>> cables = DatabaseUtils.loadPeripherals("Cable");
        LOGGER.log(Level.INFO, "loadCables: Retrieved {0} cables", cables.size());
        return cables;
    }

    public static ArrayList<HashMap<String, String>> loadDevices() throws SQLException {
        ArrayList<HashMap<String, String>> devices = DatabaseUtils.loadDevices("Computers");
        LOGGER.log(Level.INFO, "loadDevices: Retrieved {0} devices", devices.size());
        return devices;
    }

    public static ArrayList<HashMap<String, String>> loadAccessories() throws SQLException {
        ArrayList<HashMap<String, String>> accessories = DatabaseUtils.loadPeripherals("Accessory");
        LOGGER.log(Level.INFO, "loadAccessories: Retrieved {0} accessories", accessories.size());
        return accessories;
    }

    public static ArrayList<HashMap<String, String>> loadAdapters() throws SQLException {
        ArrayList<HashMap<String, String>> adapters = DatabaseUtils.loadPeripherals("Adapter");
        LOGGER.log(Level.INFO, "loadAdapters: Retrieved {0} adapters", adapters.size());
        return adapters;
    }

    public static ArrayList<HashMap<String, String>> loadChargers() throws SQLException {
        ArrayList<HashMap<String, String>> chargers = DatabaseUtils.loadPeripherals("Charger");
        LOGGER.log(Level.INFO, "loadChargers: Retrieved {0} chargers", chargers.size());
        return chargers;
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