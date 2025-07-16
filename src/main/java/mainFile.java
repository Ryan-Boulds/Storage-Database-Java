import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import database_creator.DatabaseCreatorTab;
import device_logging.ImportDataTab;
import device_logging.LogNewDeviceTab;
import utils.UIComponentUtils;
import view_inventorytab.AccessoriesCountTab;
import view_inventorytab.ViewInventoryTab;

public class mainFile {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Initialize DatabaseCreatorTab first
            DatabaseCreatorTab databaseCreatorTab = new DatabaseCreatorTab();
            databaseCreatorTab.createMissingTables(); // Ensure tables exist on startup

            JLabel statusLabel = new JLabel("Ready");
            ViewInventoryTab viewInventoryTab = new ViewInventoryTab();
            LogNewDeviceTab logNewDeviceTab = new LogNewDeviceTab();
            AccessoriesCountTab accessoriesCountTab = new AccessoriesCountTab();
            ImportDataTab importDataTab = new ImportDataTab(statusLabel);

            JFrame frame = UIComponentUtils.createMainFrame(
                "Inventory Management",
                databaseCreatorTab, // First tab
                viewInventoryTab,
                logNewDeviceTab,
                accessoriesCountTab,
                importDataTab
            );

            JTabbedPane tabbedPane = (JTabbedPane) frame.getContentPane().getComponent(0);
            tabbedPane.addChangeListener(e -> {
                if (tabbedPane.getSelectedComponent() == viewInventoryTab) {
                    viewInventoryTab.refreshDataAndTabs();
                    // Remove undefined call to updateTables
                } else if (tabbedPane.getSelectedComponent() instanceof AccessoriesCountTab) {
                    AccessoriesCountTab newTab = new AccessoriesCountTab();
                    tabbedPane.setComponentAt(tabbedPane.indexOfTab("AccessoriesCount"), newTab);
                }
            });

            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    System.exit(0);
                }
            });

            frame.setVisible(true);
        });
    }
}