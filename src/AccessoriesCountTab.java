import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;

public class AccessoriesCountTab extends JPanel {
    private JPanel panel;

    public AccessoriesCountTab() {
        setLayout(new BorderLayout(10, 10));
        refreshData();

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Accessories Section
        panel.add(UIUtils.createAlignedLabel("Accessories:"));
        ArrayList<HashMap<String, String>> accessories = UIUtils.getAccessories();
        for (String type : UIUtils.getPeripheralTypes(accessories)) {
            int count = UIUtils.getPeripheralCount(type, accessories);
            if (count > 0) {
                panel.add(UIUtils.createAlignedLabel(type + ": " + count));
            }
        }

        // Cables Section
        panel.add(UIUtils.createAlignedLabel("Cables:"));
        ArrayList<HashMap<String, String>> cables = UIUtils.getCables();
        for (HashMap<String, String> cable : cables) {
            String type = cable.getOrDefault("Peripheral_Type", "").toLowerCase();
            String countStr = cable.getOrDefault("Count", "0");
            if (!type.isEmpty() && !countStr.equals("0")) {
                int count = Integer.parseInt(countStr);
                panel.add(UIUtils.createAlignedLabel(type + ": " + count));
            }
        }

        JButton refreshButton = UIUtils.createFormattedButton("Refresh");
        refreshButton.addActionListener(e -> {
            refreshData();
            updateDisplay();
        });

        JScrollPane scrollPane = UIUtils.createScrollableContentPanel(panel);
        add(scrollPane, BorderLayout.CENTER);
        add(refreshButton, BorderLayout.SOUTH);
    }

    public void refreshData() {
        UIUtils.loadDevices();
        UIUtils.loadCables();
        UIUtils.loadAccessories();
    }

    private void updateDisplay() {
        panel.removeAll();
        panel.add(UIUtils.createAlignedLabel("Accessories:"));
        ArrayList<HashMap<String, String>> accessories = UIUtils.getAccessories();
        for (String type : UIUtils.getPeripheralTypes(accessories)) {
            int count = UIUtils.getPeripheralCount(type, accessories);
            if (count > 0) {
                panel.add(UIUtils.createAlignedLabel(type + ": " + count));
            }
        }
        panel.add(UIUtils.createAlignedLabel("Cables:"));
        ArrayList<HashMap<String, String>> cables = UIUtils.getCables();
        for (HashMap<String, String> cable : cables) {
            String type = cable.getOrDefault("Peripheral_Type", "").toLowerCase();
            String countStr = cable.getOrDefault("Count", "0");
            if (!type.isEmpty() && !countStr.equals("0")) {
                int count = Integer.parseInt(countStr);
                panel.add(UIUtils.createAlignedLabel(type + ": " + count));
            }
        }
        panel.revalidate();
        panel.repaint();
    }
}