package software_data_importer.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import software_data_importer.ImportDataTab;
import utils.FileUtils;
import utils.UIComponentUtils;

public class PreviewDialog {
    private final ImportDataTab parent;
    private List<String[]> data;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final Logger LOGGER = Logger.getLogger(PreviewDialog.class.getName());

    public PreviewDialog(ImportDataTab parent) {
        this.parent = parent;
    }

    public List<String[]> showDialog() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV & Excel Files (*.csv, *.xlsx, *.xls)", "csv", "xlsx", "xls");
        fileChooser.setFileFilter(filter);
        int result = fileChooser.showOpenDialog(parent);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            data = readFile(file);
            if (data != null) {
                showPreviewDialog(file.getName());
                return data;
            }
        }
        return null;
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
            String errorMessage = "Error reading file " + fileName + ": " + e.getMessage();
            LOGGER.log(Level.SEVERE, errorMessage, e);
            JOptionPane.showMessageDialog(parent, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private List<String[]> convertCsvToArrayList(ArrayList<HashMap<String, String>> csvData) {
        List<String[]> result = new ArrayList<>();
        if (csvData.isEmpty()) return result;

        HashMap<String, String> firstRow = csvData.get(0);
        String[] headers = firstRow.keySet().toArray(new String[firstRow.size()]);
        result.add(headers);

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
        List<String[]> excelData = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) return excelData;

            Row headerRow = rowIterator.next();
            int colCount = headerRow.getPhysicalNumberOfCells();
            String[] headers = new String[colCount];
            for (int i = 0; i < colCount; i++) {
                headers[i] = getCellValue(headerRow.getCell(i));
            }
            excelData.add(headers);

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                String[] rowData = new String[colCount];
                for (int i = 0; i < colCount; i++) {
                    rowData[i] = getCellValue(row.getCell(i));
                }
                excelData.add(rowData);
            }
        }
        return excelData;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return dateFormat.format(cell.getDateCellValue());
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private void showPreviewDialog(String fileName) {
        if (data.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "The file is empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] csvColumns = data.get(0);
        List<String[]> dataRows = data.subList(1, data.size());

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
        previewScrollPane.setPreferredSize(new Dimension(800, 400));

        JPanel checkboxPanel = new JPanel(new GridLayout(0, 1));
        JCheckBox[] columnCheckboxes = new JCheckBox[csvColumns.length];
        for (int i = 0; i < csvColumns.length; i++) {
            columnCheckboxes[i] = new JCheckBox(csvColumns[i], true);
            checkboxPanel.add(columnCheckboxes[i]);
        }
        JScrollPane checkboxScrollPane = UIComponentUtils.createScrollableContentPanel(checkboxPanel);
        checkboxScrollPane.setPreferredSize(new Dimension(200, 400));

        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.add(previewScrollPane, BorderLayout.CENTER);
        dialogPanel.add(checkboxScrollPane, BorderLayout.EAST);

        int result = JOptionPane.showConfirmDialog(parent, dialogPanel,
                "Preview: " + fileName, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            List<Integer> selectedIndices = new ArrayList<>();
            for (int i = 0; i < columnCheckboxes.length; i++) {
                if (columnCheckboxes[i].isSelected()) {
                    selectedIndices.add(i);
                }
            }
            if (selectedIndices.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "No columns selected.", "Error", JOptionPane.ERROR_MESSAGE);
            } else if (selectedIndices.size() < csvColumns.length) {
                List<String[]> filteredData = new ArrayList<>();
                String[] headers = new String[selectedIndices.size()];
                for (int i = 0; i < selectedIndices.size(); i++) {
                    headers[i] = csvColumns[selectedIndices.get(i)];
                }
                filteredData.add(headers);
                for (int i = 1; i < data.size(); i++) {
                    String[] row = data.get(i);
                    String[] filteredRow = new String[selectedIndices.size()];
                    for (int j = 0; j < selectedIndices.size(); j++) {
                        filteredRow[j] = row[selectedIndices.get(j)];
                    }
                    filteredData.add(filteredRow);
                }
                data = filteredData;
            }
        } else {
            data = null;
        }
    }
}