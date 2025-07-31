package software_data_importer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import utils.DataEntry;

public class DataProcessor {
    private static final SimpleDateFormat[] dateFormats = {
        new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy"),
        new SimpleDateFormat("MM/dd/yyyy"),
        new SimpleDateFormat("dd-MM-yyyy"),
        new SimpleDateFormat("yyyy-MM-dd")
    };
    private static final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    static {
        for (SimpleDateFormat df : dateFormats) {
            df.setLenient(false);
        }
        dbDateFormat.setLenient(false);
    }

    public List<DataEntry> processData(List<String[]> importedData, Map<String, String> columnMappings, 
                                      Map<String, String> deviceTypeMappings, String[] tableColumns, 
                                      Map<String, String> fieldTypes) {
        List<DataEntry> processedData = new ArrayList<>();
        if (importedData == null || importedData.isEmpty()) {
            return processedData;
        }

        // Get headers from the first row
        String[] headers = importedData.get(0);
        Map<Integer, String> columnIndexToDbField = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String mappedField = columnMappings.get(headers[i]);
            if (mappedField != null) {
                columnIndexToDbField.put(i, mappedField.replace(" ", "_"));
            }
        }

        // Convert tableColumns to use underscores
        String[] dbTableColumns = new String[tableColumns.length];
        for (int i = 0; i < tableColumns.length; i++) {
            dbTableColumns[i] = tableColumns[i].replace(" ", "_");
        }

        // Process each data row
        for (int i = 1; i < importedData.size(); i++) {
            String[] row = importedData.get(i);
            HashMap<String, String> deviceData = new HashMap<>();
            
            // Initialize all table columns with empty strings
            for (String column : dbTableColumns) {
                deviceData.put(column, "");
            }

            // Populate mapped fields
            for (Map.Entry<Integer, String> entry : columnIndexToDbField.entrySet()) {
                int colIndex = entry.getKey();
                String dbField = entry.getValue();
                if (colIndex < row.length) {
                    String value = row[colIndex] != null ? row[colIndex].trim() : "";
                    // Handle device type mappings for DeviceType field
                    if (dbField.equals("DeviceType") && deviceTypeMappings.containsKey(value)) {
                        value = deviceTypeMappings.get(value);
                    }
                    // Reformat date fields to yyyy-MM-dd
                    if (isDateField(dbField, fieldTypes)) {
                        value = normalizeDate(value, dbField);
                    }
                    deviceData.put(dbField, value);
                }
            }

            // Create DataEntry with values in table column order
            String[] values = new String[tableColumns.length];
            for (int j = 0; j < tableColumns.length; j++) {
                values[j] = deviceData.getOrDefault(dbTableColumns[j], "");
            }
            processedData.add(new DataEntry(values, deviceData));
        }

        return processedData;
    }

    private boolean isDateField(String field, Map<String, String> fieldTypes) {
        String fieldType = fieldTypes.getOrDefault(field, "");
        return fieldType.equalsIgnoreCase("DATE") || fieldType.equalsIgnoreCase("DATETIME");
    }

    private String normalizeDate(String dateStr, String field) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return "";
        }
        for (SimpleDateFormat df : dateFormats) {
            try {
                Date date = df.parse(dateStr);
                // Strip time component
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return dbDateFormat.format(calendar.getTime());
            } catch (ParseException e) {
                Logger.getLogger(DataProcessor.class.getName()).log(
                    Level.FINE, "Failed to parse date {0} for field {1} with format {2}: {3}",
                    new Object[]{dateStr, field, df.toPattern(), e.getMessage()});
            }
        }
        Logger.getLogger(DataProcessor.class.getName()).log(
            Level.WARNING, "Unparseable date for field {0}: {1}. Returning empty string.",
            new Object[]{field, dateStr});
        return "";
    }
}