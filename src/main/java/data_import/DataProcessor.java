package data_import;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class DataProcessor {
    private final List<utils.DataEntry> processedData = new ArrayList<>();
    private final SimpleDateFormat[] dateFormats = {
        new SimpleDateFormat("yyyy-MM-dd"),
        new SimpleDateFormat("MM/dd/yyyy"),
        new SimpleDateFormat("dd-MM-yyyy")
    };

    public List<utils.DataEntry> processData(List<String[]> importedData, Map<String, String> columnMappings, 
            Map<String, String> deviceTypeMappings, String[] tableColumns) {
        processedData.clear();
        final java.util.function.Function<String, String> normalize = s -> s.replaceAll("[\\s_-]", "").toLowerCase();

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
                    if (dbField.equals("Device_Type") && deviceTypeMappings.containsKey(value)) {
                        value = deviceTypeMappings.get(value);
                    }
                    if ((dbField.equals("Created_at") || dbField.equals("Last_Successful_Scan")) && !value.trim().isEmpty()) {
                        boolean parsed = false;
                        for (SimpleDateFormat df : dateFormats) {
                            df.setLenient(false);
                            try {
                                java.util.Date date = df.parse(value);
                                value = new SimpleDateFormat("MM/dd/yyyy").format(date);
                                parsed = true;
                                break;
                            } catch (ParseException e) {
                                java.util.logging.Logger.getLogger(DataProcessor.class.getName()).log(Level.INFO, "Failed to parse {0} with format {1} at row {2}: {3}", new Object[]{value, df.toPattern(), i, e.getMessage()});
                            }
                        }
                        if (!parsed) {
                            java.util.logging.Logger.getLogger(DataProcessor.class.getName()).log(Level.INFO, "Unparseable date for {0} at row {1}: {2}. Using empty string.", new Object[]{dbField, i, value});
                            value = "";
                        }
                    }
                    for (int k = 0; k < tableColumns.length; k++) {
                        String normalizedTableColumn = tableColumns[k].replace(" ", "_");
                        if (normalizedTableColumn.equals(dbField)) {
                            tableRow[k] = value;
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

    public List<utils.DataEntry> getProcessedData() {
        return processedData;
    }
}