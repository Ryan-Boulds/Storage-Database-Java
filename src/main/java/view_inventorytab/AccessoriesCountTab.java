package view_inventorytab;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import utils.FileUtils;
import utils.PeripheralUtils;
import utils.UIComponentUtils;

public final class AccessoriesCountTab extends JPanel {
    private final DefaultTableModel cableTableModel;
    private final DefaultTableModel accessoryTableModel;
    private final DefaultTableModel deviceTableModel;
    private final JPanel panel;

    public AccessoriesCountTab() {
        setLayout(new BorderLayout(10, 10));

        cableTableModel = new DefaultTableModel(new String[]{"Cable Type", "Count"}, 0);
        accessoryTableModel = new DefaultTableModel(new String[]{"Accessory Type", "Count"}, 0);
        deviceTableModel = new DefaultTableModel(new String[]{"Device Type", "Count"}, 0);

        JTable cableTable = new JTable(cableTableModel);
        JTable accessoryTable = new JTable(accessoryTableModel);
        JTable deviceTable = new JTable(deviceTableModel);

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton refreshButton = UIComponentUtils.createFormattedButton("Refresh");
        refreshButton.addActionListener(e -> {
            try {
                FileUtils.loadDevices();
                FileUtils.loadCables();
                FileUtils.loadAccessories();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error loading data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            updateDisplay();
        });

        JPanel tablesPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        tablesPanel.add(UIComponentUtils.createScrollableContentPanel(cableTable));
        tablesPanel.add(UIComponentUtils.createScrollableContentPanel(accessoryTable));
        tablesPanel.add(UIComponentUtils.createScrollableContentPanel(deviceTable));

        add(tablesPanel, BorderLayout.CENTER);
        add(refreshButton, BorderLayout.SOUTH);

        updateDisplay();
    }

    private void updateDisplay() {
        panel.removeAll();
        panel.add(UIComponentUtils.createAlignedLabel("Accessories:"));
        try {
            ArrayList<HashMap<String, String>> accessories = FileUtils.loadAccessories();
            for (String type : PeripheralUtils.getPeripheralTypes(accessories)) {
                int count = PeripheralUtils.getPeripheralCount(type, accessories);
                if (count > 0) {
                    panel.add(UIComponentUtils.createAlignedLabel(type + ": " + count));
                }
            }
            panel.add(UIComponentUtils.createAlignedLabel("Cables:"));
            ArrayList<HashMap<String, String>> cables = FileUtils.loadCables();
            for (HashMap<String, String> cable : cables) {
                String type = cable.getOrDefault("Peripheral_Type", "").toLowerCase();
                String countStr = cable.getOrDefault("Count", "0");
                if (!type.isEmpty() && !countStr.equals("0")) {
                    int count = Integer.parseInt(countStr);
                    panel.add(UIComponentUtils.createAlignedLabel(type + ": " + count));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating display: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        panel.revalidate();
        panel.repaint();
    }
}