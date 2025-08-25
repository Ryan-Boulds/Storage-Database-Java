package view_software_list_tab;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import view_software_list_tab.import_spreadsheet_file.ImportDataTab;
import view_software_list_tab.license_key_tracker.LicenseKeyTracker;
import view_software_list_tab.view_software_details.DeviceDetailsPanel;

public class ViewSoftwareListTab extends JPanel {

    private JTable table;
    private TableManager tableManager;
    private JPanel mainPanel;
    private JComponent currentView;
    private JSplitPane mainSplitPane;
    private ListSelectionListener tableListListener;
    private static final Logger LOGGER = Logger.getLogger(ViewSoftwareListTab.class.getName());
    private ImportDataTab importDataTab;
    private FilterPanel filterPanel;
    private TableListPanel tableListPanel;
    private boolean isRefreshing = false;
    private String lastSelectedTable = null; // Store last selected table

    @SuppressWarnings("LeakingThisInConstructor")
    public ViewSoftwareListTab() {
        setLayout(new BorderLayout());
        initializeComponents();
        add(currentView, BorderLayout.CENTER);
        tableManager.setParentTab(this);
        initializeFilterPanel();
        setupTableListListener();
        PopupHandler.addTablePopup(table, this);
    }

    private void initializeComponents() {
        mainPanel = new JPanel(new BorderLayout());
        table = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tableManager = new TableManager(table);
        JScrollPane scrollPane = new JScrollPane(table);

        tableListPanel = new TableListPanel(this);
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        mainSplitPane.setDividerLocation(200);
        mainSplitPane.setResizeWeight(0.3);
        mainSplitPane.setLeftComponent(tableListPanel);
        mainSplitPane.setRightComponent(mainPanel);
        currentView = mainSplitPane;
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JLabel statusLabelLocal = new JLabel("Ready");
        importDataTab = new ImportDataTab(statusLabelLocal, this);
        // Ensure no table is selected initially
        tableListPanel.getTableList().clearSelection();
        tableManager.setTableName(null);
        table.setModel(new DefaultTableModel());
    }

    private void initializeFilterPanel() {
        filterPanel = new FilterPanel(
            (search, status, dept) -> {
                tableManager.setWhereClause(search, status, dept);
                tableManager.refreshDataAndTabs();
            },
            () -> {
                isRefreshing = true;
                try {
                    tableManager.refreshDataAndTabs();
                    tableListPanel.updateTableList();
                } finally {
                    isRefreshing = false;
                }
            },
            tableManager,
            tableListPanel,
            this
        );
        mainPanel.add(filterPanel.filterPanel, BorderLayout.NORTH);
    }

