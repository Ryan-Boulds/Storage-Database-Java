import java.awt.Component;
import java.io.IOException;
import java.sql.SQLException;
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
import log_chargers.LogChargersTab;
import mass_entry_modifier.MassEntryModifierTab;
import utils.DatabaseUtils;
import utils.UIComponentUtils;
import view_inventory_tab.ViewInventoryTab;
import view_software_list_tab.ViewSoftwareListTab;

public class mainFile {
    private static final Logger LOGGER = Logger.getLogger(mainFile.class.getName());

    public static void main(String[] args) {
        // Check Java version
        String javaVersion = System.getProperty("java.version");
        if (!javaVersion.startsWith("1.8") && !javaVersion.startsWith("11") && !javaVersion.startsWith("17")) {
            JOptionPane.showMessageDialog(null, "This application requires Java 8, 11, or 17.", "Java Version Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Load logging configuration
        try {
            LogManager.getLogManager().readConfiguration(mainFile.class.getResourceAsStream("/logging.properties"));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load logging properties, using default console logging: {0}", e.getMessage());
            java.util.logging.ConsoleHandler handler = new java.util.logging.ConsoleHandler();
            handler.setLevel(Level.ALL);
            Logger.getLogger("").addHandler(handler);
            Logger.getLogger("").setLevel(Level.INFO);
        }

        SwingUtilities.invokeLater(() -> {
            // Prompt for database path
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new java.io.File(System.getProperty("user.home")));
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Access Database Files (*.accdb)", "accdb");
            fileChooser.setFileFilter(filter);
            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                java.io.File selectedFile = fileChooser.getSelectedFile();
                if (!selectedFile.exists() || !selectedFile.getName().endsWith(".accdb")) {
                    JOptionPane.showMessageDialog(null, "Invalid or non-existent database file selected. Please choose a valid .accdb file.", "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                String selectedPath = selectedFile.getAbsolutePath();
                LoadingWindow loadingWindow = new LoadingWindow();
                loadingWindow.appendLog("Selected database file: " + selectedPath);

                // Perform initialization in a background thread
                new Thread(() -> {
                    try {
                        loadingWindow.appendLog("Setting database path...");
                        DatabaseUtils.setDatabasePath(selectedPath);
                        loadingWindow.appendLog("Testing database connection...");
                        try (java.sql.Connection conn = DatabaseUtils.getConnection()) {
                            loadingWindow.appendLog("Database connection successful.");
                        } catch (SQLException e) {
                            String message = e.getMessage().contains("UCAExc") ? "Invalid or corrupted Access database file: " + e.getMessage() : "Database error: " + e.getMessage();
                            LOGGER.log(Level.SEVERE, message);
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(null, message, "Database Error", JOptionPane.ERROR_MESSAGE);
                                loadingWindow.close();
                                System.exit(1);
                            });
                            return;
                        }

                        loadingWindow.appendLog("Initializing DatabaseCreatorTab...");
                        DatabaseCreatorTab databaseCreatorTab = new DatabaseCreatorTab();

                        loadingWindow.appendLog("Creating UI components...");
                        JLabel statusLabel = new JLabel("Ready");
                        ViewInventoryTab viewInventoryTab = new ViewInventoryTab();
                        ViewSoftwareListTab viewSoftwareListTab = new ViewSoftwareListTab();
                        AccessoriesCountTab accessoriesCountTab = new AccessoriesCountTab();
                        LogCablesTab logCablesTab = new LogCablesTab();
                        LogAdaptersTab logAdaptersTab = new LogAdaptersTab();
                        LogChargersTab logChargersTab = new LogChargersTab();
                        MassEntryModifierTab massEntryModifierTab = new MassEntryModifierTab(statusLabel);

                        loadingWindow.appendLog("Creating main frame...");
                        JFrame frame = UIComponentUtils.createMainFrame(
                            "Aisin Inventory Manager - Work in Progress - Report issues to r-boulds@aisinil.com",
                            databaseCreatorTab,
                            viewInventoryTab,
                            viewSoftwareListTab,
                            accessoriesCountTab,
                            logCablesTab,
                            logAdaptersTab,
                            logChargersTab,
                            massEntryModifierTab
                        );

                        JTabbedPane tabbedPane = (JTabbedPane) frame.getContentPane().getComponent(0);
                        tabbedPane.setTitleAt(tabbedPane.indexOfComponent(viewSoftwareListTab), "View Software List");
                        tabbedPane.setTitleAt(tabbedPane.indexOfComponent(logChargersTab), "Log Chargers");
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
                            } else if (selected == logChargersTab) {
                                logChargersTab.refresh();
                            }
                        });

                        massEntryModifierTab.addPropertyChangeListener("inventoryUpdated", evt -> {
                            viewInventoryTab.refreshDataAndTabs();
                            viewSoftwareListTab.refreshDataAndTabs();
                        });

                        frame.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosing(java.awt.event.WindowEvent e) {
                                System.exit(0);
                            }
                        });

                        loadingWindow.appendLog("Initialization complete.");
                        try {
                            Thread.sleep(1500);
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