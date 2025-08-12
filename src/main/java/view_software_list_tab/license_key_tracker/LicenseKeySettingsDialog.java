package view_software_list_tab.license_key_tracker;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class LicenseKeySettingsDialog extends JDialog {
    private final Map<String, Integer> keyUsageLimits;
    private final JTextField keyField;
    private final JTextField limitField;

    public LicenseKeySettingsDialog(JDialog parent, Map<String, Integer> keyUsageLimits) {
        super(parent, "License Key Rules", true);
        this.keyUsageLimits = keyUsageLimits;
        this.keyField = new JTextField(20);
        this.limitField = new JTextField(5);
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
        inputPanel.add(new JLabel("Usage Limit:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(limitField, gbc);

        add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveLimit());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton, BorderLayout.WEST);
        buttonPanel.add(closeButton, BorderLayout.EAST);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void saveLimit() {
        String key = keyField.getText().trim();
        String limitText = limitField.getText().trim();
        if (key.isEmpty() || limitText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both a license key and a usage limit", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            int limit = Integer.parseInt(limitText);
            if (limit < 1) {
                JOptionPane.showMessageDialog(this, "Usage limit must be a positive integer", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            keyUsageLimits.put(key, limit);
            JOptionPane.showMessageDialog(this, "Usage limit saved for key: " + key, "Success", JOptionPane.INFORMATION_MESSAGE);
            keyField.setText("");
            limitField.setText("");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Usage limit must be a valid number", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showDialog() {
        setVisible(true);
    }
}