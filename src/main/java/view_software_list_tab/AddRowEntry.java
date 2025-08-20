package view_software_list_tab;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import utils.DatabaseUtils;
import utils.UIComponentUtils;

public class AddRowEntry extends JDialog {
    private String[] columnNames;
    private final Map<String, Integer> columnTypes;
    private final List<String> pendingColumns; // Track new columns in memory
    private JComponent[] inputs;
    private final TableManager tableManager;
    private final JPanel inputPanel;
    private final JScrollPane scrollPane;
    private static final Logger LOGGER = Logger.getLogger(AddRowEntry.class.getName());

    public AddRowEntry(JFrame parent, TableManager tableManager) {
        super(parent, "Add Row Entry", true);
        this.tableManager = tableManager;
        this.columnNames = tableManager.getColumns();
        this.columnTypes = tableManager.getColumnTypes();
        this.pendingColumns = new ArrayList<>(); // Initialize pending columns list

        setLayout(new BorderLayout());
        setSize(600, 800);
        setLocationRelativeTo(parent);

        inputPanel = new JPanel(new GridBagLayout());
        scrollPane = new JScrollPane(inputPanel);
        add(scrollPane, BorderLayout.CENTER);

        refreshInputFields();

        JPanel buttonPanel = new JPanel();
        JButton saveButton = UIComponentUtils.createFormattedButton("Save");
        saveButton.addActionListener(e -> saveAction());
        buttonPanel.add(saveButton);

        JButton cancelButton = UIComponentUtils.createFormattedButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        JButton addColumnButton = UIComponentUtils.createFormattedButton("Add Column");
        addColumnButton.addActionListener(e -> addColumnAction());
        buttonPanel.add(addColumnButton);

        add(buttonPanel, BorderLayout.SOUTH);
        LOGGER.log(Level.INFO, "Initialized AddRowEntry dialog for table '{0}'", tableManager.getTableName());
    }

    private void refreshInputFields() {
        LOGGER.log(Level.INFO, "refreshInputFields: Starting refresh, current columns: {0}, pending columns: {1}", 
            new Object[]{String.join(", ", columnNames), String.join(", ", pendingColumns)});
        
        // Combine existing columns with pending columns
        List<String> allColumns = new ArrayList<>(Arrays.asList(columnNames));
        allColumns.addAll(pendingColumns);
        inputs = new JComponent[allColumns.size()];
        inputPanel.removeAll(); // Clear existing components

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        FontMetrics fm = inputPanel.getFontMetrics(inputPanel.getFont());
        int maxLabelWidth = 0;
        for (String columnName : allColumns) {
            maxLabelWidth = Math.max(maxLabelWidth, fm.stringWidth(columnName));
        }

        for (int i = 0; i < allColumns.size(); i++) {
            String columnName = allColumns.get(i);
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.anchor = GridBagConstraints.EAST;
            inputPanel.add(UIComponentUtils.createAlignedLabel(columnName), gbc);

            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            Integer sqlType = columnTypes.get(columnName);
            if (sqlType != null && sqlType == Types.BIT) {
                inputs[i] = new JCheckBox();
            } else {
                inputs[i] = UIComponentUtils.createFormattedTextField();
                ((JTextField) inputs[i]).setColumns(20);
            }
            inputPanel.add(inputs[i], gbc);
        }

        // Force UI update
        inputPanel.revalidate();
        inputPanel.repaint();
        scrollPane.revalidate();
        scrollPane.repaint();
        SwingUtilities.invokeLater(() -> {
            inputPanel.revalidate();
            inputPanel.repaint();
            scrollPane.revalidate();
            scrollPane.repaint();
            LOGGER.log(Level.INFO, "refreshInputFields: Completed UI refresh, all columns: {0}", String.join(", ", allColumns));
        });
    }

    private void saveAction() {
        String tableName = tableManager.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No table selected", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.WARNING, "Attempted to save row with no table selected");
            return;
        }

