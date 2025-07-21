package data_import.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import data_import.ImportDataTab;
import utils.DefaultColumns;
import utils.UIComponentUtils;

public class MappingDialog {
    private final ImportDataTab parent;
    private final List<String[]> data;
    private final Map<String, String> columnMappings = new HashMap<>();
    private final Map<String, String> newFields = new HashMap<>();
    private final Map<String, String> deviceTypeMappings = new HashMap<>();

    public MappingDialog(ImportDataTab parent, List<String[]> data) {
        this.parent = parent;
        this.data = data;
    }

    public void showDialog() {
        // Preview Table
        String[] csvColumns = data.get(0);
        String[][] previewData = data.subList(1, Math.min(data.size(), 6)).toArray(new String[0][]);
        DefaultTableModel previewModel = new DefaultTableModel(csvColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (String[] row : previewData) {
            previewModel.addRow(row);
        }
        JTable previewTable = new JTable(previewModel);
        previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane previewScrollPane = UIComponentUtils.createScrollableContentPanel(previewTable);
        previewScrollPane.setPreferredSize(new Dimension(400, 300));

        // Mapping Panel
        JPanel mappingPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);

        // Normalize function to ignore case, spaces, hyphens, and underscores
        java.util.function.Function<String, String> normalize = s -> s.replaceAll("[\\s_-]", "").toLowerCase();

        // Auto-map based on normalized names with "None" option
        String[] comboOptions = Arrays.copyOf(csvColumns, csvColumns.length + 1);
        comboOptions[comboOptions.length - 1] = "None";
        String[] dbFields = DefaultColumns.getInventoryColumns();
        JComboBox<String>[] comboBoxes = new JComboBox[dbFields.length];
        for (int i = 0; i < dbFields.length; i++) {
            String dbField = dbFields[i];
            String normalizedDbField = normalize.apply(dbField);
            String defaultMatch = "None";
            for (String csvColumn : csvColumns) {
                String normalizedCsvColumn = normalize.apply(csvColumn);
                if (normalizedDbField.equals(normalizedCsvColumn)) {
                    defaultMatch = csvColumn;
                    break;
                }
            }
            JComboBox<String> comboBox = new JComboBox<>(new DefaultComboBoxModel<>(comboOptions));
            comboBox.setSelectedItem(defaultMatch);
            mappingPanel.add(new javax.swing.JLabel(dbField + ":"), gbc);
            gbc.gridx++;
            mappingPanel.add(comboBox, gbc);
            gbc.gridx = 0;
            gbc.gridy++;
            comboBoxes[i] = comboBox;

            // Initialize columnMappings with auto-mapped values
            if (!"None".equals(defaultMatch)) {
                columnMappings.put(defaultMatch, dbField);
            }
            // Add ActionListener to update mappings

            comboBox.addActionListener(e -> {
                String selected = (String) comboBox.getSelectedItem();
                if ("None".equals(selected)) {
                    for (Map.Entry<String, String> entry : columnMappings.entrySet()) {
                        if (entry.getValue().equals(dbField)) {
                            columnMappings.remove(entry.getKey());
                            break;
                        }
                    }
                } else if (selected != null) {
                    columnMappings.put(selected, dbField);
                }
                // Debug: Log current mappings
                System.out.println("Updated Column Mappings: " + columnMappings);
            });
        }

        // New Column Section
        JPanel newColumnPanel = new JPanel(new BorderLayout());
        JButton newColumnButton = new JButton("Create New Column");
        newColumnPanel.add(newColumnButton, BorderLayout.CENTER);
        gbc.gridx = 0;
        gbc.gridy++;
        mappingPanel.add(newColumnPanel, gbc);

        newColumnButton.addActionListener(e -> {
            String newFieldName = JOptionPane.showInputDialog(parent, "Enter new field name:");
            if (newFieldName != null && !newFieldName.trim().isEmpty()) {
                String type = (String) JOptionPane.showInputDialog(parent, "Select field type:",
                        "New Field Type", JOptionPane.QUESTION_MESSAGE, null,
                        new String[]{"VARCHAR(255)", "INTEGER", "DATE", "BOOLEAN"}, "VARCHAR(255)");
                if (type != null) {
                    newFields.put(newFieldName, type);
                    JOptionPane.showMessageDialog(parent, "New column '" + newFieldName + "' added.", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        // Wrap mappingPanel in a JScrollPane for vertical scrolling
        JScrollPane mappingScrollPane = UIComponentUtils.createScrollableContentPanel(mappingPanel);
        mappingScrollPane.setPreferredSize(new Dimension(400, 300));

        // Combine Preview and Mapping
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(previewScrollPane, BorderLayout.WEST);
        mainPanel.add(mappingScrollPane, BorderLayout.CENTER);

        // Create resizable dialog with OK and Cancel buttons
        JButton okButton = new JButton("Save Mappings");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(parent), "Map Columns to Database Fields", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.setSize(800, 400);
        dialog.setMinimumSize(new Dimension(600, 300));
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Handle button actions
        okButton.addActionListener(e -> {
            // Save mappings and close
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> {
            columnMappings.clear(); // Reset mappings on cancel
            dialog.dispose();
        });

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (JOptionPane.showConfirmDialog(dialog, "Do you want to save the mappings?", "Confirm Close",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    // Mappings are already updated via ActionListener
                } else {
                    columnMappings.clear(); // Reset if cancelled
                }
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    public Map<String, String> getNewFields() {
        return newFields;
    }

    public Map<String, String> getDeviceTypeMappings() {
        return deviceTypeMappings;
    }
}