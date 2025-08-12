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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import utils.DatabaseUtils;
import view_software_list_tab.TableManager;

public class LicenseKeyTracker extends JDialog {
    private final JList<String> keyList;
    private final DefaultListModel<String> keyListModel;
    private final TableManager tableManager;
    private final Map<String, Integer> keyUsageCounts;
    private int usageLimit;
    private String currentFilter = "all";
    private JButton openButton;

    public LicenseKeyTracker(JFrame parent, TableManager tableManager) {
        super(parent, "License Key Tracker", true);
        this.tableManager = tableManager;
        this.keyUsageCounts = new HashMap<>();
        this.keyListModel = new DefaultListModel<>();
        this.keyList = new JList<>(keyListModel);
        this.usageLimit = 10; // Default limit
        initializeUI();
        loadLicenseKeyRules();
        loadLicenseKeys();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setSize(800, 600); // Increased window size
        setLocationRelativeTo(null);

        keyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keyList.setFixedCellWidth(180);
        keyList.setFixedCellHeight(25);

        keyList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String key = value.toString().replace(" (Violation)", "").replace(" (Fully Utilized)", "").replace(" (Underutilized)", "");
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
                    c.setForeground(Color.BLACK); // Keep font color black
                }
                return c;
            }
        });

        // Ensure vertical scrolling is enabled
        JScrollPane listScrollPane = new JScrollPane(keyList);
        listScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        listScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(listScrollPane, BorderLayout.CENTER);

        // Top panel for filter buttons
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Font buttonFont = new Font("SansSerif", Font.PLAIN, 12); // Consistent font for all buttons

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

        add(filterPanel, BorderLayout.NORTH);

        // Bottom panel for action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        openButton = new JButton("Open Entries");
        openButton.setFont(buttonFont);
        openButton.setEnabled(false);
        openButton.addActionListener(e -> {
            String selectedKey = keyList.getSelectedValue();
            if (selectedKey != null) {
                showKeyDetails(selectedKey);
            }
        });
        buttonPanel.add(openButton);

        JButton settingsButton = new JButton("License Key Rules");
        settingsButton.setFont(buttonFont);
        settingsButton.addActionListener(e -> openSettingsDialog());
        buttonPanel.add(settingsButton);

        JButton closeButton = new JButton("Close License Key Tracker");
        closeButton.setFont(buttonFont);
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        keyList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    openButton.setEnabled(keyList.getSelectedValue() != null && !keyList.getSelectedValue().startsWith("Error"));
                }
            }
        });
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

        String[] columns = tableManager.getColumns();
        String licenseKeyColumn = null;
        for (String column : columns) {
            if (column.equalsIgnoreCase("License_Key")) {
                licenseKeyColumn = column;
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
                    keyListModel.addElement(licenseKey + label);
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
        LicenseKeySettingsDialog settingsDialog = new LicenseKeySettingsDialog(this, tableManager, usageLimit);
        settingsDialog.showDialog();
        loadLicenseKeyRules();
        loadLicenseKeys();
    }

    private void showKeyDetails(String licenseKey) {
        String cleanKey = licenseKey.replace(" (Violation)", "").replace(" (Fully Utilized)", "").replace(" (Underutilized)", "");
        LicenseKeyDetailsPanel detailsPanel = new LicenseKeyDetailsPanel(this, cleanKey, tableManager);
        detailsPanel.showPanel();
    }
}