package view_software_list_tab.license_key_tracker;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import utils.DatabaseUtils;
import view_software_list_tab.TableManager;

public class LicenseKeyTracker extends JDialog {
    private final JList<String> keyList;
    private final DefaultListModel<String> keyListModel;
    private final TableManager tableManager;
    private final Map<String, Integer> keyUsageCounts;
    private final Map<String, Integer> keyUsageLimits;

    public LicenseKeyTracker(JFrame parent, TableManager tableManager) {
        super(parent, "License Key Tracker", true);
        this.tableManager = tableManager;
        this.keyUsageCounts = new HashMap<>();
        this.keyUsageLimits = new HashMap<>();
        this.keyListModel = new DefaultListModel<>();
        this.keyList = new JList<>(keyListModel);
        initializeUI();
        loadLicenseKeyRules(); // Load usage limits from LicenseKeyRules table
        loadLicenseKeys();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setSize(600, 400);
        setLocationRelativeTo(null);

        keyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keyList.setFixedCellWidth(180);
        keyList.setFixedCellHeight(25);
        keyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedKey = keyList.getSelectedValue();
                if (selectedKey != null && !selectedKey.startsWith("Error")) {
                    showKeyDetails(selectedKey);
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(keyList);
        add(listScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JButton settingsButton = new JButton("License Key Rules");
        settingsButton.addActionListener(e -> openSettingsDialog());
        buttonPanel.add(settingsButton, BorderLayout.WEST);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton, BorderLayout.EAST);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadLicenseKeyRules() {
        keyUsageLimits.clear();
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement()) {
            // Create LicenseKeyRules table if it doesn't exist
            String createTableSQL = "CREATE TABLE LicenseKeyRules (TableName VARCHAR(255), LicenseKey VARCHAR(255), UsageLimit INTEGER, PRIMARY KEY (TableName, LicenseKey))";
            try {
                stmt.executeUpdate(createTableSQL);
                System.out.println("LicenseKeyTracker: Created LicenseKeyRules table");
            } catch (SQLException e) {
                if (!e.getMessage().contains("already exists")) {
                    System.err.println("LicenseKeyTracker: Error creating LicenseKeyRules table: " + e.getMessage());
                }
            }

            // Load usage limits for the current table
            String tableName = tableManager.getTableName();
            String sql = "SELECT LicenseKey, UsageLimit FROM LicenseKeyRules WHERE TableName = '" + tableName.replace("'", "''") + "'";
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String licenseKey = rs.getString("LicenseKey");
                int limit = rs.getInt("UsageLimit");
                keyUsageLimits.put(licenseKey, limit);
            }
        } catch (SQLException e) {
            System.err.println("LicenseKeyTracker: Error loading license key rules: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error loading license key rules: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadLicenseKeys() {
        keyListModel.clear();
        keyUsageCounts.clear();
        String tableName = tableManager.getTableName();

        // Find the actual License_Key column name (case-sensitive)
        String[] columns = tableManager.getColumns();
        String licenseKeyColumn = null;
        for (String column : columns) {
            if (column.equalsIgnoreCase("License_Key")) {
                licenseKeyColumn = column; // Use the exact column name
                break;
            }
        }

        if (licenseKeyColumn == null) {
            keyListModel.addElement("Error: License_Key column not found in table '" + tableName + "'");
            System.err.println("LicenseKeyTracker: License_Key column not found in table '" + tableName + "'");
            JOptionPane.showMessageDialog(this, "Error: License_Key column not found in table '" + tableName + "'", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT " + licenseKeyColumn + ", COUNT(*) as usage_count FROM " + tableName + " WHERE " + licenseKeyColumn + " IS NOT NULL AND " + licenseKeyColumn + " != '' GROUP BY " + licenseKeyColumn)) {
            while (rs.next()) {
                String licenseKey = rs.getString(licenseKeyColumn);
                int count = rs.getInt("usage_count");
                keyUsageCounts.put(licenseKey, count);
                keyListModel.addElement(licenseKey + (isViolation(licenseKey, count) ? " (Violation)" : ""));
            }
            if (keyListModel.isEmpty()) {
                keyListModel.addElement("No license keys found");
            }
            // Ensure the list is shown initially without selecting an item
            keyList.clearSelection();
        } catch (SQLException e) {
            keyListModel.addElement("Error: " + e.getMessage());
            System.err.println("LicenseKeyTracker: Error fetching license keys from table '" + tableName + "': " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching license keys: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isViolation(String licenseKey, int usageCount) {
        int limit = keyUsageLimits.getOrDefault(licenseKey, 10);
        return usageCount > limit;
    }

    private void openSettingsDialog() {
        LicenseKeySettingsDialog settingsDialog = new LicenseKeySettingsDialog(this, tableManager, keyUsageLimits);
        settingsDialog.showDialog();
        loadLicenseKeyRules(); // Reload rules after settings dialog closes
        loadLicenseKeys(); // Refresh the list to reflect updated limits
    }

    private void showKeyDetails(String licenseKey) {
        String cleanKey = licenseKey.replace(" (Violation)", "");
        LicenseKeyDetailsPanel detailsPanel = new LicenseKeyDetailsPanel(this, cleanKey, tableManager);
        detailsPanel.showPanel();
    }
}