    private void setupTableListListener() {
        tableListListener = e -> {
            if (!e.getValueIsAdjusting() && !isRefreshing) {
                String selectedTable = tableListPanel.getTableList().getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    isRefreshing = true;
                    try {
                        lastSelectedTable = selectedTable; // Store selected table
                        tableManager.setTableName(selectedTable);
                        if (importDataTab.getTableList() != null) {
                            importDataTab.getTableList().setSelectedValue(selectedTable, true);
                        }
                        tableManager.refreshDataAndTabs();
                        LOGGER.log(Level.INFO, "Selected table changed to: {0}", selectedTable);
                    } finally {
                        isRefreshing = false;
                    }
                } else {
                    tableManager.setTableName(null);
                    table.setModel(new DefaultTableModel());
                    if (importDataTab.getTableList() != null) {
                        importDataTab.getTableList().setSelectedValue(null, true);
                    }
                    lastSelectedTable = null; // Clear last selected table
                    LOGGER.log(Level.WARNING, "No valid table selected");
                }
            }
        };
        tableListPanel.getTableList().addListSelectionListener(tableListListener);
    }

    public void refreshDataAndTabs() {
        if (!isRefreshing) {
            isRefreshing = true;
            try {
                tableManager.refreshDataAndTabs();
                tableListPanel.updateTableList();
                if (importDataTab.getTableList() != null) {
                    String currentTable = tableManager.getTableName();
                    if (currentTable != null) {
                        importDataTab.getTableList().setSelectedValue(currentTable, true);
                    }
                }
                table.revalidate();
                table.repaint();
                LOGGER.log(Level.INFO, "Refreshed data and tabs for table '{0}'", tableManager.getTableName());
            } finally {
                isRefreshing = false;
            }
        }
    }

    public void showMainView() {
        // Remove existing listener to prevent recursive calls
        if (tableListListener != null) {
            tableListPanel.getTableList().removeListSelectionListener(tableListListener);
        }

        // Reinitialize components to mimic first-time load
        removeAll();
        initializeComponents();
        tableManager.setParentTab(this);
        initializeFilterPanel();
        setupTableListListener();
        PopupHandler.addTablePopup(table, this);

        add(currentView, BorderLayout.CENTER);
        // Restore last selected table if available
        if (lastSelectedTable != null) {
            tableListPanel.getTableList().setSelectedValue(lastSelectedTable, true);
            tableManager.setTableName(lastSelectedTable);
            tableManager.refreshDataAndTabs();
        } else {
            tableListPanel.getTableList().clearSelection();
            tableManager.setTableName(null);
            table.setModel(new DefaultTableModel());
        }
        revalidate();
        repaint();
        LOGGER.log(Level.INFO, "Reinitialized main view with table: {0}", lastSelectedTable);
    }

    public void showLicenseKeyTracker() {
    String tableName = tableManager.getTableName();
    if (tableName == null || tableName.isEmpty()) {
        DefaultListModel<String> model = (DefaultListModel<String>) tableListPanel.getTableList().getModel();
        if (!model.isEmpty() && !model.getElementAt(0).equals("No tables available")) {
            tableName = model.getElementAt(0);
            tableListPanel.getTableList().setSelectedValue(tableName, true);
            tableManager.setTableName(tableName);
            lastSelectedTable = tableName; // Store first table
            if (importDataTab.getTableList() != null) {
                importDataTab.getTableList().setSelectedValue(tableName, true);
            }
            LOGGER.log(Level.INFO, "Selected first table '{0}' for LicenseKeyTracker", tableName);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a valid table first", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.WARNING, "Attempted to open LicenseKeyTracker without selecting a valid table");
            return;
        }
    }

    // Check if License_Key column exists or can be created
    String[] columns = tableManager.getColumns();
    boolean hasLicenseKeyColumn = false;
    for (String column : columns) {
        if (column.equalsIgnoreCase("License_Key")) {
            hasLicenseKeyColumn = true;
            break;
        }
    }

    if (!hasLicenseKeyColumn) {
        LicenseKeyTracker tempTracker = new LicenseKeyTracker(this, tableManager);
        // If the tracker was not initialized (e.g., user clicked "Go Back"), return to main view
        if (tempTracker.getComponentCount() == 0) { // Check if UI was not initialized
            showMainView();
            return;
        }
    }

    remove(currentView);
    LicenseKeyTracker tracker = new LicenseKeyTracker(this, tableManager);
    currentView = tracker;
    tableManager.setLicenseKeyTracker(tracker);
    add(currentView, BorderLayout.CENTER);
    revalidate();
    repaint();
    LOGGER.log(Level.INFO, "Opened LicenseKeyTracker for table '{0}'", tableName);
}

    public void showImportDataTab() {
        remove(currentView);
        currentView = importDataTab;
        String currentTable = tableManager.getTableName();
        if (currentTable != null && importDataTab.getTableList() != null) {
            importDataTab.getTableList().setSelectedValue(currentTable, true);
        }
        add(currentView, BorderLayout.CENTER);
        revalidate();
        repaint();
        LOGGER.log(Level.INFO, "Switched to ImportDataTab view");
    }

    public TableManager getTableManager() {
        return tableManager;
    }

    public JComponent getCurrentView() {
        return currentView;
    }

    public void setCurrentView(JComponent currentView) {
        this.currentView = currentView;
    }

    public ImportDataTab getImportDataTab() {
        return importDataTab;
    }

    public TableListPanel getTableListPanel() {
        return tableListPanel;
    }

    public void showDeviceDetails(String assetName) {
        remove(currentView);
        currentView = new DeviceDetailsPanel(assetName, this);
        add(currentView, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}