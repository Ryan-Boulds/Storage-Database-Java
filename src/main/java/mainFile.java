

import java.awt.Component;
import java.io.IOException;
import java.util.logging.LogManager;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import accessories_count.AccessoriesCountTab;
import data_import.ImportDataTab;
import database_creator.DatabaseCreatorTab;
import device_logging.LogNewDeviceTab;
import log_adapters.LogAdaptersTab;
import log_cables.LogCablesTab;
import mass_entry_modifier.MassEntryModifierTab;
import utils.DatabaseUtils;
import utils.UIComponentUtils;
import view_inventorytab.ViewInventoryTab;

public class mainFile {
    public static void main(String[] args) {
        // Load logging configuration from resources
        System.setProperty("java.util.logging.config.file", "src/main/resources/logging.properties");
        try {
            LogManager.getLogManager().readConfiguration();
        } catch (IOException e) {
            System.err.println("Could not load logging properties: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            // Prompt for database path
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new java.io.File("C:/Users/ami6985/OneDrive - AISIN WORLD CORP/Documents"));
            fileChooser.setSelectedFile(new java.io.File("C:/Users/ami6985/OneDrive - AISIN WORLD CORP/Documents/InventoryManagement.accdb"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Access Database Files (*.accdb)", "accdb");
            fileChooser.setFileFilter(filter);
            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                String selectedPath = fileChooser.getSelectedFile().getAbsolutePath();
                DatabaseUtils.setDatabasePath(selectedPath);

                // Initialize DatabaseCreatorTab first
                DatabaseCreatorTab databaseCreatorTab = new DatabaseCreatorTab();
                databaseCreatorTab.createMissingTables(); // Ensure tables exist on startup

                JLabel statusLabel = new JLabel("Ready");
                ViewInventoryTab viewInventoryTab = new ViewInventoryTab();
                LogNewDeviceTab logNewDeviceTab = new LogNewDeviceTab();
                AccessoriesCountTab accessoriesCountTab = new AccessoriesCountTab();
                LogCablesTab logCablesTab = new LogCablesTab();
                LogAdaptersTab logAdaptersTab = new LogAdaptersTab();
                ImportDataTab importDataTab = new ImportDataTab(statusLabel);
                MassEntryModifierTab massEntryModifierTab = new MassEntryModifierTab(statusLabel);

                JFrame frame = UIComponentUtils.createMainFrame(
                    "Inventory Management",
                    databaseCreatorTab,
                    viewInventoryTab,
                    logNewDeviceTab,
                    accessoriesCountTab,
                    logCablesTab,
                    logAdaptersTab,
                    importDataTab,
                    massEntryModifierTab
                );

                JTabbedPane tabbedPane = (JTabbedPane) frame.getContentPane().getComponent(0);
                tabbedPane.addChangeListener(e -> {
                    Component selected = tabbedPane.getSelectedComponent();
                    if (selected == viewInventoryTab) {
                        viewInventoryTab.refreshDataAndTabs();
                    } else if (selected == massEntryModifierTab) {
                        massEntryModifierTab.refresh();
                    } else if (selected == accessoriesCountTab) {
                        accessoriesCountTab.refresh();
                    } else if (selected == logCablesTab) {
                        logCablesTab.refresh();
                    } else if (selected == logAdaptersTab) {
                        logAdaptersTab.refresh();
                    }
                });

                // Listen for updates from MassEntryModifierTab
                massEntryModifierTab.addPropertyChangeListener("inventoryUpdated", evt -> {
                    viewInventoryTab.refreshDataAndTabs();
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

                frame.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(null, "No database file selected. Application will exit.", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        });
    }
}