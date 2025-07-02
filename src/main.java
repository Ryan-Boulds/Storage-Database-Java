import javax.swing.*;

public class main {

    public static void main(String[] args) {
        // Set up the frame
        JFrame frame = new JFrame("Tabbed Pane Example");
        frame.setSize(600, 600);

        // Create a tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Add Log New Device and Log Accessories tabs
        tabbedPane.addTab("Log New Device", new LogNewDeviceTab());
        tabbedPane.addTab("Log Accessories", new LogAccessoriesTab());

        // Create an instance of LogCablesTab and add it to the tabbedPane
        tabbedPane.addTab("Log Cables", new LogCablesTab());

        // Add the tabbed pane to the frame
        frame.add(tabbedPane);

        // Set default close operation and display the frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Listen for window closing event and ensure proper shutdown
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                System.exit(0); // Ensures JVM exits when window is closed
            }
        });
    }
}
