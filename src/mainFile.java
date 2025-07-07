import javax.swing.*;

public class mainFile {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Inventory Management");
            frame.setSize(1200, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JTabbedPane tabbedPane = new JTabbedPane();
            ViewInventoryTab viewInventoryTab = new ViewInventoryTab();
            LogNewDeviceTab logNewDeviceTab = new LogNewDeviceTab();
            LogCablesTab logCablesTab = new LogCablesTab();
            LogAccessoriesTab logAccessoriesTab = new LogAccessoriesTab();
            AccessoriesCountTab accessoriesCountTab = new AccessoriesCountTab();
            ImportDataTab importDataTab = new ImportDataTab();

            tabbedPane.addTab("ViewInventory", viewInventoryTab);
            tabbedPane.addTab("LogNewDevice", logNewDeviceTab);
            tabbedPane.addTab("LogCables", logCablesTab);
            tabbedPane.addTab("LogAccessories", logAccessoriesTab);
            tabbedPane.addTab("AccessoriesCount", accessoriesCountTab);
            tabbedPane.addTab("ImportData", importDataTab);

            tabbedPane.addChangeListener(e -> {
                if (tabbedPane.getSelectedComponent() == viewInventoryTab) {
                    viewInventoryTab.refreshData();
                    viewInventoryTab.updateTable(null, "All", "All", "All");
                } else if (tabbedPane.getSelectedComponent() instanceof AccessoriesCountTab) {
                    AccessoriesCountTab newTab = new AccessoriesCountTab();
                    tabbedPane.setComponentAt(tabbedPane.indexOfTab("AccessoriesCount"), newTab);
                }
            });

            frame.add(tabbedPane);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    UIUtils.saveDevices();
                    UIUtils.saveCables();
                    UIUtils.saveAccessories();
                    UIUtils.saveTemplates();
                    System.exit(0);
                }
            });

            frame.setVisible(true);
        });
    }
}