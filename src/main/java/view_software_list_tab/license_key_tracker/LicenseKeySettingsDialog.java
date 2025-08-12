package view_software_list_tab.license_key_tracker;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import utils.DatabaseUtils;
import view_software_list_tab.TableManager;

public class LicenseKeySettingsDialog extends JDialog {
    private final TableManager tableManager;
    private final Map<String, Integer> keyUsageLimits;
    private final JTextField keyField;
    private final JSpinner limitSpinner;

    public LicenseKeySettingsDialog(JDialog parent, TableManager tableManager, Map<String, Integer> keyUsageLimits) {
        super(parent, "License Key Rules", true);
        this.tableManager = tableManager;
        this.keyUsageLimits = keyUsageLimits;
        this.keyField = new JTextField(20);
        this.limitSpinner = new JSpinner(new SpinnerNumberModel(10, 1, Integer.MAX_VALUE, 1));
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setSize(400, 200);
        setLocationRelativeTo(null);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("License Key:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(keyField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Number of allowed uses:"), gbc);
        gbc.gridx = 1;
        limitSpinner.setEditor(new JSpinner.NumberEditor(limitSpinner, "#"));
        inputPanel.add(limitSpinner, gbc);

        add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveLimit());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton, BorderLayout.WEST);
        buttonPanel.add(cancelButton, BorderLayout.EAST);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void saveLimit() {
        String key = keyField.getText().trim();
        if (key.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a license key", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int limit;
        try {
            limitSpinner.commitEdit();
            limit = (Integer) limitSpinner.getValue();
            if (limit < 1) {
                JOptionPane.showMessageDialog(this, "Number of allowed uses must be a positive integer", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (HeadlessException | ParseException e) {
            JOptionPane.showMessageDialog(this, "Number of allowed uses must be a valid number", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String tableName = tableManager.getTableName();
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement()) {
            // Insert or update the LicenseKeyRules table
            String sql = "INSERT INTO LicenseKeyRules (TableName, LicenseKey, UsageLimit) VALUES ('" +
                         tableName.replace("'", "''") + "', '" +
                         key.replace("'", "''") + "', " + limit + ")" +
                         " ON DUPLICATE KEY UPDATE UsageLimit = " + limit;
            try {
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                // Fallback for Access (UCanAccess doesn't support ON DUPLICATE KEY UPDATE)
                if (e.getMessage().contains("already exists")) {
                    sql = "UPDATE LicenseKeyRules SET UsageLimit = " + limit +
                          " WHERE TableName = '" + tableName.replace("'", "''") + "' AND LicenseKey = '" + key.replace("'", "''") + "'";
                    stmt.executeUpdate(sql);
                } else {
                    throw e;
                }
            }
            keyUsageLimits.put(key, limit);
            JOptionPane.showMessageDialog(this, "Usage limit saved for key: " + key, "Success", JOptionPane.INFORMATION_MESSAGE);
            keyField.setText("");
            limitSpinner.setValue(10);
            dispose();
        } catch (SQLException e) {
            System.err.println("LicenseKeySettingsDialog: Error saving to LicenseKeyRules for table '" + tableName + "', key '" + key + "': " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error saving usage limit: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showDialog() {
        setVisible(true);
    }
}