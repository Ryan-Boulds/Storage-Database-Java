import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import utils.FileUtils;
import utils.InventoryData;
import utils.PeripheralUtils;
import utils.UIComponentUtils;

public final class AccessoriesCountTab extends JPanel {
    private final JPanel panel;

    public AccessoriesCountTab() {
        setLayout(new BorderLayout(10, 10));
        refreshData();

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(UIComponentUtils.createAlignedLabel("Accessories:"));
        ArrayList<HashMap<String, String>> accessories = InventoryData.getAccessories();
        for (String type : PeripheralUtils.getPeripheralTypes(accessories)) {
            int count = PeripheralUtils.getPeripheralCount(type, accessories);
            if (count > 0) {
                panel.add(UIComponentUtils.createAlignedLabel(type + ": " + count));
            }
        }

        panel.add(UIComponentUtils.createAlignedLabel("Cables:"));
        ArrayList<HashMap<String, String>> cables = InventoryData.getCables();
        for (HashMap<String, String> cable : cables) {
            String type = cable.getOrDefault("Peripheral_Type", "").toLowerCase();
            String countStr = cable.getOrDefault("Count", "0");
            if (!type.isEmpty() && !countStr.equals("0")) {
                int count = Integer.parseInt(countStr);
                panel.add(UIComponentUtils.createAlignedLabel(type + ": " + count));
            }
        }

        JButton refreshButton = UIComponentUtils.createFormattedButton("Refresh");
        refreshButton.addActionListener(e -> {
            refreshData();
            updateDisplay();
        });

        JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(panel);
        add(scrollPane, BorderLayout.CENTER);
        add(refreshButton, BorderLayout.SOUTH);
    }

    public void refreshData() {
        FileUtils.loadDevices();
        FileUtils.loadCables();
        FileUtils.loadAccessories();
    }

    private void updateDisplay() {
        panel.removeAll();
        panel.add(UIComponentUtils.createAlignedLabel("Accessories:"));
        ArrayList<HashMap<String, String>> accessories = InventoryData.getAccessories();
        for (String type : PeripheralUtils.getPeripheralTypes(accessories)) {
            int count = PeripheralUtils.getPeripheralCount(type, accessories);
            if (count > 0) {
                panel.add(UIComponentUtils.createAlignedLabel(type + ": " + count));
            }
        }
        panel.add(UIComponentUtils.createAlignedLabel("Cables:"));
        ArrayList<HashMap<String, String>> cables = InventoryData.getCables();
        for (HashMap<String, String> cable : cables) {
            String type = cable.getOrDefault("Peripheral_Type", "").toLowerCase();
            String countStr = cable.getOrDefault("Count", "0");
            if (!type.isEmpty() && !countStr.equals("0")) {
                int count = Integer.parseInt(countStr);
                panel.add(UIComponentUtils.createAlignedLabel(type + ": " + count));
            }
        }
        panel.revalidate();
        panel.repaint();
    }
}