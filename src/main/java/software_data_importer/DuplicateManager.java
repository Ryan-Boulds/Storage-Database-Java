package software_data_importer;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.table.DefaultTableModel;

public class DuplicateManager {
    private final ImportDataTab parent;
    private final javax.swing.JLabel statusLabel;
    private boolean showDuplicates = true;
    private static final Logger LOGGER = Logger.getLogger(DuplicateManager.class.getName());

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
        int duplicateCount = 0;
        int totalRows = parent.getOriginalData().size();

        for (int i = 0; i < parent.getOriginalData().size(); i++) {
            if (!renderer.isExactDuplicate(i)) {
                nonDuplicates.add(parent.getOriginalData().get(i));
            } else {
                duplicateCount++;
                LOGGER.log(Level.INFO, "Removed exact duplicate at row {0}: {1}", 
                           new Object[]{i, parent.getOriginalData().get(i).getData().get("AssetName")});
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
        LOGGER.log(Level.INFO, "Removed {0} duplicates from {1} total rows, retained {2} unique entries.", 
                   new Object[]{duplicateCount, totalRows, nonDuplicates.size()});
    }

    private void updateTableDisplay() {
        DefaultTableModel tableModel = parent.getTableModel();
        tableModel.setRowCount(0);
        TableColorRenderer renderer = (TableColorRenderer) parent.getTable().getDefaultRenderer(Object.class);
        renderer.setRowStatus(parent.getRowStatus());
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