package data_import;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import data_import.ui.MappingDialog;
import data_import.ui.PreviewDialog;

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

        // Use PreviewDialog to read the file and allow column selection
        PreviewDialog previewDialog = new PreviewDialog(parent);
        importedData = previewDialog.showDialog();
        if (importedData == null || importedData.isEmpty()) {
            statusLabel.setText("No data loaded or import cancelled.");
            return;
        }

        statusLabel.setText("Data loaded for mapping.");
        java.util.logging.Logger.getLogger(DataImporter.class.getName()).log(
            Level.INFO, "Imported Data Headers: {0}", new Object[]{java.util.Arrays.toString(importedData.get(0))});

        // Use MappingDialog for column mapping
        MappingDialog mappingDialog = new MappingDialog(parent, importedData);
        mappingDialog.showDialog();
        Map<String, String> columnMappings = mappingDialog.getColumnMappings();
        Map<String, String> newFields = mappingDialog.getNewFields();
        Map<String, String> deviceTypeMappings = mappingDialog.getDeviceTypeMappings();

        if (columnMappings.isEmpty()) {
            statusLabel.setText("Mapping cancelled or no columns mapped.");
            importedData = null;
            return;
        }

        statusLabel.setText("Mapping completed.");
        java.util.logging.Logger.getLogger(DataImporter.class.getName()).log(
            Level.INFO, "Column Mappings: {0}", new Object[]{columnMappings});

        // Process and display data
        parent.getOriginalData().clear();
        parent.getRowStatus().clear();
        parent.getFieldTypes().clear();
        parent.getFieldTypes().putAll(newFields);
        parent.getOriginalData().addAll(new DataProcessor().processData(
            importedData, columnMappings, deviceTypeMappings, parent.getTableColumns(), parent.getFieldTypes()));
        for (int i = 0; i < parent.getOriginalData().size(); i++) {
            String status = parent.dataDisplayManager.computeRowStatus(i, parent.getOriginalData().get(i));
            parent.getRowStatus().put(i, status);
        }
        parent.dataDisplayManager.displayData(columnMappings, newFields, deviceTypeMappings, importedData);
        statusLabel.setText("Data displayed for review.");
    }

    public DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }

    public List<String[]> getImportedData() {
        return importedData;
    }
}