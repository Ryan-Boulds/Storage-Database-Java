package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        ArrayList<HashMap<String, String>> cables = new ArrayList<>();
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT Peripheral_Type, COUNT(*) AS Count FROM Cables GROUP BY Peripheral_Type")) {
            while (rs.next()) {
                HashMap<String, String> cable = new HashMap<>();
                String type = rs.getString("Peripheral_Type");
                if (type != null && !type.isEmpty()) {
                    cable.put("Peripheral_Type", type);
                    cable.put("Count", String.valueOf(rs.getInt("Count")));
                    cables.add(cable);
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Error loading cables: " + e.getMessage(), e);
        }
        return cables;
    }

    public static ArrayList<HashMap<String, String>> loadDevices() throws SQLException {
        return new ArrayList<>();
    }

    public static ArrayList<HashMap<String, String>> loadAccessories() throws SQLException {
        return new ArrayList<>();
    }

    public static ArrayList<String> loadTemplates() throws SQLException {
        return new ArrayList<>();
    }

    public static HashMap<String, String> loadTemplateDetails(String templateName) throws SQLException {
        return new HashMap<>();
    }
}