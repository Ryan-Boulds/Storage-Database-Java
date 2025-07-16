package view_inventorytab;

import java.awt.BorderLayout;
import java.awt.Component;
import java.sql.SQLException;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import utils.DatabaseUtils;

public class PopupHandler {
    public static void showDetailsPopup(JFrame parent, HashMap<String, String> device) {
        JDialog dialog = new JDialog(parent, "Device Details", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 300);

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));

        for (String key : device.keySet()) {
            String value = device.get(key);
            if (value != null && !value.isEmpty()) {
                JLabel label = new JLabel(key + ": " + value);
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                detailsPanel.add(label);
            }
        }

        JButton deleteButton = new JButton("Delete Device");
        deleteButton.addActionListener(e -> {
            String serialNumber = device.get("Serial_Number");
            if (serialNumber != null) {
                int confirm = JOptionPane.showConfirmDialog(
                    dialog,
                    "Are you sure you want to delete device with Serial Number: " + serialNumber + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        DatabaseUtils.deleteDevice(serialNumber);
                        JOptionPane.showMessageDialog(dialog, "Device deleted successfully");
                        dialog.dispose();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(dialog, "Error deleting device: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(detailsPanel);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(deleteButton, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public static void addTablePopup(JTable table, JTabbedPane tabbedPane) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem detailsItem = new JMenuItem("View Details");
        JMenuItem modifyItem = new JMenuItem("Modify Device");

        detailsItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                HashMap<String, String> device = new HashMap<>();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    String columnName = table.getColumnName(i).replace(" ", "_");
                    device.put(columnName, table.getValueAt(selectedRow, i).toString());
                }
                showDetailsPopup((JFrame) SwingUtilities.getWindowAncestor(table), device);
            }
        });

        modifyItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                HashMap<String, String> device = new HashMap<>();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    String columnName = table.getColumnName(i).replace(" ", "_");
                    device.put(columnName, table.getValueAt(selectedRow, i).toString());
                }
                ModifyDialog.showModifyDialog((JFrame) SwingUtilities.getWindowAncestor(table), device);
            }
        });

        popupMenu.add(detailsItem);
        popupMenu.add(modifyItem);
        table.setComponentPopupMenu(popupMenu);
    }
}