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

public class ModifyRowEntry extends JDialog {
    private String[] columnNames;
    private final Map<String, Integer> columnTypes;
    private JComponent[] inputs;
    private final TableManager tableManager;
    private final JPanel inputPanel;
    private final JScrollPane scrollPane;
    private final String primaryKey;
    private final Map<String, String> device;
    private static final Logger LOGGER = Logger.getLogger(ModifyRowEntry.class.getName());

    public ModifyRowEntry(JFrame parent, Map<String, String> device, TableManager tableManager) {
        super(parent, "Modify Row Entry", true);
        this.tableManager = tableManager;
        this.device = new HashMap<>(device);
        this.primaryKey = device.get("AssetName");
        this.columnNames = tableManager.getColumns();
        this.columnTypes = tableManager.getColumnTypes();

        setLayout(new BorderLayout());
        setSize(600, 800);
        setLocationRelativeTo(parent);

        inputPanel = new JPanel(new GridBagLayout());
        scrollPane = new JScrollPane(inputPanel);
        add(scrollPane, BorderLayout.CENTER);

        refreshInputFields();

        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveAction());
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        for (String column : columnNames) {
            LOGGER.log(Level.INFO, "ModifyDialog: Column {0} SQL type: {1}", new Object[]{column, columnTypes.get(column)});
        }
    }

    private void refreshInputFields() {
        LOGGER.log(Level.INFO, "refreshInputFields: Starting refresh, current columns: {0}", String.join(", ", columnNames));
        columnNames = tableManager.getColumns();
        inputs = new JComponent[columnNames.length];
        inputPanel.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        FontMetrics fm = inputPanel.getFontMetrics(inputPanel.getFont());
        int maxLabelWidth = 0;
        for (String columnName : columnNames) {
            maxLabelWidth = Math.max(maxLabelWidth, fm.stringWidth(columnName));
        }

        for (String columnName : columnNames) {
            if (!device.containsKey(columnName)) {
                device.put(columnName, "");
            }
        }
        device.keySet().removeIf(key -> !Arrays.asList(columnNames).contains(key) && !key.equals("AssetName"));

        for (int i = 0; i < columnNames.length; i++) {
            String columnName = columnNames[i];
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.anchor = GridBagConstraints.EAST;
            inputPanel.add(UIComponentUtils.createAlignedLabel(columnName), gbc);

            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            Integer sqlType = columnTypes.get(columnName);
            if (sqlType != null && sqlType == Types.BIT) {
                inputs[i] = new JCheckBox();
                String value = device.get(columnName);
                ((JCheckBox) inputs[i]).setSelected("true".equalsIgnoreCase(value));
            } else {
                inputs[i] = UIComponentUtils.createFormattedTextField();
                ((JTextField) inputs[i]).setColumns(20);
                String value = device.get(columnName);
                ((JTextField) inputs[i]).setText(value != null ? value : "");
            }
            inputPanel.add(inputs[i], gbc);
        }

        inputPanel.revalidate();
        inputPanel.repaint();
        scrollPane.revalidate();
        scrollPane.repaint();
        SwingUtilities.invokeLater(() -> {
            inputPanel.revalidate();
            inputPanel.repaint();
            scrollPane.revalidate();
            scrollPane.repaint();
            LOGGER.log(Level.INFO, "refreshInputFields: Completed UI refresh, new columns: {0}", String.join(", ", columnNames));
        });
    }

    private void saveAction() {
        String tableName = tableManager.getTableName();
        columnNames = tableManager.getColumns();
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            String columnName = columnNames[i];
            if (inputs[i] instanceof JTextField) {
                String text = ((JTextField) inputs[i]).getText().trim();
                values.put(columnName, text.isEmpty() ? null : text);
            } else if (inputs[i] instanceof JCheckBox) {
                values.put(columnName, ((JCheckBox) inputs[i]).isSelected() ? "true" : "false");
            }
        }

        String newAssetName = values.get("AssetName");
        if (newAssetName == null || newAssetName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Error: Asset Name cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Attempted to save empty AssetName in table '{0}'", tableName);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            StringBuilder sql = new StringBuilder("UPDATE [" + tableName + "] SET ");
            List<String> setClauses = new ArrayList<>();
            List<Object> parameters = new ArrayList<>();
            for (String column : columnNames) {
                if (!column.equals("AssetName")) {
                    setClauses.add("[" + column + "] = ?");
                    parameters.add(values.get(column));
                }
            }
            sql.append(String.join(", ", setClauses));
            sql.append(" WHERE [AssetName] = ?");
            parameters.add(primaryKey);

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < parameters.size(); i++) {
                    ps.setString(i + 1, (String) parameters.get(i));
                }
                ps.executeUpdate();
            }

            if (!newAssetName.equals(primaryKey)) {
                String updatePkSql = "UPDATE [" + tableName + "] SET [AssetName] = ? WHERE [AssetName] = ?";
                try (PreparedStatement ps = conn.prepareStatement(updatePkSql)) {
                    ps.setString(1, newAssetName);
                    ps.setString(2, primaryKey);
                    ps.executeUpdate();
                }
            }

            JOptionPane.showMessageDialog(this, "Row updated successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            SwingUtilities.invokeLater(() -> {
                tableManager.refreshDataAndTabs();
                dispose();
            });
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating row: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "SQLException updating row in table '{0}': {1}", new Object[]{tableName, e.getMessage()});
        }
    }

    public static void showModifyDialog(JFrame parent, Map<String, String> device, TableManager tableManager) {
        ModifyRowEntry dialog = new ModifyRowEntry(parent, device, tableManager);
        dialog.setVisible(true);
    }
}