package data_import;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataProcessor {
    private final List<utils.DataEntry> processedData = new ArrayList<>();
    private static final SimpleDateFormat[] dateFormats = {
        new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy"),
        new SimpleDateFormat("MM/dd/yyyy"),
        new SimpleDateFormat("dd-MM-yyyy"),
        new SimpleDateFormat("yyyy-MM-dd")
    };
    private static final SimpleDateFormat accessDateFormat = new SimpleDateFormat("MM/dd/yyyy");

    static {
        Logger.getLogger(DataProcessor.class.getName()).log(
            Level.INFO, "Initializing date formats: {0}", 
            java.util.Arrays.toString(dateFormats));
        for (SimpleDateFormat df : dateFormats) {
            df.setLenient(false);
        }
        accessDateFormat.setLenient(false);
    }

    public List<utils.DataEntry> processData(List<String[]> importedData, Map<String, String> columnMappings, 
            Map<String, String> deviceTypeMappings, String[] tableColumns) {
        processedData.clear();
        Logger.getLogger(DataProcessor.class.getName()).log(
            Level.INFO, "Date formats in use: {0}", 
            java.util.Arrays.toString(dateFormats));
        Map<String, Integer> normalizedHeaderMap = new HashMap<>();
        String[] headers = importedData.get(0);
        for (int i = 0; i < headers.length; i++) {
            normalizedHeaderMap.put(normalize.apply(headers[i]), i);
        }

        for (int i = 1; i < importedData.size(); i++) {
            String[] csvRow = importedData.get(i);
            String[] tableRow = new String[tableColumns.length];
            java.util.Arrays.fill(tableRow, "");
            HashMap<String, String> device = new HashMap<>();

            for (Map.Entry<String, String> mapping : columnMappings.entrySet()) {
                String csvColumn = mapping.getKey();
                String dbField = mapping.getValue();
                String normalizedCsvColumn = normalize.apply(csvColumn);
                Integer csvIndex = normalizedHeaderMap.get(normalizedCsvColumn);
                if (csvIndex != null && csvIndex < csvRow.length) {
                    String value = csvRow[csvIndex];
                    if (value == null) {
                        value = ""; // Handle null values from CSV
                    }
                    if (dbField.equals("Device_Type") && deviceTypeMappings.containsKey(value)) {
                        value = deviceTypeMappings.get(value);
                    }
                    if (dbField.equals("Created_at") || dbField.equals("Last_Successful_Scan")) {
                        if (value.trim().isEmpty()) {
                            value = null; // Set to null for empty date fields
                            Logger.getLogger(DataProcessor.class.getName()).log(
                                Level.INFO, "Empty date for {0} at row {1}. Setting to null.", 
                                new Object[]{dbField, i});
                        } else {
                            boolean parsed = false;
                            for (SimpleDateFormat df : dateFormats) {
                                try {
                                    Date date = df.parse(value);
                                    value = accessDateFormat.format(date); // Extract date only in MM/dd/yyyy
                                    parsed = true;
                                    break;
                                } catch (ParseException e) {
                                    Logger.getLogger(DataProcessor.class.getName()).log(
                                        Level.FINE, "Failed to parse {0} with format {1} at row {2}: {3}", 
                                        new Object[]{value, df.toPattern(), i, e.getMessage()});
                                }
                            }
                            if (!parsed) {
                                Logger.getLogger(DataProcessor.class.getName()).log(
                                    Level.WARNING, "Unparseable date for {0} at row {1}: {2}. Setting to null.", 
                                    new Object[]{dbField, i, value});
                                value = null; // Set to null for unparseable dates
                            }
                        }
                    }
                    for (int k = 0; k < tableColumns.length; k++) {
                        String normalizedTableColumn = tableColumns[k].replace(" ", "_");
                        if (normalizedTableColumn.equals(dbField)) {
                            tableRow[k] = value != null ? value : "";
                            device.put(normalizedTableColumn, value);
                            break;
                        }
                    }
                }
            }
            processedData.add(new utils.DataEntry(tableRow, device));
        }
        return processedData;
    }

    private final java.util.function.Function<String, String> normalize = s -> s.replaceAll("[\\s_-]", "").toLowerCase();
}