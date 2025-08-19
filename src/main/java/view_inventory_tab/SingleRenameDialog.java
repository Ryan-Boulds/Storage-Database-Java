package view_inventory_tab;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(SingleRenameDialog.class.getName());

    public SingleRenameDialog(Frame parent, String originalValue, String columnName, String originalAssetName, JTable deviceTable, TableManager tableManager) {
        super(parent, "Modify " + columnName, true);
        this.originalValue = originalValue;
        this.columnName = columnName;
        this.originalAssetName = originalAssetName;
        this.tableManager = tableManager;
        LOGGER.log(Level.INFO, "SingleRenameDialog: originalAssetName=''{0}'', columnName=''{1}'', originalValue=''{2}''", new Object[]{originalAssetName, columnName, originalValue});
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
            LOGGER.severe("SingleRenameDialog: Attempted to edit in Inventory table, which is not allowed");
            return;
        }

        String newValue = valueField.getText().trim();
        if (newValue.isEmpty() && columnName.equals("AssetName")) {
            statusLabel.setText("Error: Asset Name cannot be empty");
            LOGGER.severe("SingleRenameDialog: Asset Name cannot be empty");
            return;
        }

        try {
            HashMap<String, String> device = DatabaseUtils.getDeviceByAssetName(tableName, originalAssetName);
            if (device == null) {
                LOGGER.log(Level.SEVERE, "SingleRenameDialog: Device not found for AssetName=''{0}'' in table ''{1}''", new Object[]{originalAssetName, tableName});
                statusLabel.setText("Error: Device not found for AssetName: " + originalAssetName);
                return;
            }
            LOGGER.log(Level.INFO, "SingleRenameDialog: Device found, updating {0} from ''{1}'' to ''{2}'' in table ''{3}''", new Object[]{columnName, originalValue, newValue, tableName});

            device.put(columnName, newValue);
            if (columnName.equals("AssetName")) {
                String validationError = DataUtils.validateDevice(device, originalAssetName);
                if (validationError != null) {
                    LOGGER.log(Level.SEVERE, "SingleRenameDialog: Validation error: {0}", validationError);
                    statusLabel.setText("Error: " + validationError);
                    return;
                }
            }

            DatabaseUtils.updateDevice(tableName, device);
            statusLabel.setText(columnName + " updated successfully");
            if (tableManager != null) {
                tableManager.refreshDataAndTabs();
            }
            dispose();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SingleRenameDialog: SQLException in table ''{0}'': {1}", new Object[]{tableName, e.getMessage()});
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    public static void showRenameDialog(Frame parent, JTable table, String cellValue, String columnName, String assetName, TableManager tableManager) {
        SingleRenameDialog dialog = new SingleRenameDialog(parent, cellValue, columnName, assetName, table, tableManager);
        dialog.setVisible(true);
    }
}