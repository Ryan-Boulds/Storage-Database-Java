package data_import;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

public class DataImporter {
    private final ImportDataTab parent;
    private final javax.swing.JLabel statusLabel;
    private final DatabaseHandler databaseHandler;
    private List<String[]> importedData;

    public DataImporter(ImportDataTab parent, javax.swing.JLabel statusLabel) {
        this.parent = parent;
        this.statusLabel = statusLabel;
        this.databaseHandler = new DatabaseHandler();
    }

    public void importData() {
        if (importedData != null && !importedData.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(parent,
                    "Are you sure that you want to import a different file? Any changes made here will be lost unless you save to database.",
                    "Confirm Import",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
                statusLabel.setText("Import cancelled.");
                return;
            }
        }

        // File selection
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV, Excel files", "csv", "xlsx", "xls"));
        int result = fileChooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            statusLabel.setText("No file selected.");
            return;
        }

        // Simulate file reading (replace with actual file parsing logic)
        List<String[]> data = new ArrayList<>();
        // Placeholder: Assume file is read into data (headers in data.get(0), rows follow)
        data.add(parent.getTableColumns()); // Example headers
        for (int i = 0; i < 10; i++) { // Example data
            String[] row = new String[parent.getTableColumns().length];
            for (int j = 0; j < row.length; j++) {
                row[j] = "Sample" + i + "_" + j;
            }
            data.add(row);
        }
        importedData = data;
        statusLabel.setText("Data loaded for mapping.");
        java.util.logging.Logger.getLogger(DataImporter.class.getName()).log(
            Level.INFO, "Imported Data Headers: {0}", new Object[]{java.util.Arrays.toString(importedData.get(0))});

        // Mapping dialog
        Map<String, String> columnMappings = new HashMap<>();
        Map<String, String> deviceTypeMappings = new HashMap<>();
        Map<String, String> newFields = new HashMap<>();
        JPanel mappingPanel = new JPanel(new java.awt.GridLayout(0, 2, 5, 5));
        for (String header : data.get(0)) {
            mappingPanel.add(new javax.swing.JLabel("Source: " + header));
            javax.swing.JComboBox<String> comboBox = new javax.swing.JComboBox<>(parent.getTableColumns());
            mappingPanel.add(comboBox);
        }
        int mappingResult = JOptionPane.showConfirmDialog(parent, mappingPanel, "Map Columns", JOptionPane.OK_CANCEL_OPTION);
        if (mappingResult == JOptionPane.OK_OPTION) {
            for (int i = 0; i < data.get(0).length; i++) {
                javax.swing.JComboBox<?> comboBox = (javax.swing.JComboBox<?>) mappingPanel.getComponent(2 * i + 1);
                columnMappings.put(data.get(0)[i], (String) comboBox.getSelectedItem());
            }
            statusLabel.setText("Mapping completed.");
            java.util.logging.Logger.getLogger(DataImporter.class.getName()).log(
                Level.INFO, "Column Mappings: {0}", new Object[]{columnMappings});

            // Process and display data
            parent.getOriginalData().clear();
            parent.getRowStatus().clear();
            parent.getFieldTypes().clear();
            parent.getFieldTypes().putAll(newFields);
            parent.getOriginalData().addAll(new DataProcessor().processData(data, columnMappings, deviceTypeMappings, parent.getTableColumns(), parent.getFieldTypes()));
            for (int i = 0; i < parent.getOriginalData().size(); i++) {
                String status = parent.dataDisplayManager.computeRowStatus(i, parent.getOriginalData().get(i));
                parent.getRowStatus().put(i, status);
            }
            parent.dataDisplayManager.displayData(columnMappings, newFields, deviceTypeMappings, data);
            statusLabel.setText("Data displayed for review.");
        } else {
            statusLabel.setText("Mapping cancelled.");
            importedData = null;
        }
    }

    public DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }

    public List<String[]> getImportedData() {
        return importedData;
    }
}