        // Combine existing and pending columns
        List<String> allColumns = new ArrayList<>(Arrays.asList(columnNames));
        allColumns.addAll(pendingColumns);
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < allColumns.size(); i++) {
            String columnName = allColumns.get(i);
            if (inputs[i] instanceof JTextField) {
                String text = ((JTextField) inputs[i]).getText().trim();
                values.put(columnName, text.isEmpty() ? null : text);
            } else if (inputs[i] instanceof JCheckBox) {
                values.put(columnName, ((JCheckBox) inputs[i]).isSelected() ? "true" : "false");
            }
        }

        String assetName = values.get("AssetName");
        if (assetName == null || assetName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Error: AssetName is required", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Attempted to save empty AssetName in table '{0}'", tableName);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            // Begin transaction
            conn.setAutoCommit(false);
            try {
                // Add pending columns to the database
                for (String newColumn : pendingColumns) {
                    String sql = "ALTER TABLE [" + tableName + "] ADD COLUMN [" + newColumn + "] VARCHAR(255)";
                    conn.createStatement().executeUpdate(sql);
                    LOGGER.log(Level.INFO, "saveAction: Added column '{0}' to table '{1}'", new Object[]{newColumn, tableName});
                }

                // Insert the row
                StringBuilder sql = new StringBuilder("INSERT INTO [" + tableName + "] (");
                StringBuilder placeholders = new StringBuilder();
                List<String> columns = new ArrayList<>();
                List<String> parameters = new ArrayList<>();
                for (String column : allColumns) {
                    columns.add("[" + column + "]");
                    placeholders.append("?");
                    parameters.add(values.get(column));
                    if (!column.equals(allColumns.get(allColumns.size() - 1))) {
                        placeholders.append(", ");
                    }
                }
                sql.append(String.join(", ", columns)).append(") VALUES (").append(placeholders).append(")");
                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    for (int i = 0; i < parameters.size(); i++) {
                        ps.setString(i + 1, parameters.get(i));
                    }
                    ps.executeUpdate();
                }

                // Commit transaction
                conn.commit();
                JOptionPane.showMessageDialog(this, "Row and columns added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                LOGGER.log(Level.INFO, "Saved new row to table '{0}' with {1} new columns", new Object[]{tableName, pendingColumns.size()});
                SwingUtilities.invokeLater(() -> {
                    tableManager.setTableName(tableName); // Force schema reload
                    tableManager.refreshDataAndTabs();
                    dispose();
                });
            } catch (SQLException e) {
                conn.rollback(); // Roll back on error
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding row or columns: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "SQLException adding row or columns to table '{0}': {1}", new Object[]{tableName, e.getMessage()});
        }
    }

    private void addColumnAction() {
        String tableName = tableManager.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a valid table first", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.WARNING, "Attempted to add column without selecting a valid table");
            return;
        }
        String inputColumnName = JOptionPane.showInputDialog(this, "Enter new column name:");
        if (inputColumnName != null && !inputColumnName.trim().isEmpty()) {
            final String newColumnName = inputColumnName.trim();
            // Check against both existing and pending columns
            List<String> allColumns = new ArrayList<>(Arrays.asList(columnNames));
            allColumns.addAll(pendingColumns);
            if (allColumns.contains(newColumnName)) {
                JOptionPane.showMessageDialog(this, "Error: Column '" + newColumnName + "' already exists", "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, "Attempted to add duplicate column '{0}' to table '{1}'", new Object[]{newColumnName, tableName});
                return;
            }
            // Add to pending columns
            pendingColumns.add(newColumnName);
            LOGGER.log(Level.INFO, "addColumnAction: Added column '{0}' to pending columns for table '{1}'", new Object[]{newColumnName, tableName});
            // Update UI to show new column
            SwingUtilities.invokeLater(() -> {
                refreshInputFields();
                LOGGER.log(Level.INFO, "addColumnAction: Refreshed dialog UI with new pending column '{0}'", newColumnName);
            });
        }
    }

    public static void showAddDialog(JFrame parent, TableManager tableManager) {
        AddRowEntry dialog = new AddRowEntry(parent, tableManager);
        dialog.setVisible(true);
    }
}