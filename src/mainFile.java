import device_logging.LogNewDeviceTab;
import javax.swing.*;
import utils.FileUtils;
import utils.UIComponentUtils;

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
                    viewInventoryTab.refreshData();
                    viewInventoryTab.updateTable(null, "All", "All", "All");
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