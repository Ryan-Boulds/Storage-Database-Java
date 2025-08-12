package view_software_list_tab.license_key_tracker;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import utils.DatabaseUtils;
import view_software_list_tab.TableManager;

public class LicenseKeySettingsDialog extends JDialog {
    private final TableManager tableManager;
    private final JSpinner limitSpinner;
    public LicenseKeySettingsDialog(JDialog parent, TableManager tableManager, int usageLimit) {
        super(parent, "License Key Rules", true);
        this.tableManager = tableManager;
        this.limitSpinner = new JSpinner(new SpinnerNumberModel(usageLimit, 1, Integer.MAX_VALUE, 1));
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
            // Check if a record exists for the table
            String checkSql = "SELECT COUNT(*) FROM LicenseKeyRules WHERE TableName = '" + tableName.replace("'", "''") + "'";
            ResultSet rs = stmt.executeQuery(checkSql);
            rs.next();
            int count = rs.getInt(1);

            String sql;
            if (count > 0) {
                // Update existing record
                sql = "UPDATE LicenseKeyRules SET UsageLimit = " + limit +
                      " WHERE TableName = '" + tableName.replace("'", "''") + "'";
            } else {
                // Insert new record
                sql = "INSERT INTO LicenseKeyRules (TableName, UsageLimit) VALUES ('" +
                      tableName.replace("'", "''") + "', " + limit + ")";
            }

            stmt.executeUpdate(sql);
            JOptionPane.showMessageDialog(this, "Usage limit saved: " + limit, "Success", JOptionPane.INFORMATION_MESSAGE);
            limitSpinner.setValue(limit);
            dispose();
        } catch (SQLException e) {
            System.err.println("LicenseKeySettingsDialog: Error saving to LicenseKeyRules for table '" + tableName + "': " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error saving usage limit: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showDialog() {
        setVisible(true);
    }
}