package view_software_list_tab.license_key_tracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(LicenseKeyTracker.class.getName());

    public LicenseKeyTracker(ViewSoftwareListTab parentTab, TableManager tableManager) {
        this.parentTab = parentTab;
        this.tableManager = tableManager;
        this.keyUsageCounts = new HashMap<>();
        this.keyListModel = new DefaultListModel<>();
        this.keyList = new JList<>(keyListModel);
        this.usageLimit = 10; // Default limit
        setLayout(new BorderLayout(10, 10));
        initializeUI();
        createLicenseKeyRulesTableIfNotExists();
        loadLicenseKeyRules();
        loadLicenseKeys();
    }

    private void initializeUI() {
        JLabel titleLabel = new JLabel("License Key Tracker: " + tableManager.getTableName());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton closeButton = new JButton("Close License Key Tracker");
        closeButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        closeButton.addActionListener(e -> parentTab.showMainView());

        JButton undocumentedButton = new JButton("Undocumented Installations");
        undocumentedButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        undocumentedButton.addActionListener(e -> {
            UndocumentedInstallationsDialog dialog = new UndocumentedInstallationsDialog(parentTab, tableManager.getTableName());
            dialog.setVisible(true);
        });

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(closeButton, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(undocumentedButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        keyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keyList.setFixedCellWidth(300);
        keyList.setFixedCellHeight(25);

        keyList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String entry = value.toString();

                if (entry.contains("NumOfUses") && !entry.startsWith("Error") && !entry.startsWith("No license keys")) {
                    String key = entry.substring(0, entry.indexOf(" NumOfUses")).trim();
                    Integer count = keyUsageCounts.getOrDefault(key, 0);
                    LOGGER.log(Level.FINE, "Rendering key '{0}' with count {1} and usageLimit {2}", new Object[]{key, count, usageLimit});
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

        keyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = keyList.getSelectedValue();
                openButton.setEnabled(selected != null && selected.contains("NumOfUses"));
            }
        });

        JScrollPane listScrollPane = new JScrollPane(keyList);
        listScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        listScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(listScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Font buttonFont = new Font("SansSerif", Font.PLAIN, 12);

        JButton allButton = new JButton("All");
        allButton.setFont(buttonFont);
        allButton.addActionListener(e -> setFilter("all"));

        JButton violationsButton = new JButton("Violations");
        violationsButton.setFont(buttonFont);
        violationsButton.addActionListener(e -> setFilter("violations"));

        JButton maxButton = new JButton("Max Utilized");
        maxButton.setFont(buttonFont);
        maxButton.addActionListener(e -> setFilter("max"));

        JButton underutilizedButton = new JButton("Underutilized");
        underutilizedButton.setFont(buttonFont);
        underutilizedButton.addActionListener(e -> setFilter("underutilized"));

        filterPanel.add(allButton);
        filterPanel.add(violationsButton);
        filterPanel.add(maxButton);
        filterPanel.add(underutilizedButton);

        bottomPanel.add(filterPanel, BorderLayout.CENTER);

        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton settingsButton = new JButton("Settings");
        settingsButton.setFont(buttonFont);
        settingsButton.addActionListener(e -> openSettingsDialog());

        openButton = new JButton("Open Selected");
        openButton.setFont(buttonFont);
        openButton.setEnabled(false);
        openButton.addActionListener(e -> showKeyDetails(keyList.getSelectedValue()));

        settingsPanel.add(settingsButton);
        settingsPanel.add(openButton);

        bottomPanel.add(settingsPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        loadLicenseKeys();
    }

    private void createLicenseKeyRulesTableIfNotExists() {
        try (Connection conn = DatabaseUtils.getConnection()) {
            if (conn == null) {
                LOGGER.log(Level.SEVERE, "Failed to establish database connection");
                JOptionPane.showMessageDialog(this, "Failed to connect to the database", "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DatabaseMetaData meta = conn.getMetaData();
            LOGGER.log(Level.INFO, "Checking for existence of LicenseKeyRules table");
            try (ResultSet rs = meta.getTables(null, null, "LicenseKeyRules", new String[]{"TABLE"})) {
                if (!rs.next()) {
                    LOGGER.log(Level.INFO, "LicenseKeyRules table does not exist, creating it");
                    String createSql = "CREATE TABLE [LicenseKeyRules] ([TableName] TEXT(255) PRIMARY KEY, [UsageLimit] LONG)";
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(createSql);
                        LOGGER.log(Level.INFO, "Successfully created LicenseKeyRules table");
                    } catch (SQLException e) {
                        LOGGER.log(Level.SEVERE, "Failed to create LicenseKeyRules table: {0}", e.getMessage());
                        JOptionPane.showMessageDialog(this, String.format("Error creating LicenseKeyRules table: %s", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    LOGGER.log(Level.INFO, "LicenseKeyRules table already exists");
                    try (ResultSet columns = meta.getColumns(null, null, "LicenseKeyRules", null)) {
                        boolean foundTableName = false, foundUsageLimit = false;
                        while (columns.next()) {
                            String colName = columns.getString("COLUMN_NAME");
                            LOGGER.log(Level.FINE, "Found column in LicenseKeyRules: {0}", colName);
                            if (colName.equalsIgnoreCase("TableName")) foundTableName = true;
                            if (colName.equalsIgnoreCase("UsageLimit")) foundUsageLimit = true;
                        }
                        if (!foundTableName || !foundUsageLimit) {
                            LOGGER.log(Level.INFO, "LicenseKeyRules table missing required columns, dropping and recreating");
                            try (Statement stmt = conn.createStatement()) {
                                stmt.executeUpdate("DROP TABLE [LicenseKeyRules]");
                                LOGGER.log(Level.INFO, "Successfully dropped LicenseKeyRules table");
                                String createSql = "CREATE TABLE [LicenseKeyRules] ([TableName] TEXT(255) PRIMARY KEY, [UsageLimit] LONG)";
                                stmt.executeUpdate(createSql);
                                LOGGER.log(Level.INFO, "Successfully recreated LicenseKeyRules table with TableName and UsageLimit");
                            } catch (SQLException e) {
                                LOGGER.log(Level.SEVERE, "Failed to drop or recreate LicenseKeyRules table: {0}", e.getMessage());
                                JOptionPane.showMessageDialog(this, String.format("Failed to recreate LicenseKeyRules table: %s", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking/creating LicenseKeyRules table: {0}", e.getMessage());
            JOptionPane.showMessageDialog(this, String.format("Error initializing LicenseKeyRules table: %s", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadLicenseKeyRules() {
        String tableName = tableManager.getTableName();
        try (Connection conn = DatabaseUtils.getConnection()) {
            if (conn == null) {
                LOGGER.log(Level.SEVERE, "Failed to establish database connection");
                JOptionPane.showMessageDialog(this, "Failed to connect to the database", "Database Error", JOptionPane.ERROR_MESSAGE);
                usageLimit = 10; // Fallback to default
                return;
            }
            LOGGER.log(Level.INFO, "Loading license key rules for table '{0}'", tableName);
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT [UsageLimit] FROM [LicenseKeyRules] WHERE [TableName] = ?")) {
                pstmt.setString(1, tableName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        usageLimit = rs.getInt("UsageLimit");
                        LOGGER.log(Level.INFO, "Loaded usage limit {0} for table '{1}'", new Object[]{usageLimit, tableName});
                    } else {
                        usageLimit = 10; // Default value
                        try (PreparedStatement insertPstmt = conn.prepareStatement("INSERT INTO [LicenseKeyRules] ([TableName], [UsageLimit]) VALUES (?, ?)")) {
                            insertPstmt.setString(1, tableName);
                            insertPstmt.setInt(2, usageLimit);
                            insertPstmt.executeUpdate();
                            LOGGER.log(Level.INFO, "Inserted default usage limit {0} for table '{1}'", new Object[]{usageLimit, tableName});
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading license key rules for table '{0}': {1}", new Object[]{tableName, e.getMessage()});
            JOptionPane.showMessageDialog(this, String.format("Error loading license key rules: %s", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
            usageLimit = 10; // Fallback to default
        }
    }

    private void loadLicenseKeys() {
        keyListModel.clear();
        keyUsageCounts.clear();
        String tableName = tableManager.getTableName();
        String licenseKeyColumn = null;
        for (String column : tableManager.getColumns()) {
            if (column.equalsIgnoreCase("License_Key")) {
                licenseKeyColumn = column;
                break;
            }
        }

        if (licenseKeyColumn == null) {
            LOGGER.log(Level.WARNING, "License_Key column not found in table '{0}'", tableName);
            int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("The [License_Key] column does not exist in table '%s'. Do you want to add a [License_Key] column?", tableName),
                "Add License Key Column",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseUtils.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE [" + tableName + "] ADD [License_Key] TEXT(255)");
                    LOGGER.log(Level.INFO, "Added License_Key column to table '{0}'", tableName);
                    tableManager.setTableName(tableName); // Force schema reload
                    tableManager.refreshDataAndTabs();
                    licenseKeyColumn = "License_Key";
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error adding License_Key column to table '{0}': {1}", new Object[]{tableName, ex.getMessage()});
                    JOptionPane.showMessageDialog(this, String.format("Error adding License_Key column: %s", ex.getMessage()), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                keyListModel.addElement(String.format("Error: License_Key column not found in table '%s'", tableName));
                LOGGER.log(Level.SEVERE, "License_Key column not found in table '{0}'", tableName);
                return;
            }
        }

        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT [" + licenseKeyColumn + "], COUNT(*) as usage_count FROM [" + tableName + "] WHERE [" + licenseKeyColumn + "] IS NOT NULL AND [" + licenseKeyColumn + "] != '' GROUP BY [" + licenseKeyColumn + "]")) {
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
            LOGGER.log(Level.INFO, "Loaded {0} license keys for table '{1}'", new Object[]{keyListModel.getSize(), tableName});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching license keys from table '{0}': {1}", new Object[]{tableName, e.getMessage()});
            keyListModel.addElement("Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, String.format("Error fetching license keys: %s", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
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