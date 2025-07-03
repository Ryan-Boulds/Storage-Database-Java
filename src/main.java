import javax.swing.*;

public class main {
    public static void main(String[] args) {
        // Create tabs
        LogNewDeviceTab deviceTab = new LogNewDeviceTab();
        LogAccessoriesTab accessoriesTab = new LogAccessoriesTab();
        LogCablesTab cablesTab = new LogCablesTab();

        // Create and display the main frame using UIUtils
        JFrame frame = UIUtils.createMainFrame("Device Management System", 
            deviceTab, accessoriesTab, cablesTab);
        frame.setVisible(true);
    }
}