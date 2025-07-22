package data_import;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.table.DefaultTableModel;

public class DuplicateManager {
    private final ImportDataTab parent;
    private final javax.swing.JLabel statusLabel;
    private boolean showDuplicates = true;

    public DuplicateManager(ImportDataTab parent, javax.swing.JLabel statusLabel) {
        this.parent = parent;
        this.statusLabel = statusLabel;
    }

    public void toggleDuplicates() {
        showDuplicates = !showDuplicates;
        updateTableDisplay();
        statusLabel.setText("Duplicates " + (showDuplicates ? "shown" : "hidden") + ".");
    }

    public void removeDuplicates() {
        List<utils.DataEntry> nonDuplicates = new ArrayList<>();
        TableColorRenderer renderer = (TableColorRenderer) parent.getTable().getDefaultRenderer(Object.class);
        for (int i = 0; i < parent.getOriginalData().size(); i++) {
            if (!renderer.isExactDuplicate(i)) {
                nonDuplicates.add(parent.getOriginalData().get(i));
            } else {
                java.util.logging.Logger.getLogger(DuplicateManager.class.getName()).log(
                    Level.INFO, "Removed exact duplicate: {0}",
                    new Object[]{parent.getOriginalData().get(i).getData().get("AssetName")});
            }
        }
        parent.getOriginalData().clear();
        parent.getOriginalData().addAll(nonDuplicates);
        parent.getRowStatus().clear();
        for (int i = 0; i < parent.getOriginalData().size(); i++) {
            String status = parent.dataDisplayManager.computeRowStatus(i, parent.getOriginalData().get(i));
            parent.getRowStatus().put(i, status);
        }
        updateTableDisplay();
        statusLabel.setText("Exact duplicates removed from import list.");
    }

    private void updateTableDisplay() {
        DefaultTableModel tableModel = parent.getTableModel();
        tableModel.setRowCount(0);
        TableColorRenderer renderer = (TableColorRenderer) parent.getTable().getDefaultRenderer(Object.class);
        for (int i = 0; i < parent.getOriginalData().size(); i++) {
            utils.DataEntry entry = parent.getOriginalData().get(i);
            if (showDuplicates || !renderer.isExactDuplicate(i)) {
                tableModel.addRow(entry.getValues());
            }
        }
        parent.getTable().repaint();
    }

    public boolean isShowDuplicates() {
        return showDuplicates;
    }
}