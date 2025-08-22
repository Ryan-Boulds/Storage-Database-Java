package view_software_list_tab;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
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
import javax.swing.table.TableRowSorter;

import view_software_list_tab.import_spreadsheet_file.ImportDataTab;
import view_software_list_tab.license_key_tracker.LicenseKeyTracker;
import view_software_list_tab.view_software_details.DeviceDetailsPanel;

public class ViewSoftwareListTab extends JPanel {

    private final JTable table;
    protected final TableManager tableManager;
    private final JPanel mainPanel;
    private JComponent currentView;
    private final JSplitPane mainSplitPane;
    private final ListSelectionListener originalTableListListener;
    private static final Logger LOGGER = Logger.getLogger(ViewSoftwareListTab.class.getName());
    private ImportDataTab importDataTab;
    private FilterPanel filterPanel;
    private TableListPanel tableListPanel;
    private boolean isRefreshing = false;

    public ViewSoftwareListTab() {
        setLayout(new BorderLayout());

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
        add(currentView, BorderLayout.CENTER);

        JLabel statusLabelLocal = new JLabel("Ready");
        importDataTab = new ImportDataTab(statusLabelLocal, this);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Move initialization out of constructor to avoid leaking 'this'
        tableManager.setParentTab(this);
        initializeFilterPanel();

        originalTableListListener = e -> {
            if (!e.getValueIsAdjusting() && !isRefreshing) {
                String selectedTable = tableListPanel.getTableList().getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    isRefreshing = true;
                    try {
                        tableManager.setTableName(selectedTable);
                        tableManager.initializeColumns();
                        if (importDataTab.getTableSelector() != null) {
                            importDataTab.getTableSelector().setSelectedItem(selectedTable);
                        }
                        refreshDataAndTabs();
                        LOGGER.log(Level.INFO, "Selected table changed to: {0}", selectedTable);
                    } finally {
                        isRefreshing = false;
                    }
                } else {
                    tableManager.setTableName(null);
                    table.setModel(new DefaultTableModel());
                    LOGGER.log(Level.WARNING, "No valid table selected");
                }
            }
        };
        tableListPanel.getTableList().addListSelectionListener(originalTableListListener);

        
        PopupHandler.addTablePopup(table, this);
    }

    private void initializeFilterPanel() {
        filterPanel = new FilterPanel(
                (search, status, dept) -> updateTables(search),
                this::refreshDataAndTabs,
                tableManager,
                tableListPanel // Pass tableListPanel to FilterPanel
        );
        JPanel filterPanelWithButtons = new JPanel(new BorderLayout());
        filterPanelWithButtons.add(filterPanel.getPanel(), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(new JButton("License Key Tracker") {
            {
                addActionListener(e -> showLicenseKeyTracker());
            }
        });
        buttonPanel.add(new JButton("Import Spreadsheet") {
            {
                addActionListener(e -> showImportDataTab());
            }
        });
        filterPanelWithButtons.add(buttonPanel, BorderLayout.EAST);
        mainPanel.add(filterPanelWithButtons, BorderLayout.NORTH);
    }

    public void refreshDataAndTabs() {
        if (isRefreshing) {
            LOGGER.log(Level.FINE, "Skipping redundant refreshDataAndTabs call");
            return;
        }
        isRefreshing = true;
        try {
            tableListPanel.updateTableList();
            importDataTab.updateTableSelectorOptions();
            tableManager.refreshDataAndTabs();
        } finally {
            isRefreshing = false;
        }
    }

    public void updateTables(String searchTerm) {
        String currentTable = tableManager.getTableName();
        if (currentTable == null || currentTable.isEmpty()) {
            DefaultListModel<String> model = (DefaultListModel<String>) tableListPanel.getTableList().getModel();
            if (!model.isEmpty() && !model.getElementAt(0).equals("No tables available")) {
                currentTable = model.getElementAt(0);
                tableListPanel.getTableList().setSelectedValue(currentTable, true);
                tableManager.setTableName(currentTable);
                tableManager.initializeColumns();
                if (importDataTab.getTableSelector() != null) {
                    importDataTab.getTableSelector().setSelectedItem(currentTable);
                }
                LOGGER.log(Level.INFO, "Selected first table '{0}' due to null/empty current table", currentTable);
            } else {
                tableManager.setTableName(null);
                table.setModel(new DefaultTableModel());
                LOGGER.log(Level.WARNING, "No valid tables available for updateTables");
            }
        }
        refreshDataAndTabs();
        String searchText = searchTerm.toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        if (sorter != null) {
            javax.swing.RowFilter<DefaultTableModel, Integer> filter = null;
            if (!searchText.isEmpty()) {
                filter = new javax.swing.RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        for (int i = 1; i < entry.getModel().getColumnCount(); i++) {
                            Object value = entry.getValue(i);
                            if (value != null && value.toString().toLowerCase().contains(searchText)) {
                                return true;
                            }
                        }
                        return false;
                    }
                };
            }
            sorter.setRowFilter(filter);
        }
    }

    public void showMainView() {
        remove(currentView);
        currentView = mainSplitPane;
        add(currentView, BorderLayout.CENTER);

        for (ListSelectionListener listener : tableListPanel.getTableList().getListSelectionListeners()) {
            tableListPanel.getTableList().removeListSelectionListener(listener);
        }
        tableListPanel.getTableList().addListSelectionListener(originalTableListListener);

        String currentTable = tableManager.getTableName();
        LOGGER.log(Level.INFO, "showMainView: Current table name is '{0}'", currentTable);
        isRefreshing = true;
        try {
            DefaultListModel<String> model = (DefaultListModel<String>) tableListPanel.getTableList().getModel();
            if (!model.isEmpty() && !model.getElementAt(0).equals("No tables available")) {
                if (currentTable == null || currentTable.isEmpty() || !model.contains(currentTable)) {
                    currentTable = model.getElementAt(0);
                    LOGGER.log(Level.INFO, "showMainView: Selected first available table '{0}'", currentTable);
                }
                tableListPanel.getTableList().setSelectedValue(currentTable, true);
                tableManager.setTableName(currentTable);
                tableManager.initializeColumns();
                if (importDataTab.getTableSelector() != null) {
                    importDataTab.getTableSelector().setSelectedItem(currentTable);
                }
            } else {
                tableListPanel.getTableList().clearSelection();
                tableManager.setTableName(null);
                table.setModel(new DefaultTableModel());
                if (importDataTab.getTableSelector() != null) {
                    importDataTab.getTableSelector().setSelectedItem(null);
                }
                LOGGER.log(Level.WARNING, "showMainView: No valid tables available");
            }
            refreshDataAndTabs();
        } finally {
            isRefreshing = false;
        }
        revalidate();
        repaint();
        LOGGER.log(Level.INFO, "Restored main view with table '{0}'", currentTable);
    }

    private void showLicenseKeyTracker() {
        String tableName = tableManager.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            DefaultListModel<String> model = (DefaultListModel<String>) tableListPanel.getTableList().getModel();
            if (!model.isEmpty() && !model.getElementAt(0).equals("No tables available")) {
                tableName = model.getElementAt(0);
                tableListPanel.getTableList().setSelectedValue(tableName, true);
                tableManager.setTableName(tableName);
                tableManager.initializeColumns();
                if (importDataTab.getTableSelector() != null) {
                    importDataTab.getTableSelector().setSelectedItem(tableName);
                }
                LOGGER.log(Level.INFO, "Selected first table '{0}' for LicenseKeyTracker", tableName);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a valid table first", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, "Attempted to open LicenseKeyTracker without selecting a valid table");
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

    private void showImportDataTab() {
        remove(currentView);
        currentView = importDataTab;
        String currentTable = tableManager.getTableName();
        if (currentTable != null && importDataTab.getTableSelector() != null) {
            importDataTab.getTableSelector().setSelectedItem(currentTable);
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