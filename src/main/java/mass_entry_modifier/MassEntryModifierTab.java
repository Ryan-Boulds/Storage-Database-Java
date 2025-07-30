package mass_entry_modifier;

import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import utils.DatabaseUtils;
import utils.UIComponentUtils;

public class MassEntryModifierTab extends JPanel {
    private final JLabel statusLabel;
    private JComboBox<String> matchColumnCombo;
    private UniqueValueComboBox matchValueCombo;
    private JComboBox<String> setColumnCombo;
    private JTextField setValueField;
    private JCheckBox overwriteCheckBox;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public MassEntryModifierTab(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        initComponents();
    }

    private void initComponents() {
        // Match criteria panel
        JPanel matchPanel = new JPanel();
        matchPanel.setLayout(new BoxLayout(matchPanel, BoxLayout.Y_AXIS));
        matchPanel.setBorder(BorderFactory.createTitledBorder("Match Criteria"));

        String[] columns = getInventoryColumns();
        matchColumnCombo = UIComponentUtils.createFormattedComboBox(columns);
        matchValueCombo = new UniqueValueComboBox(matchColumnCombo);

        matchPanel.add(UIComponentUtils.createAlignedLabel("Match Column:"));
        matchPanel.add(matchColumnCombo);
        matchPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        matchPanel.add(UIComponentUtils.createAlignedLabel("Match Value:"));
        matchPanel.add(matchValueCombo);
        matchPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // Set value panel
        JPanel setPanel = new JPanel();
        setPanel.setLayout(new BoxLayout(setPanel, BoxLayout.Y_AXIS));
        setPanel.setBorder(BorderFactory.createTitledBorder("Set Value"));

        setColumnCombo = UIComponentUtils.createFormattedComboBox(columns);
        setValueField = UIComponentUtils.createFormattedTextField();
        overwriteCheckBox = UIComponentUtils.createFormattedCheckBox("Overwrite existing values");

        setPanel.add(UIComponentUtils.createAlignedLabel("Set Column:"));
        setPanel.add(setColumnCombo);
        setPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        setPanel.add(UIComponentUtils.createAlignedLabel("Set Value:"));
        setPanel.add(setValueField);
        setPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        setPanel.add(overwriteCheckBox);

        // Action button
        JButton applyButton = UIComponentUtils.createFormattedButton("Apply Changes");
        applyButton.addActionListener(e -> applyMassUpdate());

        // Add components to main panel
        add(matchPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(setPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(applyButton);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(statusLabel);
    }

    private String[] getInventoryColumns() {
        try {
            ArrayList<String> columns = DatabaseUtils.getInventoryColumnNames("Inventory");
            return columns.toArray(new String[0]);
        } catch (SQLException e) {
            statusLabel.setText("Error loading columns: " + e.getMessage());
            return new String[0];
        }
    }

    private void refreshComboBoxes() {
        String prevMatchColumn = (String) matchColumnCombo.getSelectedItem();
        String prevSetColumn = (String) setColumnCombo.getSelectedItem();
        String prevMatchValue = (String) matchValueCombo.getSelectedItem();

        // Refresh columns
        String[] columns = getInventoryColumns();
        matchColumnCombo.setModel(new javax.swing.DefaultComboBoxModel<>(columns));
        setColumnCombo.setModel(new javax.swing.DefaultComboBoxModel<>(columns));

        // Restore previous selections if they still exist
        if (prevMatchColumn != null) {
            for (String column : columns) {
                if (column.equals(prevMatchColumn)) {
                    matchColumnCombo.setSelectedItem(prevMatchColumn);
                    break;
                }
            }
        }
        if (prevSetColumn != null) {
            for (String column : columns) {
                if (column.equals(prevSetColumn)) {
                    setColumnCombo.setSelectedItem(prevSetColumn);
                    break;
                }
            }
        }

        // Refresh unique values
        matchValueCombo.updateValues();

        // Restore previous match value if it still exists
        if (prevMatchValue != null) {
            for (int i = 0; i < matchValueCombo.getItemCount(); i++) {
                if (matchValueCombo.getItemAt(i).equals(prevMatchValue)) {
                    matchValueCombo.setSelectedItem(prevMatchValue);
                    break;
                }
            }
        }
    }

    public void refresh() {
        refreshComboBoxes();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    private void applyMassUpdate() {
        String matchColumn = (String) matchColumnCombo.getSelectedItem();
        String matchValue = (String) matchValueCombo.getSelectedItem();
        String setColumn = (String) setColumnCombo.getSelectedItem();
        String setValue = setValueField.getText().trim();

        if (matchColumn == null || matchValue == null || setColumn == null || setValue.isEmpty()) {
            statusLabel.setText("Error: All fields must be filled");
            return;
        }

        try {
            // Check if any non-blank values would be overwritten
            boolean hasNonBlankValues = checkNonBlankValues(matchColumn, matchValue, setColumn);
            if (hasNonBlankValues && !overwriteCheckBox.isSelected()) {
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Some entries have non-blank values in " + setColumn + ". Proceed with overwriting?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    statusLabel.setText("Operation cancelled");
                    return;
                }
            }

            // Perform the mass update
            int updatedRows = updateMatchingEntries(matchColumn, matchValue, setColumn, setValue, overwriteCheckBox.isSelected());
            statusLabel.setText("Successfully updated " + updatedRows + " entries");

            // Refresh combo boxes after successful update
            refreshComboBoxes();

            // Notify listeners (e.g., ViewInventoryTab) of the update
            System.out.println("Firing inventoryUpdated event"); // Debug log
            pcs.firePropertyChange("inventoryUpdated", null, null);
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(MassEntryModifierTab.class.getName()).log(
                java.util.logging.Level.SEVERE,
                "Error updating " + matchColumn + " = " + matchValue + " to " + setColumn + " = " + setValue, e);
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private boolean checkNonBlankValues(String matchColumn, String matchValue, String setColumn) throws SQLException {
        String sql = "SELECT [" + setColumn + "] FROM Inventory WHERE [" + matchColumn + "] = ?";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, matchValue);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String value = rs.getString(setColumn);
                    // Skip the matchValue when checking for non-blank values if matchColumn equals setColumn
                    if (matchColumn.equals(setColumn) && matchValue.equals(value)) {
                        continue;
                    }
                    if (value != null && !value.trim().isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int updateMatchingEntries(String matchColumn, String matchValue, String setColumn, String setValue, boolean overwrite) throws SQLException {
        String sql;
        if (overwrite) {
            sql = "UPDATE Inventory SET [" + setColumn + "] = ? WHERE [" + matchColumn + "] = ?";
        } else {
            sql = "UPDATE Inventory SET [" + setColumn + "] = ? WHERE [" + matchColumn + "] = ? AND ([" + setColumn + "] IS NULL OR [" + setColumn + "] = '')";
        }

        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, setValue);
            stmt.setString(2, matchValue);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Updated " + rowsAffected + " rows for " + matchColumn + " = " + matchValue); // Debug log
            return rowsAffected;
        }
    }
}