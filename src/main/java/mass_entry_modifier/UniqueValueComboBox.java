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
import utils.DefaultColumns;

public final class UniqueValueComboBox extends JComboBox<String> {
    private final JComboBox<String> columnCombo;

    public UniqueValueComboBox(JComboBox<String> columnCombo) {
        this.columnCombo = columnCombo;
        setPreferredSize(new Dimension(450, 30));
        setMaximumSize(new Dimension(450, 30));
        setAlignmentX(LEFT_ALIGNMENT);
        updateValues();
        columnCombo.addActionListener(e -> updateValues());
    }

    void updateValues() {
        String selectedColumn = (String) columnCombo.getSelectedItem();
        if (selectedColumn == null) {
            removeAllItems();
            return;
        }

        Set<String> uniqueValues = new HashSet<>();
        String sql;
        String columnType = DefaultColumns.getInventoryColumnDefinitions().getOrDefault(selectedColumn, "TEXT");

        if (null == columnType) {
            // For text columns, select directly
            sql = "SELECT DISTINCT [" + selectedColumn + "] AS Value FROM Inventory WHERE [" + selectedColumn + "] IS NOT NULL AND [" + selectedColumn + "] <> ''";
        } else // Adjust SQL based on column type
        switch (columnType) {
            case "DOUBLE":
                // For numeric columns like Memory, select raw values and convert in Java
                sql = "SELECT DISTINCT [" + selectedColumn + "] AS Value FROM Inventory WHERE [" + selectedColumn + "] IS NOT NULL";
                break;
            case "DATE":
                // For date columns, use Format to ensure consistent string output
                sql = "SELECT DISTINCT Format([" + selectedColumn + "], 'yyyy-mm-dd') AS Value FROM Inventory WHERE [" + selectedColumn + "] IS NOT NULL";
                break;
            default:
                // For text columns, select directly
                sql = "SELECT DISTINCT [" + selectedColumn + "] AS Value FROM Inventory WHERE [" + selectedColumn + "] IS NOT NULL AND [" + selectedColumn + "] <> ''";
                break;
        }

        System.out.println("Executing SQL for " + selectedColumn + ": " + sql);
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String value;
                if ("DOUBLE".equals(columnType)) {
                    // Convert numeric values to strings in Java
                    double doubleValue = rs.getDouble("Value");
                    value = rs.wasNull() ? null : String.format("%.0f", doubleValue);
                } else {
                    value = rs.getString("Value");
                }
                if (value != null && !value.trim().isEmpty()) {
                    uniqueValues.add(value);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving unique values for column " + selectedColumn + ": " + e.getMessage());
            e.printStackTrace();
            removeAllItems();
            return;
        }

        removeAllItems();
        uniqueValues.stream().sorted().forEach(this::addItem);
        System.out.println("Populated " + uniqueValues.size() + " unique values for " + selectedColumn);
    }
}