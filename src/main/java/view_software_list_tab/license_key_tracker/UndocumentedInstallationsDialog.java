package view_software_list_tab.license_key_tracker;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
import javax.swing.table.TableRowSorter;

import utils.DatabaseUtils;
import view_software_list_tab.PopupHandler;
import view_software_list_tab.TableManager;
import view_software_list_tab.ViewSoftwareListTab;

public class UndocumentedInstallationsDialog extends JDialog {
    private final JTable table;
    private final TableManager tableManager;
    private final ViewSoftwareListTab parentTab;
    private final String tableName;
    private final JTextField searchField;

    public UndocumentedInstallationsDialog(ViewSoftwareListTab parentTab, String tableName) {
        super();
        this.parentTab = parentTab;
        this.tableName = tableName;
        this.table = new JTable();
        this.tableManager = new TableManager(table, tableName);
        this.searchField = new JTextField(20);
        setTitle("Undocumented Installations for " + tableName);
        setLayout(new BorderLayout());
        setSize(800, 600);
        setLocationRelativeTo(null);
        initializeUI();
        loadData();
    }

    private void initializeUI() {
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

        table.setCellSelectionEnabled(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        PopupHandler.addTablePopup(table, parentTab);
    }

    private void loadData() {
        String licenseKeyColumn = findLicenseKeyColumn();
        if (licenseKeyColumn == null) {
            JOptionPane.showMessageDialog(this, "No License_Key column found in table '" + tableName + "'", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        tableManager.refreshDataAndTabs();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM [" + tableName + "] WHERE [" + licenseKeyColumn + "] IS NULL OR [" + licenseKeyColumn + "] = ''")) {
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
            }
        } catch (SQLException e) {
            System.err.println("UndocumentedInstallationsDialog: Error fetching undocumented installations for table '" + tableName + "': " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String findLicenseKeyColumn() {
        String[] columns = tableManager.getColumns();
        String licenseKeyColumn = null;
        boolean warningIssued = false;
        for (String column : columns) {
            if (column.equalsIgnoreCase("License_Key") || column.equalsIgnoreCase("Licence_Key")) {
                if (licenseKeyColumn == null) {
                    licenseKeyColumn = column; // Prioritize exact match or first occurrence
                } else {
                    if (!warningIssued) {
                        System.err.println("UndocumentedInstallationsDialog: Multiple License_Key-like columns found in table '" + tableName + "': " + licenseKeyColumn + ", " + column);
                        JOptionPane.showMessageDialog(this, "Warning: Multiple License_Key-like columns found (" + licenseKeyColumn + ", " + column + "). Using '" + licenseKeyColumn + "'.", "Column Ambiguity", JOptionPane.WARNING_MESSAGE);
                        warningIssued = true;
                    }
                }
            }
        }
        if (licenseKeyColumn == null) {
            System.err.println("UndocumentedInstallationsDialog: No License_Key column found in table '" + tableName + "'");
        }
        return licenseKeyColumn;
    }

    private void applyFilter() {
        String text = searchField.getText().toLowerCase();
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        if (sorter != null) {
            RowFilter<DefaultTableModel, Integer> filter = null;
            if (!text.isEmpty()) {
                filter = new RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        for (int i = 1; i < entry.getModel().getColumnCount(); i++) { // Start from 1 to skip Edit column
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