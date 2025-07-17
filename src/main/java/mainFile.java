import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import data_import.ImportDataTab;
import database_creator.DatabaseCreatorTab;
import device_logging.LogNewDeviceTab;
import utils.DatabaseUtils;
import utils.UIComponentUtils;
import view_inventorytab.AccessoriesCountTab;
import view_inventorytab.LogAdaptersTab;
import view_inventorytab.LogCablesTab;
import view_inventorytab.ViewInventoryTab;

public class mainFile {
    public static void main(String[] args) {
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

                JFrame frame = UIComponentUtils.createMainFrame(
                    "Inventory Management",
                    databaseCreatorTab, // First tab
                    viewInventoryTab,
                    logNewDeviceTab,
                    accessoriesCountTab,
                    logCablesTab,
                    logAdaptersTab,
                    importDataTab
                );

                JTabbedPane tabbedPane = (JTabbedPane) frame.getContentPane().getComponent(0);
                tabbedPane.addChangeListener(e -> {
                    if (tabbedPane.getSelectedComponent() == viewInventoryTab) {
                        viewInventoryTab.refreshDataAndTabs();
                    } else if (tabbedPane.getSelectedComponent() instanceof AccessoriesCountTab) {
                        AccessoriesCountTab newTab = new AccessoriesCountTab();
                        tabbedPane.setComponentAt(tabbedPane.indexOfTab("AccessoriesCount"), newTab);
                    } else if (tabbedPane.getSelectedComponent() instanceof LogCablesTab) {
                        LogCablesTab newTab = new LogCablesTab();
                        tabbedPane.setComponentAt(tabbedPane.indexOfTab("LogCables"), newTab);
                    }
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