import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

public class ImportDataTab extends JPanel {
    private DefaultTableModel tableModel;
    private JTable table;

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
        String[] columns = {"Device Name", "Device Type", "Brand", "Model", "Serial Number", "Status", "Department", "Warranty Expiry", "Network Address", "Purchase Cost", "Vendor", "OS Version", "Assigned User", "Building Location", "Room/Desk", "Specification", "Added Memory", "Added Storage", "Last Maintenance", "Maintenance Due"};
        tableModel = new DefaultTableModel(columns, 0) {
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
                List<String[]> data = readCSVFile(file);  // Read .csv file
                updateTable(data); // Update the table with the imported data
                JOptionPane.showMessageDialog(this, "Data imported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
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
                // Pad rowData to match 20 columns
                String[] paddedRow = new String[20];
                for (int i = 0; i < 20; i++) {
                    paddedRow[i] = i < rowData.length ? rowData[i] : "";
                }
                data.add(paddedRow);
            }
        }
        return data;
    }

    // Method to update the table with the imported data
    private void updateTable(List<String[]> data) {
        tableModel.setRowCount(0);  // Clear current table data
        for (String[] row : data) {
            tableModel.addRow(row);
        }
    }
}