package view_software_list_tab.license_key_tracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;

import utils.DatabaseUtils;
import view_software_list_tab.TableManager;
import view_software_list_tab.ViewSoftwareListTab;

public class LicenseKeyTracker extends JPanel {
    private final JList<String> keyList;
    private final DefaultListModel<String> keyListModel;
    private final TableManager tableManager;
    private final ViewSoftwareListTab parentTab;
    private final Map<String, Integer> keyUsageCounts;
    private int usageLimit;
    private String currentFilter = "all";
    private JButton openButton;

    public LicenseKeyTracker(ViewSoftwareListTab parentTab, TableManager tableManager) {
        this.parentTab = parentTab;
        this.tableManager = tableManager;
        this.keyUsageCounts = new HashMap<>();
        this.keyListModel = new DefaultListModel<>();
        this.keyList = new JList<>(keyListModel);
        this.usageLimit = 10; // Default limit
        setLayout(new BorderLayout(10, 10));
        initializeUI();
        loadLicenseKeyRules();
        loadLicenseKeys();
    }

    private void initializeUI() {
        JLabel titleLabel = new JLabel("License Key Tracker: " + tableManager.getTableName());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton closeButton = new JButton("Close License Key Tracker");
        closeButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        closeButton.addActionListener(e -> parentTab.showMainView());

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(closeButton, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        keyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keyList.setFixedCellWidth(300);
        keyList.setFixedCellHeight(25);

        keyList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String entry = value.toString();
                
                if (entry.contains("NumOfUses")) {
                    String key = entry.substring(0, entry.indexOf(" NumOfUses")).trim();
                    Integer count = keyUsageCounts.getOrDefault(key, 0);
                    
                    if (isSelected) {
                        c.setBackground(list.getSelectionBackground());
                        c.setForeground(list.getSelectionForeground());
                    } else {
                        if (count > usageLimit) {
                            c.setBackground(Color.RED);
                        } else if (count == usageLimit) {
                            c.setBackground(Color.GREEN);
                        } else {
                            c.setBackground(Color.YELLOW);
                        }
                        c.setForeground(Color.BLACK);
                    }
                } else {
                    if (isSelected) {
                        c.setBackground(list.getSelectionBackground());
                        c.setForeground(list.getSelectionForeground());
                    } else {
                        c.setBackground(list.getBackground());
                        c.setForeground(list.getForeground());
                    }
                }
                return c;
            }
        });

        JScrollPane listScrollPane = new JScrollPane(keyList);
        listScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        listScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(listScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Font buttonFont = new Font("SansSerif", Font.PLAIN, 12);

        JButton violationsButton = new JButton("Show Violations");
        violationsButton.setFont(buttonFont);
        violationsButton.addActionListener(e -> {
            currentFilter = "violations";
            loadLicenseKeys();
        });
        filterPanel.add(violationsButton);

        JButton maxUsageButton = new JButton("Show Maximum Usages");
        maxUsageButton.setFont(buttonFont);
        maxUsageButton.addActionListener(e -> {
            currentFilter = "max";
            loadLicenseKeys();
        });
        filterPanel.add(maxUsageButton);

        JButton underutilizedButton = new JButton("Show Underutilized Keys");
        underutilizedButton.setFont(buttonFont);
        underutilizedButton.addActionListener(e -> {
            currentFilter = "underutilized";
            loadLicenseKeys();
        });
        filterPanel.add(underutilizedButton);

        JButton allKeysButton = new JButton("Show All Keys");
        allKeysButton.setFont(buttonFont);
        allKeysButton.addActionListener(e -> {
            currentFilter = "all";
            loadLicenseKeys();
        });
        filterPanel.add(allKeysButton);

        bottomPanel.add(filterPanel, BorderLayout.NORTH);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        openButton = new JButton("Open Entries");
        openButton.setFont(buttonFont);
        openButton.setEnabled(false);
        openButton.addActionListener(e -> {
            String selectedKey = keyList.getSelectedValue();
            if (selectedKey != null) {
                showKeyDetails(selectedKey);
            }
        });
        actionPanel.add(openButton);

        JButton settingsButton = new JButton("License Key Rules");
        settingsButton.setFont(buttonFont);
        settingsButton.addActionListener(e -> openSettingsDialog());
        actionPanel.add(settingsButton);

        bottomPanel.add(actionPanel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        keyList.addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                String selectedValue = keyList.getSelectedValue();
                openButton.setEnabled(selectedValue != null && selectedValue.contains("NumOfUses"));
            }
        });
    }

    public void updateTable(String tableName) {
        tableManager.setTableName(tableName);
        loadLicenseKeyRules();
        loadLicenseKeys();
    }

    private void loadLicenseKeyRules() {
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement()) {
            String createTableSQL = "CREATE TABLE LicenseKeyRules (TableName VARCHAR(255), UsageLimit INTEGER, PRIMARY KEY (TableName))";
            try {
                stmt.executeUpdate(createTableSQL);
                System.out.println("LicenseKeyTracker: Created LicenseKeyRules table");
            } catch (SQLException e) {
                if (!e.getMessage().contains("already exists")) {
                    System.err.println("LicenseKeyTracker: Error creating LicenseKeyRules table: " + e.getMessage());
                }
            }

            String tableName = tableManager.getTableName();
            String sql = "SELECT UsageLimit FROM LicenseKeyRules WHERE TableName = '" + tableName.replace("'", "''") + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                usageLimit = rs.getInt("UsageLimit");
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

        ((JLabel) ((JPanel) getComponent(0)).getComponent(1)).setText("License Key Tracker: " + tableName);

        String[] columns = tableManager.getColumns();
        String licenseKeyColumn = null;
        for (String column : columns) {
            if (column.equalsIgnoreCase("License_Key")) {
                licenseKeyColumn = column;
                break;
            }
        }

        if (licenseKeyColumn == null) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "The License_Key column does not exist in table '" + tableName + "'. Do you want to add a License_Key column?",
                "Add License Key Column",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseUtils.getConnection()) {
                    String sql = "ALTER TABLE " + tableName + " ADD License_Key VARCHAR(255)";
                    conn.createStatement().executeUpdate(sql);
                    JOptionPane.showMessageDialog(this, "License_Key column added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                    SwingUtilities.invokeLater(() -> {
                        tableManager.refreshDataAndTabs();
                        loadLicenseKeys();
                    });
                } catch (SQLException ex) {
                    System.err.println("LicenseKeyTracker: SQLException adding License_Key column to table '" + tableName + "': " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Error adding License_Key column: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                keyListModel.addElement("Error: License_Key column not found in table '" + tableName + "'");
                System.err.println("LicenseKeyTracker: License_Key column not found in table '" + tableName + "'");
            }
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT " + licenseKeyColumn + ", COUNT(*) as usage_count FROM " + tableName + " WHERE " + licenseKeyColumn + " IS NOT NULL AND " + licenseKeyColumn + " != '' GROUP BY " + licenseKeyColumn)) {
            while (rs.next()) {
                String licenseKey = rs.getString(licenseKeyColumn);
                int count = rs.getInt("usage_count");
                keyUsageCounts.put(licenseKey, count);

                boolean addKey = false;
                String label = "";
                if (count > usageLimit) {
                    label = " (Violation)";
                    if (currentFilter.equals("violations") || currentFilter.equals("all")) {
                        addKey = true;
                    }
                } else if (count == usageLimit) {
                    label = " (Fully Utilized)";
                    if (currentFilter.equals("max") || currentFilter.equals("all")) {
                        addKey = true;
                    }
                } else {
                    label = " (Underutilized)";
                    if (currentFilter.equals("underutilized") || currentFilter.equals("all")) {
                        addKey = true;
                    }
                }

                if (addKey) {
                    String formattedEntry = String.format("%-30s NumOfUses: %d%s", licenseKey, count, label);
                    keyListModel.addElement(formattedEntry);
                }
            }
            if (keyListModel.isEmpty()) {
                keyListModel.addElement("No license keys found for current filter");
            }
            keyList.clearSelection();
        } catch (SQLException e) {
            keyListModel.addElement("Error: " + e.getMessage());
            System.err.println("LicenseKeyTracker: Error fetching license keys from table '" + tableName + "': " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching license keys: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSettingsDialog() {
        LicenseKeySettingsDialog settingsDialog = new LicenseKeySettingsDialog(null, tableManager, usageLimit);
        settingsDialog.showDialog();
        loadLicenseKeyRules();
        loadLicenseKeys();
    }

    private void showKeyDetails(String selectedKey) {
        if (selectedKey != null && selectedKey.contains("NumOfUses")) {
            String cleanKey = selectedKey.substring(0, selectedKey.indexOf(" NumOfUses")).trim();
            LicenseKeyDetailsPanel detailsPanel = new LicenseKeyDetailsPanel(null, cleanKey, tableManager);
            detailsPanel.showPanel();
        } else {
            JOptionPane.showMessageDialog(this, "Please select a valid license key.", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
        }
    }
}