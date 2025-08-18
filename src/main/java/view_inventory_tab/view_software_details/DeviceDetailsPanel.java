package view_inventory_tab.view_software_details;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import utils.DatabaseUtils;
import utils.TablesNotIncludedList;
import utils.UIComponentUtils;
import view_inventory_tab.ViewInventoryTab;

public class DeviceDetailsPanel extends JPanel {
    private final String assetName;
    private final ViewInventoryTab parentTab;
    private static final int CONTENT_WIDTH = 800;
    private static final int SPECS_HEIGHT = 200;
    private static final int LIST_HEIGHT = 400;
    private static final int LIST_WIDTH = 200;
    private static final Logger LOGGER = Logger.getLogger(DeviceDetailsPanel.class.getName());
    private JList<String> tableList;
    private JPanel dataListPanel;

    public DeviceDetailsPanel(String assetName, view_inventory_tab.ViewInventoryTab parentTab) {
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
        contentPanel.setPreferredSize(new Dimension(CONTENT_WIDTH, SPECS_HEIGHT + LIST_HEIGHT + 50));
        contentPanel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

        JTextArea inventorySpecsArea = new JTextArea();
        inventorySpecsArea.setEditable(false);
        inventorySpecsArea.setLineWrap(true);
        inventorySpecsArea.setWrapStyleWord(true);
        Border border = new LineBorder(Color.GRAY, 1);
        inventorySpecsArea.setBorder(border);
        inventorySpecsArea.setPreferredSize(new Dimension(CONTENT_WIDTH, SPECS_HEIGHT));
        contentPanel.add(inventorySpecsArea);
        contentPanel.add(Box.createVerticalStrut(10));

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
        dataListScrollPane.setPreferredSize(new Dimension(CONTENT_WIDTH - LIST_WIDTH, LIST_HEIGHT));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableListScrollPane, dataListScrollPane);
        splitPane.setDividerLocation(LIST_WIDTH);
        contentPanel.add(splitPane);

        add(contentPanel, BorderLayout.CENTER);

        refreshTableList();
        updateInventorySpecs();
        if (!tableList.isSelectionEmpty()) {
            tableList.setSelectedIndex(0);
        }
    }

    private void updateInventorySpecs() {
        try {
            HashMap<String, String> inventoryEntry = DatabaseUtils.getDeviceByAssetName("Inventory", assetName);
            if (inventoryEntry != null && !inventoryEntry.isEmpty()) {
                StringBuilder specs = new StringBuilder();
                for (String column : inventoryEntry.keySet()) {
                    String value = inventoryEntry.get(column);
                    if (value != null && !value.trim().isEmpty()) {
                        specs.append(column).append(": ").append(value).append("\n");
                    }
                }
                JTextArea inventorySpecsArea = (JTextArea) ((JPanel) getComponent(1)).getComponent(0);
                inventorySpecsArea.setText(specs.toString());
                LOGGER.log(Level.INFO, "Inventory specs loaded for AssetName {0}: {1}", new Object[]{assetName, specs.toString()});
            } else {
                JTextArea inventorySpecsArea = (JTextArea) ((JPanel) getComponent(1)).getComponent(0);
                inventorySpecsArea.setText("No Inventory data found for AssetName: " + assetName);
                LOGGER.log(Level.WARNING, "No Inventory data found for AssetName {0}", assetName);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying Inventory table for AssetName {0}: {1}", new Object[]{assetName, e.getMessage()});
            JTextArea inventorySpecsArea = (JTextArea) ((JPanel) getComponent(1)).getComponent(0);
            inventorySpecsArea.setText("Error loading Inventory data: " + e.getMessage());
        }
    }

    private void refreshTableList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        try {
            List<String> tableNames = DatabaseUtils.getTableNames();
            List<String> includedTables = TablesNotIncludedList.getIncludedTablesForInventory();
            List<String> validTables = new ArrayList<>();
            for (String table : tableNames) {
                if (includedTables.contains(table)) {
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
        Set<String> inventoryNonEmptyColumns = new HashSet<>();
        try {
            HashMap<String, String> inventoryEntry = DatabaseUtils.getDeviceByAssetName("Inventory", assetName);
            if (inventoryEntry != null && !inventoryEntry.isEmpty()) {
                for (String column : inventoryEntry.keySet()) {
                    String value = inventoryEntry.get(column);
                    if (value != null && !value.trim().isEmpty()) {
                        inventoryNonEmptyColumns.add(column);
                    }
                }
                LOGGER.log(Level.INFO, "Inventory columns with non-empty values: {0}", inventoryNonEmptyColumns);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying Inventory table: {0}", e.getMessage());
            JLabel errorLabel = new JLabel("Error loading data: " + e.getMessage());
            dataListPanel.add(errorLabel);
            dataListPanel.revalidate();
            dataListPanel.repaint();
            return;
        }

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
            if (!column.equals("AssetName") && !column.equals("TableName") && !inventoryNonEmptyColumns.contains(column)) {
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
}