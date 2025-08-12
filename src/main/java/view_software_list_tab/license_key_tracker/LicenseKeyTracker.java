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
        JButton settingsButton = new JButton("Key Rules");
        settingsButton.addActionListener(e -> openSettingsDialog());
        buttonPanel.add(settingsButton, BorderLayout.WEST);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton, BorderLayout.EAST);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadLicenseKeys() {
        keyListModel.clear();
        keyUsageCounts.clear();
        String tableName = tableManager.getTableName();
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT License_Key, COUNT(*) as usage_count FROM " + tableName + " WHERE License_Key IS NOT NULL AND License_Key != '' GROUP BY License_Key")) {
            while (rs.next()) {
                String licenseKey = rs.getString("License_Key");
                int count = rs.getInt("usage_count");
                keyUsageCounts.put(licenseKey, count);
                keyListModel.addElement(licenseKey + (isViolation(licenseKey, count) ? " (Violation)" : ""));
            }
            if (keyListModel.isEmpty()) {
                keyListModel.addElement("No license keys found");
            } else {
                keyList.setSelectedIndex(0);
            }
        } catch (SQLException e) {
            keyListModel.addElement("Error: " + e.getMessage());
            System.err.println("LicenseKeyTracker: Error fetching license keys: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching license keys: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isViolation(String licenseKey, int usageCount) {
        int limit = keyUsageLimits.getOrDefault(licenseKey, 10);
        return usageCount > limit;
    }

    private void openSettingsDialog() {
        LicenseKeySettingsDialog settingsDialog = new LicenseKeySettingsDialog(this, keyUsageLimits);
        settingsDialog.showDialog();
        loadLicenseKeys();
    }

    private void showKeyDetails(String licenseKey) {
        String cleanKey = licenseKey.replace(" (Violation)", "");
        LicenseKeyDetailsPanel detailsPanel = new LicenseKeyDetailsPanel(this, cleanKey, tableManager);
        detailsPanel.showPanel();
    }
}