import java.awt.Component;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import accessories_count.AccessoriesCountTab;
import database_creator.DatabaseCreatorTab;
import log_adapters.LogAdaptersTab;
import log_cables.LogCablesTab;
import mass_entry_modifier.MassEntryModifierTab;
import utils.DatabaseUtils;
import utils.UIComponentUtils;
import view_inventory_tab.ViewInventoryTab;
import view_software_list_tab.ViewSoftwareListTab;

public class mainFile {
    private static final Logger LOGGER = Logger.getLogger(mainFile.class.getName());

    public static void main(String[] args) {
        // Load logging configuration from resources
        System.setProperty("java.util.logging.config.file", "src/main/resources/logging.properties");
        try {
            LogManager.getLogManager().readConfiguration();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not load logging properties: {0}", e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            // Prompt for database path
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new java.io.File(System.getProperty("user.home")));
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Access Database Files (*.accdb)", "accdb");
            fileChooser.setFileFilter(filter);
            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                String selectedPath = fileChooser.getSelectedFile().getAbsolutePath();
                LoadingWindow loadingWindow = new LoadingWindow();
                loadingWindow.appendLog("Selected database file: " + selectedPath);

                // Perform initialization in a background thread
                new Thread(() -> {
                    try {
                        loadingWindow.appendLog("Setting database path...");
                        DatabaseUtils.setDatabasePath(selectedPath);

                        loadingWindow.appendLog("Initializing DatabaseCreatorTab...");
                        DatabaseCreatorTab databaseCreatorTab = new DatabaseCreatorTab();
                        // No explicit createMissingTables() call, handled in DatabaseCreatorTab constructor

                        loadingWindow.appendLog("Creating UI components...");
                        JLabel statusLabel = new JLabel("Ready");
                        ViewInventoryTab viewInventoryTab = new ViewInventoryTab();
                        ViewSoftwareListTab viewSoftwareListTab = new ViewSoftwareListTab();
                        //LogNewDeviceTab logNewDeviceTab = new LogNewDeviceTab();
                        AccessoriesCountTab accessoriesCountTab = new AccessoriesCountTab();
                        LogCablesTab logCablesTab = new LogCablesTab();
                        LogAdaptersTab logAdaptersTab = new LogAdaptersTab();
                        inventory_data_importer.ImportDataTab importDataTab = new inventory_data_importer.ImportDataTab(statusLabel);
                        // Removed: data_importing_tabs.ImportDataTab softwareImportDataTab = new data_importing_tabs.ImportDataTab(statusLabel);
                        MassEntryModifierTab massEntryModifierTab = new MassEntryModifierTab(statusLabel);

                        loadingWindow.appendLog("Creating main frame...");
                        JFrame frame = UIComponentUtils.createMainFrame(
                            "Inventory Management",
                            databaseCreatorTab,
                            viewInventoryTab,
                            viewSoftwareListTab,
                            //logNewDeviceTab,
                            accessoriesCountTab,
                            logCablesTab,
                            logAdaptersTab,
                            importDataTab,
                            // Removed: softwareImportDataTab,
                            massEntryModifierTab
                        );

                        JTabbedPane tabbedPane = (JTabbedPane) frame.getContentPane().getComponent(0);
                        tabbedPane.setTitleAt(tabbedPane.indexOfComponent(importDataTab), "Import Inventory Data");
                        // Removed: tabbedPane.setTitleAt(tabbedPane.indexOfComponent(softwareImportDataTab), "Import Software Data");
                        tabbedPane.setTitleAt(tabbedPane.indexOfComponent(viewSoftwareListTab), "View Software List");
                        tabbedPane.addChangeListener(e -> {
                            Component selected = tabbedPane.getSelectedComponent();
                            if (selected == viewInventoryTab) {
                                viewInventoryTab.refreshDataAndTabs();
                            } else if (selected == viewSoftwareListTab) {
                                viewSoftwareListTab.refreshDataAndTabs();
                            } else if (selected == massEntryModifierTab) {
                                massEntryModifierTab.refresh();
                            } else if (selected == accessoriesCountTab) {
                                accessoriesCountTab.refresh();
                            } else if (selected == logCablesTab) {
                                logCablesTab.refresh();
                            } else if (selected == logAdaptersTab) {
                                logAdaptersTab.refresh();
                            }
                            // Removed: else if (selected == softwareImportDataTab) { ... }
                        });

                        // Listen for updates from MassEntryModifierTab
                        massEntryModifierTab.addPropertyChangeListener("inventoryUpdated", evt -> {
                            viewInventoryTab.refreshDataAndTabs();
                            viewSoftwareListTab.refreshDataAndTabs();
                            // Optionally refresh other tabs if they depend on Inventory data
                            // accessoriesCountTab.refresh();
                            // logCablesTab.refresh();
                            // logAdaptersTab.refresh();
                        });

                        frame.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosing(java.awt.event.WindowEvent e) {
                                System.exit(0);
                            }
                        });

                        loadingWindow.appendLog("Initialization complete.");
                        // Add a short delay to show logs before closing
                        try {
                            Thread.sleep(1000); // 1-second delay to ensure logs are visible
                        } catch (InterruptedException e) {
                            LOGGER.log(Level.WARNING, "Delay interrupted: {0}", e.getMessage());
                        }
                        SwingUtilities.invokeLater(() -> {
                            loadingWindow.close();
                            frame.setVisible(true);
                        });
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Initialization error: {0}", e.getMessage());
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null, "Error initializing application: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            loadingWindow.close();
                            System.exit(1);
                        });
                    }
                }).start();
            } else {
                JOptionPane.showMessageDialog(null, "No database file selected. Application will exit.", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        });
    }
}