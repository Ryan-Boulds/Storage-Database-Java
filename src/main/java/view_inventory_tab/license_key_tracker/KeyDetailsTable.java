package view_inventory_tab.license_key_tracker;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.swing.table.TableRowSorter;

import utils.DatabaseUtils;
import view_inventory_tab.PopupHandler;
import view_inventory_tab.TableManager;
import view_inventory_tab.ViewInventoryTab;

@SuppressWarnings({"unused", "OverridableMethodCallInConstructor"})
public class KeyDetailsTable extends JPanel {
    private final String licenseKey;
    private final TableManager tableManager;
    private final LicenseKeyTracker licenseKeyTracker;
    private final ViewInventoryTab parentTab;
    private final JTable table;
    private final JTextField searchField;
    private static final Logger LOGGER = Logger.getLogger(KeyDetailsTable.class.getName());

    public KeyDetailsTable(String licenseKey, TableManager tableManager, LicenseKeyTracker licenseKeyTracker, ViewInventoryTab parentTab) {
        this.licenseKey = licenseKey;
        this.parentTab = parentTab;
        this.licenseKeyTracker = licenseKeyTracker;
        this.tableManager = tableManager;
        this.table = tableManager.getTable();
        this.searchField = new JTextField(20);
        this.tableManager.setLicenseKeyTracker(licenseKeyTracker);
        setLayout(new BorderLayout());
        LOGGER.log(Level.INFO, "KeyDetailsTable initialized for licenseKey='{0}', tableName='{1}'", 
            new Object[]{licenseKey, tableManager.getTableName()});
        initializeUI();
        loadData();
    }

    private void initializeUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel("Entries for License Key: " + (licenseKey != null ? licenseKey : "None"));
        topPanel.add(titleLabel);
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

        table.setCellSelectionEnabled(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        PopupHandler.addTablePopup(table, parentTab);
        LOGGER.log(Level.INFO, "UI initialized for KeyDetailsTable, tableName='{0}'", tableManager.getTableName());
    }

    public void loadData() {
    String tableName = tableManager.getTableName();
    String licenseKeyColumn = findLicenseKeyColumn();
    if (licenseKeyColumn == null) {
        LOGGER.log(Level.WARNING, "No License_Key column found in table '{0}', keeping table empty", tableName);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        return;
    }

    DefaultTableModel model = (DefaultTableModel) table.getModel();
    model.setRowCount(0);

    LOGGER.log(Level.INFO, "Table columns: {0}, column count: {1}", 
        new Object[]{String.join(", ", tableManager.getColumns()), model.getColumnCount()});

    if (licenseKey == null) {
        LOGGER.log(Level.INFO, "No licenseKey provided, keeping table empty");
        return;
    }

    String query = "SELECT * FROM [" + tableName + "] WHERE [" + licenseKeyColumn + "] = ?";
    LOGGER.log(Level.INFO, "Executing query: {0} with licenseKey='{1}'", new Object[]{query, licenseKey});
    try (Connection conn = DatabaseUtils.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, licenseKey);
        try (ResultSet rs = stmt.executeQuery()) {
            int rowCount = 0;
            while (rs.next()) {
                Object[] row = new Object[model.getColumnCount()];
                for (int i = 0; i < model.getColumnCount(); i++) {
                    if (i == 0) {
                        row[i] = "Edit";
                    } else {
                        row[i] = rs.getString(model.getColumnName(i));
                    }
                }
                model.addRow(row);
                rowCount++;
            }
            LOGGER.log(Level.INFO, "Loaded {0} rows for licenseKey='{1}' in table '{2}'", 
                new Object[]{rowCount, licenseKey, tableName});
        }
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "Error fetching entries for key '{0}' in table '{1}': {2}", 
            new Object[]{licenseKey, tableName, e.getMessage()});
        JOptionPane.showMessageDialog(this, "Error fetching data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    tableManager.sortTable(1);
}

    private String findLicenseKeyColumn() {
        String[] columns = tableManager.getColumns();
        String licenseKeyColumn = null;
        boolean warningIssued = false;
        for (String column : columns) {
            if (column.equalsIgnoreCase("License_Key") || column.equalsIgnoreCase("Licence_Key")) {
                if (licenseKeyColumn == null) {
                    licenseKeyColumn = column;
                } else {
                    if (!warningIssued) {
                        LOGGER.log(Level.WARNING, "Multiple License_Key-like columns found in table '{0}': {1}, {2}. Using '{1}'", 
                            new Object[]{tableManager.getTableName(), licenseKeyColumn, column});
                        JOptionPane.showMessageDialog(this, 
                            "Warning: Multiple License_Key-like columns found (" + licenseKeyColumn + ", " + column + "). Using '" + licenseKeyColumn + "'.", 
                            "Column Ambiguity", JOptionPane.WARNING_MESSAGE);
                        warningIssued = true;
                    }
                }
            }
        }
        if (licenseKeyColumn == null) {
            LOGGER.log(Level.WARNING, "No License_Key column found in table '{0}'", tableManager.getTableName());
        }
        return licenseKeyColumn;
    }

    private void applyFilter() {
        String text = searchField.getText().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        if (sorter != null) {
            RowFilter<DefaultTableModel, Integer> filter = null;
            if (!text.isEmpty()) {
                filter = new RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        for (int i = 1; i < entry.getModel().getColumnCount(); i++) {
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
    }
}