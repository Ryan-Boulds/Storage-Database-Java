package device_logging;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import utils.DataUtils;
import utils.DatabaseUtils;
import utils.FileUtils;
import utils.UIComponentUtils;

public class ImportDataTab extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final String[] dbFields = {
        "Device_Name", "Device_Type", "Brand", "Model", "Serial_Number", "Building_Location",
        "Room_Desk", "Specification", "Processor_Type", "Storage_Capacity", "Network_Address",
        "OS_Version", "Department", "Added_Memory", "Status", "Assigned_User",
        "Warranty_Expiry_Date", "Last_Maintenance", "Maintenance_Due", "Date_Of_Purchase",
        "Purchase_Cost", "Vendor", "Memory_RAM"
    };
    private final String[] tableColumns = {
        "Device Name", "Device Type", "Brand", "Model", "Serial Number", "Status",
        "Department", "Warranty Expiry", "Network Address", "Purchase Cost", "Vendor",
        "OS Version", "Assigned User", "Building Location", "Room/Desk", "Specification",
        "Added Memory", "Added Storage", "Last Maintenance", "Maintenance Due", "Memory (RAM)"
    };
    private Map<String, String> columnMappings = new HashMap<>();
    private List<String[]> importedData;

    public ImportDataTab(JLabel statusLabel) {
        setLayout(new BorderLayout(10, 10));

        JButton importButton = UIComponentUtils.createFormattedButton("Import Data (.csv, .xlsx, .xls)");
        importButton.addActionListener(e -> importData());

        JButton saveButton = UIComponentUtils.createFormattedButton("Save to Database");
        saveButton.addActionListener(e -> saveToDatabase(statusLabel));

        JButton viewMappingsButton = UIComponentUtils.createFormattedButton("View Current Mappings");
        viewMappingsButton.addActionListener(e -> showCurrentMappings());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(importButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(viewMappingsButton);
        add(buttonPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(tableColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane tableScrollPane = UIComponentUtils.createScrollableContentPanel(table);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    private void importData() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV & Excel Files (*.csv, *.xlsx, *.xls)", "csv", "xlsx", "xls");
        fileChooser.setFileFilter(filter);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            List<String[]> data = readFile(file);
            if (data != null) {
                importedData = data;
                showPreviewDialog(data, file.getName());
            }
        }
    }

    private List<String[]> readFile(File file) {
        String fileName = file.getName().toLowerCase();
        try {
            if (fileName.endsWith(".csv")) {
                ArrayList<HashMap<String, String>> csvData = FileUtils.readCSVFile(file);
                return convertCsvToArrayList(csvData);
            } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                return readExcelFile(file);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private List<String[]> convertCsvToArrayList(ArrayList<HashMap<String, String>> csvData) {
        List<String[]> result = new ArrayList<>();
        if (csvData.isEmpty()) return result;

        // Extract headers from the first row
        HashMap<String, String> firstRow = csvData.get(0);
        String[] headers = firstRow.keySet().toArray(new String[0]);
        result.add(headers);

        // Convert each HashMap to a String array
        for (HashMap<String, String> row : csvData) {
            String[] rowData = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                rowData[i] = row.getOrDefault(headers[i], "");
            }
            result.add(rowData);
        }
        return result;
    }

    private List<String[]> readExcelFile(File file) throws IOException {
        List<String[]> data = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) return data;

            Row headerRow = rowIterator.next();
            int colCount = headerRow.getPhysicalNumberOfCells();
            String[] headers = new String[colCount];
            for (int i = 0; i < colCount; i++) {
                headers[i] = getCellValue(headerRow.getCell(i));
            }
            data.add(headers);

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                String[] rowData = new String[colCount];
                for (int i = 0; i < colCount; i++) {
                    rowData[i] = getCellValue(row.getCell(i));
                }
                data.add(rowData);
            }
        }
        return data;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private void showPreviewDialog(List<String[]> data, String fileName) {
        if (data.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The file is empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] csvColumns = data.get(0);
        List<String[]> dataRows = data.subList(1, Math.min(data.size(), 6));

        DefaultTableModel previewModel = new DefaultTableModel(csvColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (String[] row : dataRows) {
            String[] paddedRow = new String[csvColumns.length];
            for (int i = 0; i < csvColumns.length; i++) {
                paddedRow[i] = i < row.length ? row[i] : "";
            }
            previewModel.addRow(paddedRow);
        }

        JTable previewTable = new JTable(previewModel);
        JScrollPane previewScrollPane = UIComponentUtils.createScrollableContentPanel(previewTable);
        previewScrollPane.setPreferredSize(new Dimension(600, 200));

        JPanel checkboxPanel = new JPanel(new GridLayout(0, 1));
        JCheckBox[] columnCheckboxes = new JCheckBox[csvColumns.length];
        for (int i = 0; i < csvColumns.length; i++) {
            columnCheckboxes[i] = new JCheckBox(csvColumns[i], true);
            checkboxPanel.add(columnCheckboxes[i]);
        }
        JScrollPane checkboxScrollPane = UIComponentUtils.createScrollableContentPanel(checkboxPanel);
        checkboxScrollPane.setPreferredSize(new Dimension(200, 200));

        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.add(previewScrollPane, BorderLayout.CENTER);
        dialogPanel.add(checkboxScrollPane, BorderLayout.EAST);

        int result = JOptionPane.showConfirmDialog(this, dialogPanel,
                "Preview: " + fileName, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            List<Integer> selectedIndices = new ArrayList<>();
            for (int i = 0; i < columnCheckboxes.length; i++) {
                if (columnCheckboxes[i].isSelected()) {
                    selectedIndices.add(i);
                }
            }
            if (selectedIndices.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No columns selected.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            showMappingDialog(data, selectedIndices, csvColumns);
        }
    }

    private void showMappingDialog(List<String[]> data, List<Integer> selectedIndices, String[] csvColumns) {
        JPanel mappingPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        @SuppressWarnings("unchecked")
        JComboBox<String>[] mappingCombos = new JComboBox[selectedIndices.size()];
        JTextField[] newFieldFields = new JTextField[selectedIndices.size()];
        @SuppressWarnings("unchecked")
        JComboBox<String>[] typeCombos = new JComboBox[selectedIndices.size()];
        String[] dbFieldsWithNew = Arrays.copyOf(dbFields, dbFields.length + 1);
        dbFieldsWithNew[dbFields.length] = "Add New Field";

        for (int i = 0; i < selectedIndices.size(); i++) {
            int colIndex = selectedIndices.get(i);
            String csvColumn = csvColumns[colIndex];
            mappingPanel.add(UIComponentUtils.createAlignedLabel(csvColumn + ":"));

            JComboBox<String> combo = UIComponentUtils.createFormattedComboBox(dbFieldsWithNew);
            String normalizedCsvColumn = DataUtils.normalizeColumnName(csvColumn);
            for (String dbField : dbFields) {
                if (dbField.toLowerCase().equals(normalizedCsvColumn) ||
                    dbField.toLowerCase().replace("_", "").equals(normalizedCsvColumn.replace(" ", ""))) {
                    combo.setSelectedItem(dbField);
                    break;
                }
            }
            mappingCombos[i] = combo;

            JTextField newFieldField = UIComponentUtils.createFormattedTextField();
            newFieldField.setVisible(false);
            newFieldFields[i] = newFieldField;

            JComboBox<String> typeCombo = UIComponentUtils.createFormattedComboBox(new String[]{"VARCHAR(255)", "INTEGER", "DATE", "BOOLEAN"});
            typeCombo.setVisible(false);
            typeCombos[i] = typeCombo;

            JPanel newFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            newFieldPanel.add(newFieldField);
            newFieldPanel.add(typeCombo);
            mappingPanel.add(newFieldPanel);

            combo.addActionListener(e -> {
                boolean isNewField = combo.getSelectedItem().equals("Add New Field");
                newFieldField.setVisible(isNewField);
                typeCombo.setVisible(isNewField);
                mappingPanel.revalidate();
                mappingPanel.repaint();
            });
        }

        JScrollPane mappingScrollPane = UIComponentUtils.createScrollableContentPanel(mappingPanel);
        mappingScrollPane.setPreferredSize(new Dimension(600, 300));

        int result = JOptionPane.showConfirmDialog(this, mappingScrollPane,
                "Map Columns to Database Fields", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            columnMappings.clear();
            Map<String, String> newFields = new HashMap<>();
            for (int i = 0; i < selectedIndices.size(); i++) {
                int colIndex = selectedIndices.get(i);
                String selectedField = (String) mappingCombos[i].getSelectedItem();
                if (selectedField.equals("Add New Field")) {
                    String newFieldName = newFieldFields[i].getText().trim();
                    if (newFieldName.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "New field name cannot be empty for column: " + csvColumns[colIndex], "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (Arrays.asList(dbFields).contains(newFieldName)) {
                        JOptionPane.showMessageDialog(this, "Field name '" + newFieldName + "' already exists in the database.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Add new field '" + newFieldName + "' with type " + typeCombos[i].getSelectedItem() + "?",
                            "Confirm New Field", JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                    newFields.put(newFieldName, (String) typeCombos[i].getSelectedItem());
                    columnMappings.put(csvColumns[colIndex], newFieldName);
                } else {
                    columnMappings.put(csvColumns[colIndex], selectedField);
                }
            }
            displayData(data, selectedIndices, columnMappings, newFields);
        }
    }

    private void displayData(List<String[]> data, List<Integer> selectedIndices, Map<String, String> columnMappings, Map<String, String> newFields) {
        tableModel.setRowCount(0);

        for (Map.Entry<String, String> entry : newFields.entrySet()) {
            try {
                DatabaseUtils.addNewField("Inventory", entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding new field '" + entry.getKey() + "': " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        for (int i = 1; i < data.size(); i++) {
            String[] csvRow = data.get(i);
            String[] tableRow = new String[tableColumns.length];
            Arrays.fill(tableRow, "");

            for (int j = 0; j < selectedIndices.size(); j++) {
                int csvColIndex = selectedIndices.get(j);
                String csvColumn = data.get(0)[csvColIndex];
                String dbField = columnMappings.get(csvColumn);
                String value = csvColIndex < csvRow.length ? csvRow[csvColIndex] : "";

                for (int k = 0; k < tableColumns.length; k++) {
                    if (tableColumns[k].replace(" ", "_").equals(dbField)) {
                        tableRow[k] = value;
                        break;
                    }
                }
            }
            tableModel.addRow(tableRow);
        }

        if (!newFields.isEmpty()) {
            StringBuilder newFieldsMessage = new StringBuilder("New fields added to database schema:\n");
            for (Map.Entry<String, String> entry : newFields.entrySet()) {
                newFieldsMessage.append(entry.getKey()).append(" (").append(entry.getValue()).append(")\n");
            }
            JOptionPane.showMessageDialog(this, newFieldsMessage.toString(), "New Fields Added", JOptionPane.INFORMATION_MESSAGE);
        }

        JOptionPane.showMessageDialog(this, "Data loaded for review. Edit as needed and click 'Save to Database'.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveToDatabase(JLabel statusLabel) {
        if (importedData == null || importedData.isEmpty()) {
            statusLabel.setText("No data to save.");
            JOptionPane.showMessageDialog(this, "No data to save.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<HashMap<String, String>> devicesToSave = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            HashMap<String, String> device = new HashMap<>();
            for (int j = 0; j < tableColumns.length; j++) {
                String dbField = tableColumns[j].replace(" ", "_");
                String value = (String) tableModel.getValueAt(i, j);
                if (value != null && !value.trim().isEmpty()) {
                    device.put(dbField, value);
                }
            }

            String serialNumber = device.get("Serial_Number");
            String error = DataUtils.validateDevice(device, serialNumber);
            if (error != null) {
                statusLabel.setText("Error in row " + (i + 1) + ": " + error);
                JOptionPane.showMessageDialog(this, "Error in row " + (i + 1) + ": " + error, "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            devicesToSave.add(device);
        }

        try {
            for (HashMap<String, String> device : devicesToSave) {
                DatabaseUtils.saveDevice(device);
            }
            statusLabel.setText("Data saved to database successfully!");
            JOptionPane.showMessageDialog(this, "Data saved to database successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            statusLabel.setText("Error saving to database: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error saving to database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showCurrentMappings() {
        if (columnMappings.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No mappings defined.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel mappingPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        for (Map.Entry<String, String> entry : columnMappings.entrySet()) {
            mappingPanel.add(UIComponentUtils.createAlignedLabel("Source: " + entry.getKey()));
            mappingPanel.add(UIComponentUtils.createAlignedLabel("Maps to: " + entry.getValue()));
        }

        JScrollPane mappingScrollPane = UIComponentUtils.createScrollableContentPanel(mappingPanel);
        mappingScrollPane.setPreferredSize(new Dimension(400, 200));

        JOptionPane.showMessageDialog(this, mappingScrollPane, "Current Column Mappings", JOptionPane.INFORMATION_MESSAGE);
    }
}