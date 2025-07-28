package mass_entry_modifier;

import java.awt.Dimension;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComboBox;

import utils.DatabaseUtils;

public final class UniqueValueComboBox extends JComboBox<String> {
    private final JComboBox<String> columnCombo;

    public UniqueValueComboBox(JComboBox<String> columnCombo) {
        this.columnCombo = columnCombo;
        setPreferredSize(new Dimension(450, 30));
        setMaximumSize(new Dimension(450, 30));
        setAlignmentX(LEFT_ALIGNMENT);

        // Initialize with values for the default selected column
        updateValues();

        // Add listener to update values when the column selection changes
        columnCombo.addActionListener(e -> updateValues());
    }

    void updateValues() { // Changed from private to package-private
        String selectedColumn = (String) columnCombo.getSelectedItem();
        if (selectedColumn == null) {
            removeAllItems();
            return;
        }

        Set<String> uniqueValues = new HashSet<>();
        String sql = "SELECT DISTINCT [" + selectedColumn + "] FROM Inventory WHERE [" + selectedColumn + "] IS NOT NULL AND [" + selectedColumn + "] <> ''";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String value = rs.getString(selectedColumn);
                if (value != null && !value.trim().isEmpty()) {
                    uniqueValues.add(value);
                }
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(UniqueValueComboBox.class.getName()).log(
                java.util.logging.Level.SEVERE, "Error retrieving unique values for column " + selectedColumn, e);
            removeAllItems();
            return;
        }

        // Update combo box items
        removeAllItems();
        uniqueValues.stream()
                    .sorted()
                    .forEach(this::addItem);
    }
}