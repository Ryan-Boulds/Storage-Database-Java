package view_software_list_tab.view_software_details;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;

import utils.DatabaseUtils;
import utils.UIComponentUtils;
import view_software_list_tab.ViewSoftwareListTab;

public class DeviceDetailsPanel extends JPanel {
    private final String assetName;
    private final ViewSoftwareListTab parentTab;
    private static final int CONTENT_WIDTH = 800;
    private static final int LIST_HEIGHT = 400;
    private static final int LIST_WIDTH = 200;
    private static final Logger LOGGER = Logger.getLogger(DeviceDetailsPanel.class.getName());
    private JList<String> tableList;
    private JPanel dataListPanel;

    public DeviceDetailsPanel(String assetName, ViewSoftwareListTab parentTab) {
        this.assetName = assetName;
        this.parentTab = parentTab;
        setLayout(new BorderLayout(10, 10));
        initializeComponents();
    }

    private void initializeComponents() {
        JLabel titleLabel = UIComponentUtils.createAlignedLabel("Software Details: " + assetName);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton backButton = UIComponentUtils.createFormattedButton("Back");
        backButton.addActionListener(e -> parentTab.showMainView());

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(backButton, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setPreferredSize(new Dimension(CONTENT_WIDTH, LIST_HEIGHT + 50));
        contentPanel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

        tableList = new JList<>();
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = tableList.getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    updateDataList(selectedTable);
                }
            }
        });
        JScrollPane tableListScrollPane = new JScrollPane(tableList);
        tableListScrollPane.setPreferredSize(new Dimension(LIST_WIDTH, LIST_HEIGHT));

        dataListPanel = new JPanel();
        dataListPanel.setLayout(new BoxLayout(dataListPanel, BoxLayout.Y_AXIS));
        JScrollPane dataListScrollPane = new JScrollPane(dataListPanel);
        dataListScrollPane.setPreferredSize(new Dimension(CONTENT_WIDTH - LIST_WIDTH - 10, LIST_HEIGHT));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableListScrollPane, dataListScrollPane);
        splitPane.setDividerLocation(LIST_WIDTH);
        splitPane.setResizeWeight(0.3);
        contentPanel.add(splitPane);

        JScrollPane contentScrollPane = new JScrollPane(contentPanel);
        add(contentScrollPane, BorderLayout.CENTER);

        loadTableList();
    }

    private void loadTableList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        try {
            List<String> tableNames = DatabaseUtils.getTableNames();
            List<String> includedTables = getIncludedInventoryTables();
            List<String> validTables = new ArrayList<>();
            for (String table : tableNames) {
                if (includedTables.contains(table) && !isExcludedTable(table)) {
                    validTables.add(table);
                }
            }
            validTables.sort(String::compareToIgnoreCase);
            for (String table : validTables) {
                listModel.addElement(table);
            }
            if (listModel.isEmpty()) {
                listModel.addElement("No tables available");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching table names: {0}", e.getMessage());
            listModel.addElement("Error: " + e.getMessage());
        }
        tableList.setModel(listModel);
    }

    private void updateDataList(String tableName) {
        dataListPanel.removeAll();
        HashMap<String, String> entry;
        try {
            entry = DatabaseUtils.getDeviceByAssetName(tableName, assetName);
            LOGGER.log(Level.INFO, "Table {0} entry for AssetName {1}: {2}", new Object[]{tableName, assetName, entry});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying table {0}: {1}", new Object[]{tableName, e.getMessage()});
            JLabel errorLabel = new JLabel("Error loading data: " + e.getMessage());
            dataListPanel.add(errorLabel);
            dataListPanel.revalidate();
            dataListPanel.repaint();
            return;
        }

        if (entry == null || entry.isEmpty()) {
            JLabel messageLabel = new JLabel("No entry found in table " + tableName + " for AssetName: " + assetName);
            dataListPanel.add(messageLabel);
            dataListPanel.revalidate();
            dataListPanel.repaint();
            return;
        }

        Set<String> columns = new HashSet<>();
        for (String column : entry.keySet()) {
            if (!column.equals("AssetName") && !column.equals("TableName")) {
                String value = entry.get(column);
                if (value != null && !value.trim().isEmpty()) {
                    columns.add(column);
                }
            }
        }

        JLabel tableLabel = new JLabel("Table: " + tableName);
        tableLabel.setFont(new Font("Arial", Font.BOLD, 12));
        dataListPanel.add(tableLabel);
        for (String column : columns) {
            String value = entry.get(column);
            JLabel dataLabel = new JLabel(column + ": " + value);
            dataLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            dataListPanel.add(dataLabel);
        }

        dataListPanel.revalidate();
        dataListPanel.repaint();
    }

    private boolean isExcludedTable(String tableName) {
        return tableName.equalsIgnoreCase("TableInformation") ||
               tableName.equalsIgnoreCase("LicenseKeyRules") ||
               tableName.equalsIgnoreCase("Templates");
    }

    private List<String> getIncludedInventoryTables() {
        List<String> tables = new ArrayList<>();
        try (Connection conn = DatabaseUtils.getConnection()) {
            if (!tableExists("TableInformation", conn)) {
                createTableInformationTable(conn);
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DISTINCT InventoryTables FROM TableInformation WHERE InventoryTables IS NOT NULL")) {
                while (rs.next()) {
                    String tableName = rs.getString("InventoryTables");
                    if (tableName != null && !tableName.trim().isEmpty()) {
                        tables.add(tableName);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching included inventory tables: {0}", e.getMessage());
        }
        return tables;
    }

    private boolean tableExists(String tableName, Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void createTableInformationTable(Connection conn) throws SQLException {
        String createSql = "CREATE TABLE TableInformation (ID INTEGER PRIMARY KEY, InventoryTables VARCHAR(255))";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createSql);
        }
    }
}