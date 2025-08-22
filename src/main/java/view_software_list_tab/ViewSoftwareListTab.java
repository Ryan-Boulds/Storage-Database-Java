package view_software_list_tab;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public ViewSoftwareListTab() {
        setLayout(new BorderLayout());

        // Initialize main panel for filter and table
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

        // Initialize table list panel
        tableListPanel = new TableListPanel(this);

        // Initialize split pane for left (software list) and right (main panel)
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        mainSplitPane.setDividerLocation(200);
        mainSplitPane.setResizeWeight(0.3);

        // Set up split pane
        mainSplitPane.setLeftComponent(tableListPanel);
        mainSplitPane.setRightComponent(mainPanel);
        currentView = mainSplitPane;
        add(currentView, BorderLayout.CENTER);

        // Initialize status label for ImportDataTab
        JLabel statusLabelLocal = new JLabel("Ready");

        // Initialize ImportDataTab with reference to this ViewSoftwareListTab
        importDataTab = new ImportDataTab(statusLabelLocal, this);

        // Initialize main panel with table
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Initialize filter panel after all fields are initialized
        initializeFilterPanel();

        // Initialize listeners
        originalTableListListener = e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = tableListPanel.getTableList().getSelectedValue();
                if (selectedTable != null && !selectedTable.startsWith("Error") && !selectedTable.equals("No tables available")) {
                    tableManager.setTableName(selectedTable);
                    if (importDataTab.getTableSelector() != null) {
                        importDataTab.getTableSelector().setSelectedItem(selectedTable);
                    }
                    refreshDataAndTabs();
                }
            }
        };
        tableListPanel.getTableList().addListSelectionListener(originalTableListListener);

        // Attach popup menu handler
        PopupHandler.addTablePopup(table, this);
    }

    private void initializeFilterPanel() {
        filterPanel = new FilterPanel(
                (search, status, dept) -> updateTables(search),
                this::refreshDataAndTabs,
                tableManager
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
        tableManager.refreshDataAndTabs();
        tableListPanel.updateTableList();
        importDataTab.updateTableSelectorOptions();
    }

    public void updateTables(String searchTerm) {
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

    public void showDeviceDetails(String assetName) {
        remove(currentView);
        currentView = new DeviceDetailsPanel(assetName, this);
        add(currentView, BorderLayout.CENTER);
        revalidate();
        repaint();
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
        if (currentTable != null) {
            tableListPanel.getTableList().setSelectedValue(currentTable, true);
            tableManager.setTableName(currentTable);
            if (importDataTab.getTableSelector() != null) {
                importDataTab.getTableSelector().setSelectedItem(currentTable);
            }
            refreshDataAndTabs();
        } else {
            tableListPanel.getTableList().clearSelection();
            tableManager.setTableName(null);
            if (importDataTab.getTableSelector() != null) {
                importDataTab.getTableSelector().setSelectedItem(null);
            }
            refreshDataAndTabs();
        }

        revalidate();
        repaint();
    }

    private void showLicenseKeyTracker() {
        String tableName = tableManager.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a valid table first", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.WARNING, "Attempted to open LicenseKeyTracker without selecting a valid table");
            return;
        }
        remove(currentView);
        currentView = new LicenseKeyTracker(this, tableManager);
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
}
