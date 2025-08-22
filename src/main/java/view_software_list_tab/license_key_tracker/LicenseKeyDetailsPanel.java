package view_software_list_tab.license_key_tracker;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import utils.DatabaseUtils;
import view_software_list_tab.TableManager;

public class LicenseKeyDetailsPanel extends JDialog {
    private final String licenseKey;
    private final TableManager tableManager;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JTextField searchField;
    private final TableRowSorter<DefaultTableModel> sorter;
    private static final Logger LOGGER = Logger.getLogger(LicenseKeyDetailsPanel.class.getName());

    public LicenseKeyDetailsPanel(LicenseKeyTracker parent, String licenseKey, TableManager tableManager) {
        super(); // Use default JDialog constructor
        setTitle("License Key Details: " + licenseKey);
        setModal(true);
        this.licenseKey = licenseKey;
        this.tableManager = tableManager;
        this.tableModel = new DefaultTableModel();
        this.table = new JTable(tableModel);
        this.searchField = new JTextField(20);
        this.sorter = new TableRowSorter<>(tableModel);
        initializeUI();
        loadKeyEntries();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(800, 400);
        setLocationRelativeTo(null);

        // Search bar panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel searchLabel = new JLabel("Search:");
        topPanel.add(searchLabel);
        topPanel.add(searchField);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        add(topPanel, BorderLayout.NORTH);

        // Table setup
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowSorter(sorter);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new BorderLayout());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton, BorderLayout.EAST);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadKeyEntries() {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        String[] columns = tableManager.getColumns();
        for (String column : columns) {
            tableModel.addColumn(column);
        }

        String tableName = tableManager.getTableName();
        String licenseKeyColumn = null;
        for (String column : columns) {
            if (column.equalsIgnoreCase("License_Key")) {
                licenseKeyColumn = column;
                break;
            }
        }

        if (licenseKeyColumn == null) {
            LOGGER.log(Level.SEVERE, "License_Key column not found in table '{0}'", tableName);
            JOptionPane.showMessageDialog(this, String.format("Error: License_Key column not found in table '%s'", tableName), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Map to store usage counts for sorting
        Map<String, Integer> usageCounts = new HashMap<>();
        List<Object[]> rows = new ArrayList<>();

        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM [" + tableName + "] WHERE [" + licenseKeyColumn + "] = ?")) {
            stmt.setString(1, licenseKey);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[columns.length];
                    String assetName = null;
                    for (int i = 0; i < columns.length; i++) {
                        row[i] = rs.getString(columns[i]);
                        if (columns[i].equalsIgnoreCase("AssetName")) {
                            assetName = rs.getString(columns[i]);
                        }
                    }
                    // Count occurrences of this AssetName for sorting
                    if (assetName != null) {
                        usageCounts.put(assetName, usageCounts.getOrDefault(assetName, 0) + 1);
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching entries for license key '{0}' in table '{1}': {2}", 
                new Object[]{licenseKey, tableName, e.getMessage()});
            JOptionPane.showMessageDialog(this, String.format("Error fetching entries: %s", e.getMessage()), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Sort rows by usage count (descending) and AssetName (alphabetically)
        rows.sort((row1, row2) -> {
            String assetName1 = null, assetName2 = null;
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].equalsIgnoreCase("AssetName")) {
                    assetName1 = (String) row1[i];
                    assetName2 = (String) row2[i];
                    break;
                }
            }
            if (assetName1 == null || assetName2 == null) {
                return 0; // Fallback if AssetName not found
            }
            int count1 = usageCounts.getOrDefault(assetName1, 0);
            int count2 = usageCounts.getOrDefault(assetName2, 0);
            if (count1 != count2) {
                return Integer.compare(count2, count1); // Descending order
            }
            return assetName1.compareToIgnoreCase(assetName2); // Alphabetical for ties
        });

        // Add sorted rows to table model
        for (Object[] row : rows) {
            tableModel.addRow(row);
        }

        // Adjust column widths
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            String header = columns[i];
            int maxWidth = table.getFontMetrics(table.getFont()).stringWidth(header) + 20;
            for (int row = 0; row < table.getRowCount(); row++) {
                Object value = table.getValueAt(row, i);
                String text = value != null ? value.toString() : "";
                int textWidth = table.getFontMetrics(table.getFont()).stringWidth(text) + 20;
                maxWidth = Math.max(maxWidth, textWidth);
            }
            column.setPreferredWidth(maxWidth);
        }

        LOGGER.log(Level.INFO, "Loaded {0} entries for license key '{1}' in table '{2}'", 
            new Object[]{rows.size(), licenseKey, tableName});
    }

    private void applyFilter() {
        String text = searchField.getText().toLowerCase();
        RowFilter<DefaultTableModel, Integer> filter = null;
        if (!text.isEmpty()) {
            filter = new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    for (int i = 0; i < entry.getModel().getColumnCount(); i++) {
                        Object value = entry.getValue(i);
                        if (value != null && value.toString().toLowerCase().contains(text)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }
        sorter.setRowFilter(filter);
    }

    public void showPanel() {
        setVisible(true);
    }
}