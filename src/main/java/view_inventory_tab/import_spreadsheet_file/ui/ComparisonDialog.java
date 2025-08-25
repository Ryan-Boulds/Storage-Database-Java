package view_inventory_tab.import_spreadsheet_file.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import utils.DataEntry;
import utils.UIComponentUtils;
import view_inventory_tab.import_spreadsheet_file.ImportDataTab;

public class ComparisonDialog {
    private final ImportDataTab parent;
    private final String[] tableColumns;
    private final HashMap<String, String> newDevice;
    private final HashMap<String, String> oldDevice;
    private final String assetName;
    private JDialog dialog;
    private HashMap<String, String> resolvedDevice;
    private String[] resolvedValues;
    private static final Logger LOGGER = Logger.getLogger(ComparisonDialog.class.getName());

    public ComparisonDialog(ImportDataTab parent, DataEntry entry, String[] tableColumns, HashMap<String, String> oldDevice) {
        this.parent = parent;
        this.tableColumns = tableColumns;
        this.newDevice = entry.getData();
        this.oldDevice = oldDevice;
        this.assetName = newDevice.get("AssetName");
    }

    public DataEntry showDialog() {
        try {
            if (oldDevice == null) {
                String errorMessage = "No existing device found for AssetName: " + assetName;
                LOGGER.warning(errorMessage);
                JOptionPane.showMessageDialog(parent, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            dialog = new JDialog(JOptionPane.getFrameForComponent(parent), "Compare and Resolve: " + assetName, true);
            dialog.setLayout(new BorderLayout());
            dialog.setSize(800, 400);
            dialog.setMinimumSize(new Dimension(400, 300));
            dialog.setResizable(true);

            JPanel panel = new JPanel(new GridLayout(0, 4, 5, 5));
            panel.add(new JLabel("Field"));
            panel.add(new JLabel("Old Value (Database)"));
            panel.add(new JLabel("New Value (Imported)"));
            panel.add(new JLabel("Keep"));

            Map<String, JRadioButton> oldButtons = new HashMap<>();
            Map<String, JRadioButton> newButtons = new HashMap<>();
            for (String field : tableColumns) {
                String dbField = field.replace(" ", "_");
                String oldValue = oldDevice.get(dbField);
                String newValue = newDevice.get(dbField);
                String oldVal = (oldValue == null || oldValue.trim().isEmpty()) ? "" : oldValue;
                String newVal = (newValue == null || newValue.trim().isEmpty()) ? "" : newValue;

                if (dbField.equals("AssetName") || !oldVal.equals(newVal)) {
                    panel.add(new JLabel(field));
                    panel.add(new JLabel(oldVal));
                    panel.add(new JLabel(newVal));
                    JPanel radioPanel = new JPanel(new GridLayout(1, 2));
                    JRadioButton oldRadio = new JRadioButton("Old");
                    JRadioButton newRadio = new JRadioButton("New");
                    if (dbField.equals("AssetName")) {
                        oldRadio.setEnabled(false);
                        newRadio.setEnabled(false);
                        newRadio.setSelected(true);
                    } else {
                        newRadio.setSelected(true);
                    }
                    ButtonGroup group = new ButtonGroup();
                    group.add(oldRadio);
                    group.add(newRadio);
                    radioPanel.add(oldRadio);
                    radioPanel.add(newRadio);
                    panel.add(radioPanel);

                    oldButtons.put(dbField, oldRadio);
                    newButtons.put(dbField, newRadio);
                }
            }

            JScrollPane scrollPane = UIComponentUtils.createScrollableContentPanel(panel);
            dialog.add(scrollPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            JButton okButton = UIComponentUtils.createFormattedButton("OK");
            JButton cancelButton = UIComponentUtils.createFormattedButton("Cancel");
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            okButton.addActionListener(e -> {
                resolvedDevice = new HashMap<>();
                resolvedValues = new String[tableColumns.length];
                for (int i = 0; i < tableColumns.length; i++) {
                    String dbField = tableColumns[i].replace(" ", "_");
                    String value;
                    if (newButtons.containsKey(dbField) && oldButtons.containsKey(dbField)) {
                        value = newButtons.get(dbField).isSelected() ? newDevice.get(dbField) : oldDevice.get(dbField);
                    } else {
                        value = newDevice.get(dbField) != null ? newDevice.get(dbField) : oldDevice.get(dbField);
                    }
                    value = (value == null || value.trim().isEmpty()) ? "" : value;
                    resolvedDevice.put(dbField, value);
                    resolvedValues[i] = value;
                }
                LOGGER.log(Level.INFO, "Resolved device for AssetName: {0}, Data: {1}", new Object[]{assetName, resolvedDevice});
                dialog.dispose();
            });

            cancelButton.addActionListener(e -> {
                resolvedDevice = null;
                resolvedValues = null;
                dialog.dispose();
            });

            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);

            if (resolvedDevice != null && resolvedValues != null) {
                return new DataEntry(resolvedValues, resolvedDevice);
            }
        } catch (Exception e) {
            String errorMessage = "Error comparing data for AssetName " + assetName + ": " + e.getMessage();
            LOGGER.log(Level.SEVERE, errorMessage, e);
            JOptionPane.showMessageDialog(parent, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }
}