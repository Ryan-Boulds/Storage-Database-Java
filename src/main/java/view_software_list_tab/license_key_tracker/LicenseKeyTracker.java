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
        this.usageLimit = 10;
        setLayout(new BorderLayout(10, 10));
        initializeUI();
        loadLicenseKeyRules();
        loadLicenseKeys();
    }

    private void initializeUI() {
        JLabel titleLabel = new JLabel(String.format("License Key Tracker: %s", tableManager.getTableName()));
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

        keyList.addListSelectionListener(this::updateOpenButton);
    }

    private void updateOpenButton(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            String selected = keyList.getSelectedValue();
            openButton.setEnabled(selected != null && selected.contains("NumOfUses"));
        }
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        loadLicenseKeys();
    }

    private void loadLicenseKeyRules() {
        String tableName = tableManager.getTableName();
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT UsageLimit FROM LicenseKeyRules WHERE TableName = '" + tableName.replace("'", "''") + "'")) {
            if (rs.next()) {
                usageLimit = rs.getInt("UsageLimit");
            } else {
                usageLimit = 10;
            }
        } catch (SQLException e) {
            System.err.println(String.format("LicenseKeyTracker: Error loading LicenseKeyRules for table '%s': %s", tableName, e.getMessage()));
            JOptionPane.showMessageDialog(this, String.format("Error loading license key rules: %s", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
            usageLimit = 10;
        }
    }

    public void updateTable(String tableName) {
        tableManager.setTableName(tableName);
        loadLicenseKeyRules();
        loadLicenseKeys();
        JLabel titleLabel = (JLabel) ((JPanel) getComponent(0)).getComponent(1);
        titleLabel.setText(String.format("License Key Tracker: %s", tableName));
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
            int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("The License_Key column does not exist in table '%s'. Do you want to add a License_Key column?", tableName),
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
                    System.err.println(String.format("LicenseKeyTracker: SQLException adding License_Key column to table '%s': %s", tableName, ex.getMessage()));
                    JOptionPane.showMessageDialog(this, String.format("Error adding License_Key column: %s", ex.getMessage()), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                keyListModel.addElement(String.format("Error: License_Key column not found in table '%s'", tableName));
                System.err.println(String.format("LicenseKeyTracker: License_Key column not found in table '%s'", tableName));
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
            keyListModel.addElement(String.format("Error: %s", e.getMessage()));
            System.err.println(String.format("LicenseKeyTracker: Error fetching license keys from table '%s': %s", tableName, e.getMessage()));
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