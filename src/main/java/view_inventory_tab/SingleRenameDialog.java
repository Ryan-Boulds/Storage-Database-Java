package view_inventory_tab;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.sql.SQLException;
import java.util.HashMap;

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

public class SingleRenameDialog extends JDialog {
    private JTextField valueField;
    private JLabel statusLabel;
    private JButton saveButton;
    private JButton cancelButton;
    private final String originalValue;
    private final String columnName;
    private final String originalAssetName;
    private final TableManager tableManager;

    public SingleRenameDialog(Frame parent, String originalValue, String columnName, String originalAssetName, JTable deviceTable, TableManager tableManager) {
        super(parent, "Modify " + columnName, true);
        this.originalValue = originalValue;
        this.columnName = columnName;
        this.originalAssetName = originalAssetName;
        this.tableManager = tableManager;
        System.out.println("SingleRenameDialog: originalAssetName='" + originalAssetName + "', columnName='" + columnName + "', originalValue='" + originalValue + "'");
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

        mainPanel.add(UIComponentUtils.createAlignedLabel("New " + columnName + ":"));
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
        valueField.setText(originalValue);
    }

    private void saveChanges() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            statusLabel.setText("Error: Editing is not allowed for the Inventory table");
            System.err.println("SingleRenameDialog: Attempted to edit in Inventory table, which is not allowed");
            return;
        }

        String newValue = valueField.getText().trim();
        if (newValue.isEmpty() && columnName.equals("AssetName")) {
            statusLabel.setText("Error: Asset Name cannot be empty");
            return;
        }

        try {
            HashMap<String, String> device = DatabaseUtils.getDeviceByAssetName(tableName, originalAssetName);
            if (device == null) {
                System.err.println("SingleRenameDialog: Device not found for AssetName='" + originalAssetName + "' in table '" + tableName + "'");
                statusLabel.setText("Error: Device not found for AssetName: " + originalAssetName);
                return;
            }
            System.out.println("SingleRenameDialog: Device found, updating " + columnName + " from '" + originalValue + "' to '" + newValue + "' in table '" + tableName + "'");

            device.put(columnName, newValue);
            String validationError = DataUtils.validateDevice(device, originalAssetName);
            if (validationError != null) {
                System.err.println("SingleRenameDialog: Validation error: " + validationError);
                statusLabel.setText("Error: " + validationError);
                return;
            }

            DatabaseUtils.updateDevice(tableName, device);
            statusLabel.setText(columnName + " updated successfully");
            if (tableManager != null) {
                tableManager.refreshDataAndTabs();
            }
            dispose();
        } catch (SQLException e) {
            System.err.println("SingleRenameDialog: SQLException in table '" + tableName + "': " + e.getMessage());
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    public static void showRenameDialog(Frame parent, JTable table, String cellValue, String columnName, String assetName, TableManager tableManager) {
        SingleRenameDialog dialog = new SingleRenameDialog(parent, cellValue, columnName, assetName, table, tableManager);
        dialog.setVisible(true);
    }
}