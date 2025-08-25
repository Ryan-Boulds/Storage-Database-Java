package view_inventory_tab.license_key_tracker;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import utils.DatabaseUtils;
import view_inventory_tab.TableManager;

public class LicenseKeySettingsDialog extends JDialog {
    private final TableManager tableManager;
    private final JSpinner limitSpinner;
    private static final Logger LOGGER = Logger.getLogger(LicenseKeySettingsDialog.class.getName());

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
        inputPanel.add(new JLabel("Number of allowed uses for " + tableManager.getTableName() + ":"), gbc);
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
                LOGGER.log(Level.WARNING, "Invalid usage limit: {0}", limit);
                JOptionPane.showMessageDialog(this, "Number of allowed uses must be a positive integer", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (ParseException e) {
            LOGGER.log(Level.WARNING, "ParseException in limitSpinner: {0}", e.getMessage());
            JOptionPane.showMessageDialog(this, "Number of allowed uses must be a valid number", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (HeadlessException e) {
            LOGGER.log(Level.WARNING, "HeadlessException in limitSpinner: {0}", e.getMessage());
            JOptionPane.showMessageDialog(this, "Error accessing UI components", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String tableName = tableManager.getTableName();
        try (Connection conn = DatabaseUtils.getConnection()) {
            if (conn == null) {
                LOGGER.log(Level.SEVERE, "Failed to establish database connection");
                JOptionPane.showMessageDialog(this, "Failed to connect to the database", "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String checkSql = "SELECT COUNT(*) FROM [LicenseKeyRules] WHERE [TableName] = ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, tableName);
                try (ResultSet rs = checkPs.executeQuery()) {
                    rs.next();
                    int count = rs.getInt(1);

                    if (count > 0) {
                        String updateSql = "UPDATE [LicenseKeyRules] SET [UsageLimit] = ? WHERE [TableName] = ?";
                        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                            ps.setInt(1, limit);
                            ps.setString(2, tableName);
                            ps.executeUpdate();
                            LOGGER.log(Level.INFO, "Updated UsageLimit to {0} for TableName='{1}'", new Object[]{limit, tableName});
                        }
                    } else {
                        String insertSql = "INSERT INTO [LicenseKeyRules] ([TableName], [UsageLimit]) VALUES (?, ?)";
                        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                            ps.setString(1, tableName);
                            ps.setInt(2, limit);
                            ps.executeUpdate();
                            LOGGER.log(Level.INFO, "Inserted UsageLimit {0} for TableName='{1}'", new Object[]{limit, tableName});
                        }
                    }
                }
            }
            JOptionPane.showMessageDialog(this, String.format("Usage limit saved: %d", limit), "Success", JOptionPane.INFORMATION_MESSAGE);
            limitSpinner.setValue(limit);
            dispose();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving to LicenseKeyRules for table '{0}': {1}", new Object[]{tableName, e.getMessage()});
            JOptionPane.showMessageDialog(this, String.format("Error saving usage limit: %s", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showDialog() {
        setVisible(true);
    }
}