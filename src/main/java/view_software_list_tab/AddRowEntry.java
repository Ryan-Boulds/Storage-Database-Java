package view_software_list_tab;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

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

import utils.DataUtils;
import utils.DatabaseUtils;
import utils.SQLGenerator;
import utils.UIComponentUtils;

public class AddRowEntry extends JDialog {
    private final HashMap<String, String> device;
    private final String[] columnNames;
    private final Map<String, Integer> columnTypes;
    private final JComponent[] inputs;
    private final JFrame parent;
    private final TableManager tableManager;
    private final String primaryKeyColumn = "AssetName";

    public AddRowEntry(JFrame parent, TableManager tableManager) {
        super(parent, "Add Row Entry", true);
        this.parent = parent;
        this.device = new HashMap<>();
        this.tableManager = tableManager;
        this.columnNames = tableManager.getColumns();
        this.columnTypes = tableManager.getColumnTypes();

        setLayout(new BorderLayout());
        setSize(600, 800);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        this.inputs = new JComponent[columnNames.length];

        int maxLabelWidth = 0;
        FontMetrics fm = panel.getFontMetrics(panel.getFont());
        for (String fieldName : columnNames) {
            maxLabelWidth = Math.max(maxLabelWidth, fm.stringWidth(fieldName + ":"));
        }
        maxLabelWidth += 20;

        for (int i = 0; i < columnNames.length; i++) {
            String fieldName = columnNames[i];
            JComponent input;
            String key = fieldName;
            Integer sqlType = columnTypes.getOrDefault(key, Types.VARCHAR);
            System.out.println("AddDialog: Column " + key + " SQL type: " + sqlType);

            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0;
            JPanel labelPanel = new JPanel(new BorderLayout());
            labelPanel.add(UIComponentUtils.createAlignedLabel(fieldName + ":"), BorderLayout.WEST);
            labelPanel.setPreferredSize(new java.awt.Dimension(maxLabelWidth, 30));
            panel.add(labelPanel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            if (sqlType == null) {
                JTextField textField = UIComponentUtils.createFormattedTextField();
                textField.setText("");
                textField.setPreferredSize(new java.awt.Dimension(200, 30));
                input = textField;
            } else {
                switch (sqlType) {
                    case Types.DATE:
                    case Types.TIMESTAMP:
                        JPanel datePicker = UIComponentUtils.createFormattedDatePicker();
                        JTextField dateField = (JTextField) datePicker.getComponent(0);
                        dateField.setText("");
                        dateField.setPreferredSize(new java.awt.Dimension(200, 30));
                        input = datePicker;
                        break;
                    case Types.DOUBLE:
                    case Types.FLOAT:
                    case Types.DECIMAL:
                    case Types.NUMERIC:
                        JTextField doubleField = UIComponentUtils.createFormattedTextField();
                        doubleField.setText("");
                        doubleField.setPreferredSize(new java.awt.Dimension(200, 30));
                        input = doubleField;
                        break;
                    case Types.BIT:
                    case Types.BOOLEAN:
                        JCheckBox checkBox = new JCheckBox();
                        checkBox.setSelected(false);
                        checkBox.setPreferredSize(new java.awt.Dimension(200, 30));
                        input = checkBox;
                        break;
                    default:
                        JTextField textField = UIComponentUtils.createFormattedTextField();
                        textField.setText("");
                        textField.setPreferredSize(new java.awt.Dimension(200, 30));
                        input = textField;
                        break;
                }
            }
            inputs[i] = input;
            panel.add(input, gbc);
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveAction());
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        JButton addColumnButton = new JButton("Add Column");
        addColumnButton.addActionListener(e -> addColumnAction());
        buttonPanel.add(addColumnButton);

        JButton deleteColumnButton = new JButton("Delete Column");
        deleteColumnButton.addActionListener(e -> deleteColumnAction());
        buttonPanel.add(deleteColumnButton);

        JButton renameColumnButton = new JButton("Rename Column");
        renameColumnButton.addActionListener(e -> renameColumnAction());
        buttonPanel.add(renameColumnButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void saveAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Adding rows is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: Attempted to add row in Inventory table, which is not allowed");
            return;
        }

        for (int i = 0; i < columnNames.length; i++) {
            String fieldName = columnNames[i];
            JComponent input = inputs[i];
            String value;
            if (input instanceof JTextField) {
                value = ((JTextField) input).getText().trim();
            } else if (input instanceof JPanel && ((JPanel) input).getComponent(0) instanceof JTextField) {
                value = ((JTextField) ((JPanel) input).getComponent(0)).getText().trim();
            } else if (input instanceof JCheckBox) {
                value = Boolean.toString(((JCheckBox) input).isSelected());
            } else {
                continue;
            }
            device.put(fieldName, value);
        }

        String primaryKey = device.get(primaryKeyColumn);
        if (primaryKey == null || primaryKey.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Error: Asset Name cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if AssetName already exists
        String sql = "SELECT COUNT(*) FROM [" + tableName + "] WHERE AssetName = ?";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, primaryKey);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(this, "Error: Asset Name '" + primaryKey + "' already exists", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error checking Asset Name: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: SQLException checking existence in table '" + tableName + "': " + e.getMessage());
            return;
        }

        String validationError = DataUtils.validateDevice(device, null);
        if (validationError != null) {
            JOptionPane.showMessageDialog(this, "Validation Error: " + validationError, "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: Validation error in table '" + tableName + "': " + validationError);
            return;
        }

        // Insert the new device
        String insertSql = SQLGenerator.generateInsertSQL(tableName, device);
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            int index = 1;
            for (String value : device.values()) {
                stmt.setString(index++, value != null ? value : "");
            }
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Row added successfully");
            dispose();
            SwingUtilities.invokeLater(() -> tableManager.refreshDataAndTabs());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent, "Error adding row: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: SQLException in table '" + tableName + "': " + e.getMessage());
        }
    }

    private void addColumnAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Adding columns is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: Attempted to add column in Inventory table, which is not allowed");
            return;
        }

        String newColumnName = JOptionPane.showInputDialog(this, "Enter new column name:");
        if (newColumnName != null && !newColumnName.trim().isEmpty()) {
            newColumnName = newColumnName.trim();
            for (String column : columnNames) {
                if (column.equalsIgnoreCase(newColumnName)) {
                    JOptionPane.showMessageDialog(this, "Error: Column '" + newColumnName + "' already exists", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            try (Connection conn = DatabaseUtils.getConnection()) {
                String sql = "ALTER TABLE " + tableName + " ADD " + newColumnName + " VARCHAR(255)";
                conn.createStatement().executeUpdate(sql);
                JOptionPane.showMessageDialog(this, "Column added successfully");
                SwingUtilities.invokeLater(() -> {
                    tableManager.setTableName(tableName);
                    tableManager.refreshDataAndTabs();
                });
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("AddDialog: SQLException adding column to table '" + tableName + "': " + e.getMessage());
            }
        }
    }

    private void deleteColumnAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Deleting columns is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: Attempted to delete column in Inventory table, which is not allowed");
            return;
        }

        String columnToDelete = (String) JOptionPane.showInputDialog(
            this,
            "Select column to delete:",
            "Delete Column",
            JOptionPane.PLAIN_MESSAGE,
            null,
            columnNames,
            columnNames[0]
        );
        if (columnToDelete != null && !columnToDelete.equals(primaryKeyColumn)) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the column '" + columnToDelete + "'? This will remove all data in this column.",
                "Confirm Delete Column",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseUtils.getConnection()) {
                    String sql = "ALTER TABLE " + tableName + " DROP COLUMN " + columnToDelete;
                    conn.createStatement().executeUpdate(sql);
                    JOptionPane.showMessageDialog(this, "Column '" + columnToDelete + "' deleted successfully");
                    SwingUtilities.invokeLater(() -> {
                        tableManager.setTableName(tableName);
                        tableManager.refreshDataAndTabs();
                    });
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error deleting column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("AddDialog: SQLException deleting column in table '" + tableName + "': " + e.getMessage());
                }
            }
        } else if (columnToDelete != null && columnToDelete.equals(primaryKeyColumn)) {
            JOptionPane.showMessageDialog(this, "Error: Cannot delete the primary key column '" + primaryKeyColumn + "'", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: Attempted to delete primary key column '" + primaryKeyColumn + "' in table '" + tableName + "'");
        }
    }

    private void renameColumnAction() {
        String tableName = tableManager.getTableName();
        if ("Inventory".equals(tableName)) {
            JOptionPane.showMessageDialog(this, "Error: Renaming columns is not allowed for the Inventory table", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("AddDialog: Attempted to rename column in Inventory table, which is not allowed");
            return;
        }

        String oldColumnName = (String) JOptionPane.showInputDialog(
            this,
            "Select column to rename:",
            "Rename Column",
            JOptionPane.PLAIN_MESSAGE,
            null,
            columnNames,
            columnNames[0]);
        if (oldColumnName != null && !oldColumnName.equals(primaryKeyColumn)) {
            String newColumnName = JOptionPane.showInputDialog(this, "Enter new column name for " + oldColumnName + ":");
            if (newColumnName != null && !newColumnName.trim().isEmpty()) {
                newColumnName = newColumnName.trim();
                for (String column : columnNames) {
                    if (column.equalsIgnoreCase(newColumnName)) {
                        JOptionPane.showMessageDialog(this, "Error: Column '" + newColumnName + "' already exists", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                try (Connection conn = DatabaseUtils.getConnection()) {
                    String sql = "ALTER TABLE " + tableName + " RENAME COLUMN " + oldColumnName + " TO " + newColumnName;
                    conn.createStatement().executeUpdate(sql);
                    JOptionPane.showMessageDialog(this, "Column renamed successfully");
                    SwingUtilities.invokeLater(() -> {
                        tableManager.setTableName(tableName);
                        tableManager.refreshDataAndTabs();
                    });
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error renaming column: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    System.err.println("AddDialog: SQLException renaming column in table '" + tableName + "': " + e.getMessage());
                }
            }
        }
    }

    public static void showAddDialog(JFrame parent, TableManager tableManager) {
        AddRowEntry dialog = new AddRowEntry(parent, tableManager);
        dialog.setVisible(true);
    }
}