package data_import;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import data_import.ui.MappingDialog;
import data_import.ui.PreviewDialog;

public class DataImporter {
    private final ImportDataTab parent;
    private final JLabel statusLabel;
    private final DatabaseHandler databaseHandler;
    private List<String[]> importedData;
    private static final Logger LOGGER = Logger.getLogger(DataImporter.class.getName());

    public DataImporter(ImportDataTab parent, JLabel statusLabel) {
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

        PreviewDialog previewDialog = new PreviewDialog(parent);
        importedData = previewDialog.showDialog();
        if (importedData == null || importedData.isEmpty()) {
            statusLabel.setText("No data loaded or import cancelled.");
            LOGGER.log(Level.WARNING, "No data loaded or import cancelled.");
            return;
        }

        statusLabel.setText("Data loaded for mapping: " + importedData.size() + " rows.");
        MappingDialog mappingDialog = new MappingDialog(parent, importedData);
        mappingDialog.showDialog();
        Map<String, String> columnMappings = mappingDialog.getColumnMappings();
        Map<String, String> newFields = mappingDialog.getNewFields();
        Map<String, String> deviceTypeMappings = mappingDialog.getDeviceTypeMappings();

        if (columnMappings.isEmpty()) {
            statusLabel.setText("Mapping cancelled or no columns mapped.");
            LOGGER.log(Level.WARNING, "Mapping cancelled or no columns mapped.");
            importedData = null;
            return;
        }

        statusLabel.setText("Import and mapping completed successfully.");
        LOGGER.log(Level.INFO, "Successfully imported and mapped data with {0} headers and {1} mappings.", 
                   new Object[]{importedData.get(0).length, columnMappings.size()});

        try {
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
        } catch (Exception e) {
            String errorMessage = "Error processing data: " + e.getMessage();
            statusLabel.setText(errorMessage);
            LOGGER.log(Level.SEVERE, "Error processing data: {0}", e.getMessage());
            JOptionPane.showMessageDialog(parent, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }

    public List<String[]> getImportedData() {
        return importedData;
    }
}