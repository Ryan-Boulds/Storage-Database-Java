import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

public class ImportDataTab extends JPanel {
    private DefaultTableModel tableModel;
    private JTable table;
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

    public ImportDataTab() {
        setLayout(new BorderLayout(10, 10));

        // Add Import Button
        JButton importButton = new JButton("Import CSV Data (.csv)");
        importButton.addActionListener(e -> importData());

        // Panel setup
        JPanel panel = new JPanel();
        panel.add(importButton);
        add(panel, BorderLayout.NORTH);

        // Table setup
        tableModel = new DefaultTableModel(tableColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Scrollable table
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        tableScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        tableScrollPane.getVerticalScrollBar().setBlockIncrement(60);
        tableScrollPane.setDoubleBuffered(true);

        add(tableScrollPane, BorderLayout.CENTER);
    }

    // Method to handle CSV file import
    private void importData() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files (*.csv)", "csv");
        fileChooser.setFileFilter(filter);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                List<String[]> data = readCSVFile(file);
                showPreviewDialog(data, file.getName());
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error reading the file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Method to read the CSV file
    private List<String[]> readCSVFile(File file) throws IOException {
        List<String[]> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Split the line by commas, handling quoted fields
                String[] rowData = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                // Remove quotes from quoted fields and trim
                for (int i = 0; i < rowData.length; i++) {
                    rowData[i] = rowData[i].replaceAll("^\"|\"$", "").trim();
                }
                data.add(rowData);
            }
        }
        return data;
    }

    // Show preview dialog with column selection
    private void showPreviewDialog(List<String[]> data, String fileName) {
        if (data.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The CSV file is empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] csvColumns = data.get(0); // Assume first row is headers
        List<String[]> dataRows = data.subList(1, Math.min(data.size(), 6)); // Show up to 5 rows for preview

        // Create table model for preview
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
        JScrollPane previewScrollPane = new JScrollPane(previewTable);
        previewScrollPane.setPreferredSize(new Dimension(600, 200));

        // Create checkboxes for column selection
        JPanel checkboxPanel = new JPanel(new GridLayout(0, 1));
        JCheckBox[] columnCheckboxes = new JCheckBox[csvColumns.length];
        for (int i = 0; i < csvColumns.length; i++) {
            columnCheckboxes[i] = new JCheckBox(csvColumns[i], true);
            checkboxPanel.add(columnCheckboxes[i]);
        }
        JScrollPane checkboxScrollPane = new JScrollPane(checkboxPanel);
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

    // Show dialog for mapping columns to database fields
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
            mappingPanel.add(UIUtils.createAlignedLabel(csvColumn + ":"));

            JComboBox<String> combo = UIUtils.createFormattedComboBox(dbFieldsWithNew);
            // Try to pre-select a matching database field
            String normalizedCsvColumn = csvColumn.replace(" ", "_").toLowerCase();
            for (String dbField : dbFields) {
                if (dbField.toLowerCase().equals(normalizedCsvColumn)) {
                    combo.setSelectedItem(dbField);
                    break;
                }
            }
            mappingCombos[i] = combo;

            JTextField newFieldField = UIUtils.createFormattedTextField();
            newFieldField.setVisible(false);
            newFieldFields[i] = newFieldField;

            JComboBox<String> typeCombo = UIUtils.createFormattedComboBox(new String[]{"VARCHAR(255)", "INTEGER", "DATE", "BOOLEAN"});
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

        JScrollPane mappingScrollPane = new JScrollPane(mappingPanel);
        mappingScrollPane.setPreferredSize(new Dimension(600, 300));

        int result = JOptionPane.showConfirmDialog(this, mappingScrollPane,
                "Map Columns to Database Fields", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            Map<Integer, String> columnMappings = new HashMap<>();
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
                            "Are you sure you want to add new field '" + newFieldName + "' with type " + typeCombos[i].getSelectedItem() + "?",
                            "Confirm New Field", JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                    newFields.put(newFieldName, (String) typeCombos[i].getSelectedItem());
                    columnMappings.put(colIndex, newFieldName);
                } else {
                    columnMappings.put(colIndex, selectedField);
                }
            }
            importSelectedData(data, columnMappings, newFields);
        }
    }

    // Import selected data and update the table
    private void importSelectedData(List<String[]> data, Map<Integer, String> columnMappings, Map<String, String> newFields) {
        List<String[]> tableData = new ArrayList<>();
        List<HashMap<String, String>> devicesToSave = new ArrayList<>();

        // Skip header row
        for (int i = 1; i < data.size(); i++) {
            String[] csvRow = data.get(i);
            String[] tableRow = new String[tableColumns.length];
            HashMap<String, String> device = new HashMap<>();

            // Initialize table row with empty strings
            for (int j = 0; j < tableColumns.length; j++) {
                tableRow[j] = "";
            }

            // Map CSV data to database fields
            for (Map.Entry<Integer, String> entry : columnMappings.entrySet()) {
                int csvColIndex = entry.getKey();
                String dbField = entry.getValue();
                String value = csvColIndex < csvRow.length ? csvRow[csvColIndex] : "";
                device.put(dbField, value);

                // Map to table columns if they match
                for (int j = 0; j < tableColumns.length; j++) {
                    if (tableColumns[j].replace(" ", "_").equals(dbField)) {
                        tableRow[j] = value;
                        break;
                    }
                }
            }

            // Validate device data
            String error = UIUtils.validateDevice(device);
            if (error != null) {
                JOptionPane.showMessageDialog(this, "Error in row " + i + ": " + error, "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            devicesToSave.add(device);
            tableData.add(tableRow);
        }

        // Save devices to UIUtils.getDevices()
        for (HashMap<String, String> device : devicesToSave) {
            UIUtils.saveDevice(device);
        }

        // Update table display
        tableModel.setRowCount(0);
        for (String[] row : tableData) {
            tableModel.addRow(row);
        }

        // Notify about new fields (for future SQL table updates)
        if (!newFields.isEmpty()) {
            StringBuilder newFieldsMessage = new StringBuilder("New fields added (update database schema):\n");
            for (Map.Entry<String, String> entry : newFields.entrySet()) {
                newFieldsMessage.append(entry.getKey()).append(" (").append(entry.getValue()).append(")\n");
            }
            JOptionPane.showMessageDialog(this, newFieldsMessage.toString(), "New Fields Added", JOptionPane.INFORMATION_MESSAGE);
        }

        JOptionPane.showMessageDialog(this, "Data imported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}