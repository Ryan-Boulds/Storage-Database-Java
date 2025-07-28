package view_inventorytab;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import utils.DataUtils;
import utils.DatabaseUtils;
import utils.UIComponentUtils;

public class MultiRenameDialog extends JDialog {
    private JTextField valueField;
    private JLabel statusLabel;
    private JButton saveButton;
    private JButton cancelButton;
    private final ArrayList<String> originalCellValues;
    private final String columnName;
    private final ArrayList<String> originalAssetNames;
    private final TableManager tableManager;

    public MultiRenameDialog(Frame parent, ArrayList<String> originalCellValues, String columnName, ArrayList<String> originalAssetNames, JTable deviceTable, TableManager tableManager) {
        super(parent, "Multi-Modify " + columnName, true);
        this.originalCellValues = originalCellValues;
        this.columnName = columnName;
        this.originalAssetNames = originalAssetNames;
        this.tableManager = tableManager;
        System.out.println("MultiRenameDialog: columnName='" + columnName + "', assetNames=" + originalAssetNames + ", cellValues=" + originalCellValues); // Debug
        initializeComponents();
        populateFields();
        setLocationRelativeTo(parent);
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(500, 200);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainPanel.add(UIComponentUtils.createAlignedLabel("Enter new value for " + columnName + ":"));
        valueField = UIComponentUtils.createFormattedTextField();
        mainPanel.add(valueField);

        statusLabel = UIComponentUtils.createAlignedLabel("");
        mainPanel.add(statusLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = UIComponentUtils.createFormattedButton("Save");
        cancelButton = UIComponentUtils.createFormattedButton("Cancel");

        saveButton.addActionListener(e -> saveChanges());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void populateFields() {
        // If all selected cells have the same value, pre-populate the field
        HashSet<String> uniqueValues = new HashSet<>(originalCellValues);
        if (uniqueValues.size() == 1) {
            String commonValue = uniqueValues.iterator().next();
            valueField.setText(commonValue);
            System.out.println("MultiRenameDialog: Pre-populated with common value: '" + commonValue + "'"); // Debug
        }
    }

    private void saveChanges() {
        String newValue = valueField.getText().trim();
        if (newValue.isEmpty() && columnName.equals("AssetName")) {
            statusLabel.setText("Error: Asset Name cannot be empty");
            return;
        }

        try {
            for (int i = 0; i < originalAssetNames.size(); i++) {
                String originalAssetName = originalAssetNames.get(i);
                HashMap<String, String> device = DatabaseUtils.getDeviceByAssetName(originalAssetName);
                if (device == null) {
                    System.err.println("MultiRenameDialog: Device not found for AssetName='" + originalAssetName + "'"); // Debug
                    statusLabel.setText("Error: Device not found for AssetName: " + originalAssetName);
                    return;
                }
                System.out.println("MultiRenameDialog: Device found, updating " + columnName + " from '" + originalCellValues.get(i) + "' to '" + newValue + "' for AssetName='" + originalAssetName + "'"); // Debug

                device.put(columnName, newValue);
                // Validate AssetName uniqueness only for AssetName column
                String validationError = columnName.equals("AssetName") ? DataUtils.validateDevice(device, originalAssetName) : null;
                if (validationError != null) {
                    System.err.println("MultiRenameDialog: Validation error for '" + originalAssetName + "': " + validationError); // Debug
                    statusLabel.setText("Error for " + originalAssetName + ": " + validationError);
                    return;
                }

                DatabaseUtils.updateDevice(device);
            }
            statusLabel.setText(columnName + " values updated successfully");
            if (tableManager != null) {
                tableManager.refreshDataAndTabs();
            }
            dispose();
        } catch (SQLException e) {
            System.err.println("MultiRenameDialog: SQLException: " + e.getMessage()); // Debug
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    public static void showRenameDialog(Frame parent, JTable table, ArrayList<String> cellValues, String columnName, ArrayList<String> assetNames, TableManager tableManager) {
        MultiRenameDialog dialog = new MultiRenameDialog(parent, cellValues, columnName, assetNames, table, tableManager);
        dialog.setVisible(true);
    }
}