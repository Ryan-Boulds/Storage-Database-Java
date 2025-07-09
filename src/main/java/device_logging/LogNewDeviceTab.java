package device_logging;

import java.awt.*;
import javax.swing.*;
import utils.UIComponentUtils;

public class LogNewDeviceTab extends JPanel {
    private JPanel contentPanel;
    private JLabel statusLabel;
    private ComputerPanel computerPanel;
    private PrinterPanel printerPanel;
    private RouterPanel routerPanel;
    private SwitchPanel switchPanel;

    public LogNewDeviceTab() {
        setLayout(new BorderLayout(10, 10));

        JComboBox<String> deviceTypeCombo = UIComponentUtils.createFormattedComboBox(new String[]{"Computer", "Printer", "Router", "Switch"});
        JPanel topPanel = new JPanel();
        topPanel.add(UIComponentUtils.createAlignedLabel("Select Device Type:"));
        topPanel.add(deviceTypeCombo);

        contentPanel = new JPanel();
        JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(contentPanel);
        statusLabel = UIComponentUtils.createAlignedLabel("");

        computerPanel = new ComputerPanel(statusLabel);
        printerPanel = new PrinterPanel(statusLabel);
        routerPanel = new RouterPanel(statusLabel);
        switchPanel = new SwitchPanel(statusLabel);

        contentPanel.add(computerPanel);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        deviceTypeCombo.addActionListener(e -> {
            String selectedDeviceType = (String) deviceTypeCombo.getSelectedItem();
            contentPanel.removeAll();
            switch (selectedDeviceType) {
                case "Computer": contentPanel.add(computerPanel); break;
                case "Printer": contentPanel.add(printerPanel); break;
                case "Router": contentPanel.add(routerPanel); break;
                case "Switch": contentPanel.add(switchPanel); break;
            }
            contentPanel.revalidate();
            contentPanel.repaint();
            statusLabel.setText("");
        });

        deviceTypeCombo.setSelectedItem("Computer");
    }
}