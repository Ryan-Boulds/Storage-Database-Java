package view_inventory_tab.license_key_tracker;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import utils.DatabaseUtils;
import view_inventory_tab.TableManager;

public class LicenseKeyDetailsPanel extends JDialog {
    private final String licenseKey;
    private final TableManager tableManager;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private static final Logger LOGGER = Logger.getLogger(LicenseKeyDetailsPanel.class.getName());

    public LicenseKeyDetailsPanel(LicenseKeyTracker parent, String licenseKey, TableManager tableManager) {
        super();
        this.licenseKey = licenseKey;
        this.tableManager = tableManager;
        this.tableModel = new DefaultTableModel();
        this.table = new JTable(tableModel);
        initializeUI();
        loadKeyEntries();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setSize(800, 400);
        setLocationRelativeTo(null);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

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
            LOGGER.log(Level.WARNING, "License_Key column not found in table '{0}', keeping table empty", tableName);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM [" + tableName + "] WHERE [" + licenseKeyColumn + "] = '" + licenseKey.replace("'", "''") + "'")) {
            while (rs.next()) {
                Object[] row = new Object[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    row[i] = rs.getString(columns[i]);
                }
                tableModel.addRow(row);
            }
            LOGGER.log(Level.INFO, "Loaded {0} rows for license key '{1}' in table '{2}'", 
                new Object[]{tableModel.getRowCount(), licenseKey, tableName});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching entries for license key '{0}' in table '{1}': {2}", 
                new Object[]{licenseKey, tableName, e.getMessage()});
            JOptionPane.showMessageDialog(this, "Error fetching entries: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        FontMetrics fontMetrics = table.getFontMetrics(table.getFont());
        int padding = 20;
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            String header = columns[i];
            int maxWidth = fontMetrics.stringWidth(header) + padding;
            for (int row = 0; row < table.getRowCount(); row++) {
                Object value = table.getValueAt(row, i);
                String text = value != null ? value.toString() : "";
                int textWidth = fontMetrics.stringWidth(text) + padding;
                maxWidth = Math.max(maxWidth, textWidth);
            }
            column.setPreferredWidth(maxWidth);
        }
    }

    public void showPanel() {
        setVisible(true);
    }
}