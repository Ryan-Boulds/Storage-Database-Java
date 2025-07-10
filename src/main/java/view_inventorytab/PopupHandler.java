package view_inventorytab;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.function.BiConsumer;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import utils.FileUtils;
import utils.InventoryData;

public class PopupHandler {
    private final JPopupMenu popupMenu;

    public PopupHandler(BiConsumer<HashMap<String, String>, String> modifyAction) {
        popupMenu = new JPopupMenu();
        JMenuItem modifyItem = new JMenuItem("Change/Modify");
        JMenuItem removeItem = new JMenuItem("Remove Entry");
        popupMenu.add(modifyItem);
        popupMenu.add(removeItem);

        modifyItem.addActionListener(e -> {
            JTable table = (JTable) ((JPopupMenu) e.getSource()).getInvoker();
            int row = table.getSelectedRow();
            if (row >= 0) {
                JTabbedPane tabbedPane = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, table);
                String type = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
                HashMap<String, String> device = new HashMap<>();
                for (int col = 0; col < table.getColumnCount(); col++) {
                    device.put(table.getColumnName(col).replace(" ", "_"), (String) table.getValueAt(row, col));
                }
                modifyAction.accept(device, type);
            }
        });

        removeItem.addActionListener(e -> {
            JTable table = (JTable) ((JPopupMenu) e.getSource()).getInvoker();
            int row = table.getSelectedRow();
            if (row >= 0) {
                String serialNumber = (String) table.getValueAt(row, table.getColumnModel().getColumnIndex("Serial Number"));
                String deviceName = (String) table.getValueAt(row, table.getColumnModel().getColumnIndex("Device Name"));
                int confirm = JOptionPane.showConfirmDialog(
                    table,
                    "Are you sure you want to remove device '" + deviceName + "' (Serial: " + serialNumber + ")?",
                    "Confirm Removal",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    boolean removed = false;
                    for (int i = 0; i < InventoryData.getDevices().size(); i++) {
                        HashMap<String, String> device = InventoryData.getDevices().get(i);
                        if (device.get("Serial_Number").equals(serialNumber)) {
                            InventoryData.getDevices().remove(i);
                            removed = true;
                            break;
                        }
                    }
                    if (removed) {
                        FileUtils.saveDevices();
                        JOptionPane.showMessageDialog(table, "Device removed successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(table, "Error: Device not found", "Removal Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }

    public void addTablePopup(JTable table, JTabbedPane tabbedPane) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        popupMenu.show(table, e.getX(), e.getY());
                    }
                }
            }
        });
    }
}