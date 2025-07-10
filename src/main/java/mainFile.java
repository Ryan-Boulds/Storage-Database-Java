import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import device_logging.LogNewDeviceTab;
import utils.FileUtils;
import utils.UIComponentUtils;
import view_inventorytab.ViewInventoryTab;

public class mainFile {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ViewInventoryTab viewInventoryTab = new ViewInventoryTab();
            LogNewDeviceTab logNewDeviceTab = new LogNewDeviceTab();
            LogCablesTab logCablesTab = new LogCablesTab();
            LogAccessoriesTab logAccessoriesTab = new LogAccessoriesTab();
            AccessoriesCountTab accessoriesCountTab = new AccessoriesCountTab();
            ImportDataTab importDataTab = new ImportDataTab();

            JFrame frame = UIComponentUtils.createMainFrame(
                "Inventory Management",
                viewInventoryTab,
                logNewDeviceTab,
                logCablesTab,
                logAccessoriesTab,
                accessoriesCountTab,
                importDataTab
            );

            JTabbedPane tabbedPane = (JTabbedPane) frame.getContentPane().getComponent(0);
            tabbedPane.addChangeListener(e -> {
                if (tabbedPane.getSelectedComponent() == viewInventoryTab) {
                    viewInventoryTab.refreshDataAndTabs();
                    viewInventoryTab.updateTables("", "All", "All");
                } else if (tabbedPane.getSelectedComponent() instanceof AccessoriesCountTab) {
                    AccessoriesCountTab newTab = new AccessoriesCountTab();
                    tabbedPane.setComponentAt(tabbedPane.indexOfTab("AccessoriesCount"), newTab);
                }
            });

            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    FileUtils.saveDevices();
                    FileUtils.saveCables();
                    FileUtils.saveAccessories();
                    FileUtils.saveTemplates();
                    System.exit(0);
                }
            });

            frame.setVisible(true);
        });
    }
}