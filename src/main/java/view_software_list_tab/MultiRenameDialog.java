package view_software_list_tab;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

public class MultiRenameDialog extends JDialog {
    private JTextField valueField;
    private JLabel statusLabel;
    private JButton saveButton;
    private JButton cancelButton;
    private final ArrayList<String> originalCellValues;
    private final String columnName;
    private final ArrayList<String> originalAssetNames;
    private final TableManager tableManager;
    private static final Logger LOGGER = Logger.getLogger(MultiRenameDialog.class.getName());

    public MultiRenameDialog(Frame parent, ArrayList<String> originalCellValues, String columnName, ArrayList<String> originalAssetNames, JTable deviceTable, TableManager tableManager) {
        super(parent, "Multi-Modify " + columnName, true);
        this.originalCellValues = originalCellValues;
        this.columnName = columnName;
        this.originalAssetNames = originalAssetNames;
        this.tableManager = tableManager;
        LOGGER.log(Level.INFO, "MultiRenameDialog: columnName=''{0}'', assetNames={1}, cellValues={2}", new Object[]{columnName, originalAssetNames, originalCellValues});
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
        HashSet<String> uniqueValues = new HashSet<>(originalCellValues);
        if (uniqueValues.size() == 1) {
            String commonValue = uniqueValues.iterator().next();
            valueField.setText(commonValue);
            LOGGER.log(Level.INFO, "MultiRenameDialog: Pre-populated with common value: ''{0}''", commonValue);
        }
    }

    private void saveChanges() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            statusLabel.setText("Error: Editing is not allowed for the Inventory table");
            LOGGER.severe("MultiRenameDialog: Attempted to edit in Inventory table, which is not allowed");
            return;
        }

        String newValue = valueField.getText().trim();
        if (newValue.isEmpty() && columnName.equals("AssetName")) {
            statusLabel.setText("Error: Asset Name cannot be empty");
            LOGGER.severe("MultiRenameDialog: Asset Name cannot be empty");
            return;
        }

        try {
            for (int i = 0; i < originalAssetNames.size(); i++) {
                String originalAssetName = originalAssetNames.get(i);
                HashMap<String, String> device = DatabaseUtils.getDeviceByAssetName(tableName, originalAssetName);
                if (device == null) {
                    LOGGER.log(Level.SEVERE, "MultiRenameDialog: Device not found for AssetName=''{0}'' in table ''{1}''", new Object[]{originalAssetName, tableName});
                    statusLabel.setText("Error: Device not found for AssetName: " + originalAssetName);
                    return;
                }
                LOGGER.log(Level.INFO, "MultiRenameDialog: Device found, updating {0} from ''{1}'' to ''{2}'' for AssetName=''{3}'' in table ''{4}''", new Object[]{columnName, originalCellValues.get(i), newValue, originalAssetName, tableName});

                device.put(columnName, newValue);
                if (columnName.equals("AssetName")) {
                    String validationError = DataUtils.validateDevice(device, originalAssetName);
                    if (validationError != null) {
                        LOGGER.log(Level.SEVERE, "MultiRenameDialog: Validation error for ''{0}'': {1}", new Object[]{originalAssetName, validationError});
                        statusLabel.setText("Error for " + originalAssetName + ": " + validationError);
                        return;
                    }
                }

                DatabaseUtils.updateDevice(tableName, device);
            }
            statusLabel.setText(columnName + " values updated successfully");
            if (tableManager != null) {
                tableManager.refreshDataAndTabs();
            }
            dispose();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "MultiRenameDialog: SQLException in table ''{0}'': {1}", new Object[]{tableName, e.getMessage()});
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    public static void showRenameDialog(Frame parent, JTable table, ArrayList<String> cellValues, String columnName, ArrayList<String> assetNames, TableManager tableManager) {
        MultiRenameDialog dialog = new MultiRenameDialog(parent, cellValues, columnName, assetNames, table, tableManager);
        dialog.setVisible(true);
    }
}