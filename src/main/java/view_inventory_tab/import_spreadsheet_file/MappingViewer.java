package view_inventory_tab.import_spreadsheet_file;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import utils.UIComponentUtils;

public class MappingViewer {

    private final ImportDataTab parent;
    private final javax.swing.JLabel statusLabel;

    public MappingViewer(ImportDataTab parent, javax.swing.JLabel statusLabel) {
        this.parent = parent;
        this.statusLabel = statusLabel;
    }

    public void showCurrentMappings() {
        // Placeholder: Replace with actual DatabaseHandler.getColumnMappings() when implemented
        Map<String, String> columnMappings = new HashMap<>();
        columnMappings.put("SampleSource", "AssetName"); // Example mapping
        if (columnMappings.isEmpty()) {
            statusLabel.setText("No mappings defined.");
            JOptionPane.showMessageDialog(parent, "No mappings defined.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel mappingPanel = new JPanel(new java.awt.GridLayout(0, 2, 5, 5));
        for (Map.Entry<String, String> entry : columnMappings.entrySet()) {
            mappingPanel.add(UIComponentUtils.createAlignedLabel("Source: " + entry.getKey()));
            mappingPanel.add(UIComponentUtils.createAlignedLabel("Maps to: " + entry.getValue()));
        }

        javax.swing.JScrollPane mappingScrollPane = UIComponentUtils.createScrollableContentPanel(mappingPanel);
        mappingScrollPane.setPreferredSize(new Dimension(400, 200));

        statusLabel.setText("Displaying column mappings.");
        JOptionPane.showMessageDialog(parent, mappingScrollPane, "Current Column Mappings", JOptionPane.INFORMATION_MESSAGE);
    }
}
