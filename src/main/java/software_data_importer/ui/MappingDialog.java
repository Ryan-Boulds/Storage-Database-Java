package software_data_importer.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import software_data_importer.ImportDataTab;
import utils.DatabaseUtils;
import utils.UIComponentUtils;

public class MappingDialog {
    private final ImportDataTab parent;
    private final List<String[]> data;
    private final Map<String, String> columnMappings = new HashMap<>();
    private final Map<String, String> deviceTypeMappings = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(MappingDialog.class.getName());

    public MappingDialog(ImportDataTab parent, List<String[]> data) {
        this.parent = parent;
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public void showDialog() {
        String[] csvColumns = data.get(0);
        List<String[]> previewData = data.subList(1, data.size());
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
        previewScrollPane.setPreferredSize(new Dimension(800, 400));

        JPanel mappingPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);

        java.util.function.Function<String, String> normalize = s -> s.replaceAll("[\\s_-]", "").toLowerCase();
        String[] comboOptions = Arrays.copyOf(csvColumns, csvColumns.length + 1);
        comboOptions[comboOptions.length - 1] = "None";
        String[] dbFields;
        try {
            dbFields = DatabaseUtils.getInventoryColumnNames(parent.getSelectedTable()).toArray(new String[0]);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving columns for table {0}: {1}", 
                       new Object[]{parent.getSelectedTable(), e.getMessage()});
            JOptionPane.showMessageDialog(parent, "Error retrieving columns: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

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

            if (!"None".equals(defaultMatch)) {
                columnMappings.put(defaultMatch, dbField);
            }
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
                LOGGER.log(Level.INFO, "Updated Column Mappings: {0}", new Object[]{columnMappings});
            });
        }

        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("Save Mappings");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(previewScrollPane, BorderLayout.WEST);
        mainPanel.add(mappingPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(parent), "Map Columns to Database Fields", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.setSize(1200, 600);
        dialog.setMinimumSize(new Dimension(800, 400));
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        okButton.addActionListener(e -> {
            LOGGER.log(Level.INFO, "Mappings saved: {0}", new Object[]{columnMappings});
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> {
            columnMappings.clear();
            LOGGER.info("Mapping cancelled, mappings cleared.");
            dialog.dispose();
        });

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (JOptionPane.showConfirmDialog(dialog, "Do you want to save the mappings?", "Confirm Close",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    LOGGER.log(Level.INFO, "Mappings saved on window close: {0}", new Object[]{columnMappings});
                } else {
                    columnMappings.clear();
                    LOGGER.info("Mappings cleared on window close.");
                }
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    public Map<String, String> getDeviceTypeMappings() {
        return deviceTypeMappings;
    }
}