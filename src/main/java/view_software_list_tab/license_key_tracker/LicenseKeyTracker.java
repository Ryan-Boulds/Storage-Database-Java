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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import utils.DatabaseUtils;
import view_software_list_tab.TableManager;
import view_software_list_tab.ViewSoftwareListTab;

@SuppressWarnings("OverridableMethodCallInConstructor")
public class LicenseKeyTracker extends JPanel {
    private final JList<String> keyList;
    private final DefaultListModel<String> keyListModel;
    private final TableManager tableManager;
    private final ViewSoftwareListTab parentTab;
    private final Map<String, Integer> keyUsageCounts;
    private int usageLimit;
    private final String currentFilter = "all";
    private JButton toggleUndocumentedButton;
    private JPanel detailsContainer;
    private UndocumentedInstallationsPanel undocumentedPanel;
    private KeyDetailsTable keyDetailsTable;
    private String lastSelectedKey;
    private boolean showingUndocumented = false;
    private static final Logger LOGGER = Logger.getLogger(LicenseKeyTracker.class.getName());

    public LicenseKeyTracker(ViewSoftwareListTab parentTab, TableManager tableManager) {
        this.parentTab = parentTab;
        this.tableManager = tableManager;
        this.keyUsageCounts = new HashMap<>();
        this.keyListModel = new DefaultListModel<>();
        this.keyList = new JList<>(keyListModel);
        this.usageLimit = 10;
        this.lastSelectedKey = null;
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

        JButton settingsButton = new JButton("Settings");
        settingsButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        settingsButton.addActionListener(e -> openSettingsDialog());

        toggleUndocumentedButton = new JButton("View Undocumented Installations");
        toggleUndocumentedButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        toggleUndocumentedButton.addActionListener(e -> toggleUndocumentedView());

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(closeButton, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(settingsButton);
        rightPanel.add(toggleUndocumentedButton);
        topPanel.add(rightPanel, BorderLayout.EAST);
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
                if (selected != null && selected.contains("NumOfUses")) {
                    String cleanKey = selected.substring(0, selected.indexOf(" NumOfUses")).trim();
                    lastSelectedKey = cleanKey;
                    showKeyDetails(cleanKey);
                    showingUndocumented = false;
                    toggleUndocumentedButton.setText("View Undocumented Installations");
                } else {
                    lastSelectedKey = null;
                    showEmptyDetails();
                    showingUndocumented = false;
                    toggleUndocumentedButton.setText("View Undocumented Installations");
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(keyList);
        listScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        listScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        detailsContainer = new JPanel(new BorderLayout());
        keyDetailsTable = new KeyDetailsTable(null, tableManager, this, parentTab);
        detailsContainer.add(keyDetailsTable, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, listScrollPane, detailsContainer);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        updateUndocumentedButtonState();
    }

    private void toggleUndocumentedView() {
        if (!showingUndocumented) {
            keyList.clearSelection();
            showUndocumentedInstallations();
            showingUndocumented = true;
            toggleUndocumentedButton.setText("Hide Undocumented Installations");
            LOGGER.log(Level.INFO, "Switched to undocumented installations for table '{0}'", tableManager.getTableName());
        } else {
            toggleUndocumentedButton.setText("View Undocumented Installations");
            showingUndocumented = false;
            showKeyDetails(lastSelectedKey);
            if (lastSelectedKey != null) {
                String formattedEntry = String.format("%-30s NumOfUses: %d%s", lastSelectedKey, 
                    keyUsageCounts.getOrDefault(lastSelectedKey, 0), getKeyLabel(lastSelectedKey));
                keyList.setSelectedValue(formattedEntry, true);
            } else {
                keyList.clearSelection();
            }
            LOGGER.log(Level.INFO, "Restored last key '{0}' for table '{1}'", 
                new Object[]{lastSelectedKey != null ? lastSelectedKey : "none", tableManager.getTableName()});
        }
    }

    private String getKeyLabel(String licenseKey) {
        int count = keyUsageCounts.getOrDefault(licenseKey, 0);
        if (count > usageLimit) {
            return " (Violation)";
        } else if (count == usageLimit) {
            return " (Fully Utilized)";
        } else {
            return " (Underutilized)";
        }
    }

    private void showKeyDetails(String licenseKey) {
        LOGGER.log(Level.INFO, "Calling showKeyDetails with licenseKey='{0}'", licenseKey);
        detailsContainer.removeAll();
        keyDetailsTable = new KeyDetailsTable(licenseKey, tableManager, this, parentTab);
        detailsContainer.add(keyDetailsTable, BorderLayout.CENTER);
        detailsContainer.revalidate();
        detailsContainer.repaint();
        LOGGER.log(Level.INFO, "Displayed details for license key '{0}' in table '{1}'", 
            new Object[]{licenseKey, tableManager.getTableName()});
    }

    private void showUndocumentedInstallations() {
        detailsContainer.removeAll();
        undocumentedPanel = new UndocumentedInstallationsPanel(parentTab, tableManager.getTableName(), this);
        detailsContainer.add(undocumentedPanel, BorderLayout.CENTER);
        detailsContainer.revalidate();
        detailsContainer.repaint();
        undocumentedPanel.loadData();
        LOGGER.log(Level.INFO, "Displayed undocumented installations for table '{0}'", tableManager.getTableName());
    }

    private void showEmptyDetails() {
        detailsContainer.removeAll();
        keyDetailsTable = new KeyDetailsTable(null, tableManager, this, parentTab);
        detailsContainer.add(keyDetailsTable, BorderLayout.CENTER);
        detailsContainer.revalidate();
        detailsContainer.repaint();
    }

    private void updateUndocumentedButtonState() {
        String licenseKeyColumn = null;
        for (String column : tableManager.getColumns()) {
            if (column.equalsIgnoreCase("License_Key")) {
                licenseKeyColumn = column;
                break;
            }
        }
        toggleUndocumentedButton.setEnabled(licenseKeyColumn != null);
    }

    private void createLicenseKeyRulesTableIfNotExists() {
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "LicenseKeyRules", new String[]{"TABLE"})) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("CREATE TABLE LicenseKeyRules (TableName VARCHAR(255) PRIMARY KEY, UsageLimit INTEGER)");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating LicenseKeyRules table: {0}", e.getMessage());
        }
    }

    private void loadLicenseKeyRules() {
        String tableName = tableManager.getTableName();
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT UsageLimit FROM LicenseKeyRules WHERE TableName = ?")) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    usageLimit = rs.getInt("UsageLimit");
                } else {
                    usageLimit = 10;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading license key rules for table '{0}': {1}", new Object[]{tableName, e.getMessage()});
            JOptionPane.showMessageDialog(this, String.format("Error loading license key rules: %s", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
            usageLimit = 10;
        }
    }

    public void loadLicenseKeys() {
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
                    tableManager.initializeColumns();
                    licenseKeyColumn = "License_Key";
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error adding License_Key column to table '{0}': {1}", new Object[]{tableName, ex.getMessage()});
                    JOptionPane.showMessageDialog(this, String.format("Error adding License_Key column: %s", ex.getMessage()), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                keyListModel.addElement(String.format("Error: License_Key column not found in table '%s'", tableName));
                LOGGER.log(Level.SEVERE, "License_Key column not found in table '{0}'", tableName);
                showEmptyDetails();
                return;
            }
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>();

        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT [" + licenseKeyColumn + "], COUNT(*) as usage_count FROM [" + tableName + "] WHERE [" + licenseKeyColumn + "] IS NOT NULL AND [" + licenseKeyColumn + "] != '' GROUP BY [" + licenseKeyColumn + "]")) {
            while (rs.next()) {
                String licenseKey = rs.getString(licenseKeyColumn);
                int count = rs.getInt("usage_count");
                keyUsageCounts.put(licenseKey, count);
                entries.add(new AbstractMap.SimpleEntry<>(licenseKey, count));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching license keys from table '{0}': {1}", new Object[]{tableName, e.getMessage()});
            keyListModel.addElement("Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, String.format("Error fetching license keys: %s", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
            showEmptyDetails();
            return;
        }

        entries.sort(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed()
            .thenComparing(Map.Entry::getKey));

        for (Map.Entry<String, Integer> entry : entries) {
            String licenseKey = entry.getKey();
            int count = entry.getValue();
            String label = getKeyLabel(licenseKey);
            if (currentFilter.equals("all") ||
                (currentFilter.equals("violations") && count > usageLimit) ||
                (currentFilter.equals("max") && count == usageLimit) ||
                (currentFilter.equals("underutilized") && count < usageLimit)) {
                String formattedEntry = String.format("%-30s NumOfUses: %d%s", licenseKey, count, label);
                keyListModel.addElement(formattedEntry);
            }
        }

        if (keyListModel.isEmpty()) {
            keyListModel.addElement("No license keys found for current filter");
        }

        // Restore selection after refresh
        if (lastSelectedKey != null) {
            String formattedEntry = String.format("%-30s NumOfUses: %d%s", lastSelectedKey, 
                keyUsageCounts.getOrDefault(lastSelectedKey, 0), getKeyLabel(lastSelectedKey));
            keyList.setSelectedValue(formattedEntry, true);
        } else {
            keyList.clearSelection();
        }

        LOGGER.log(Level.INFO, "Loaded {0} license keys for table '{1}'", new Object[]{keyListModel.getSize(), tableManager.getTableName()});
        updateUndocumentedButtonState();
        showEmptyDetails();
    }

    private void openSettingsDialog() {
        LicenseKeySettingsDialog settingsDialog = new LicenseKeySettingsDialog(null, tableManager, usageLimit);
        settingsDialog.showDialog();
        loadLicenseKeyRules();
        loadLicenseKeys();
    }
}