package database_creator.Table_Editor;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import utils.UIComponentUtils;

public class TableUIComponents {
    private final TableEditor editor;
    private final JComboBox<String> tableComboBox;
    private final JTable fieldsTable;
    private final DefaultTableModel tableModel;
    private final List<Map<String, String>> fields = new ArrayList<>();
    private final JPopupMenu columnMenu;
    private TableOperationHandler operationHandler;
    private final JPanel controlPanel;
    private final JScrollPane tableScrollPane;

    public TableUIComponents(TableEditor editor) {
        this.editor = editor;
        tableModel = new DefaultTableModel(new String[]{"Field Name", "Field Type", "Primary Key"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        fieldsTable = new JTable(tableModel);

        // Table selection
        JPanel topPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        tableComboBox = new JComboBox<>();
        tableComboBox.addActionListener(e -> operationHandler.loadTableSchema());
        topPanel.add(new JLabel("Select Table:"));
        topPanel.add(tableComboBox);

        // New table input
        JTextField newTableNameField = UIComponentUtils.createFormattedTextField();
        topPanel.add(new JLabel("New Table Name:"));
        topPanel.add(newTableNameField);

        // Field input for new tables
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        JTextField fieldNameField = UIComponentUtils.createFormattedTextField();
        JComboBox<String> fieldTypeComboBox = new JComboBox<>(new String[]{"TEXT", "INTEGER", "DOUBLE", "DATE", "VARCHAR(255)"});
        JComboBox<String> primaryKeyComboBox = new JComboBox<>(new String[]{"No", "Yes"});
        inputPanel.add(new JLabel("New Field Name:"));
        inputPanel.add(fieldNameField);
        inputPanel.add(new JLabel("Field Type:"));
        inputPanel.add(fieldTypeComboBox);
        inputPanel.add(new JLabel("Primary Key:"));
        inputPanel.add(primaryKeyComboBox);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton addFieldButton = UIComponentUtils.createFormattedButton("Add Field");
        addFieldButton.addActionListener(e -> {
            operationHandler.addField(fieldNameField, fieldTypeComboBox, primaryKeyComboBox);
            showMessageDialog("Status", "Field added to new table definition.", 1);
        });
        JButton createTableButton = UIComponentUtils.createFormattedButton("Create New Table");
        createTableButton.addActionListener(e -> operationHandler.createNewTable(newTableNameField));
        JButton addColumnButton = UIComponentUtils.createFormattedButton("Add Column to Table");
        addColumnButton.addActionListener(e -> operationHandler.addColumn());
        JButton deleteTableButton = UIComponentUtils.createFormattedButton("Delete Table");
        deleteTableButton.addActionListener(e -> operationHandler.deleteTable());

        buttonPanel.add(addFieldButton);
        buttonPanel.add(createTableButton);
        buttonPanel.add(addColumnButton);
        buttonPanel.add(deleteTableButton);

        controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(topPanel, BorderLayout.NORTH);
        controlPanel.add(inputPanel, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Fields table with context menu
        columnMenu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename Column");
        renameItem.addActionListener(e -> operationHandler.renameColumn());
        JMenuItem changeTypeItem = new JMenuItem("Change Column Type");
        changeTypeItem.addActionListener(e -> operationHandler.changeColumnType());
        JMenuItem moveLeftItem = new JMenuItem("Move Left");
        moveLeftItem.addActionListener(e -> operationHandler.moveColumn(-1));
        JMenuItem moveRightItem = new JMenuItem("Move Right");
        moveRightItem.addActionListener(e -> operationHandler.moveColumn(1));
        JMenuItem setPrimaryKeyItem = new JMenuItem("Set Primary Key");
        setPrimaryKeyItem.addActionListener(e -> operationHandler.setPrimaryKey());
        JMenuItem removePrimaryKeyItem = new JMenuItem("Remove Primary Key");
        removePrimaryKeyItem.addActionListener(e -> operationHandler.removePrimaryKey());
        JMenuItem deleteColumnItem = new JMenuItem("Delete Column");
        deleteColumnItem.addActionListener(e -> operationHandler.deleteColumn());

        columnMenu.add(renameItem);
        columnMenu.add(changeTypeItem);
        columnMenu.add(moveLeftItem);
        columnMenu.add(moveRightItem);
        columnMenu.add(setPrimaryKeyItem);
        columnMenu.add(removePrimaryKeyItem);
        columnMenu.add(deleteColumnItem);

        fieldsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() && fieldsTable.getSelectedRow() != -1) {
                    int row = fieldsTable.getSelectedRow();
                    moveLeftItem.setEnabled(row > 0);
                    moveRightItem.setEnabled(row < fieldsTable.getRowCount() - 1);
                    String currentPK = null;
                    for (Map<String, String> field : fields) {
                        if (field.get("primaryKey").equals("Yes")) {
                            currentPK = field.get("name");
                            break;
                        }
                    }
                    String selectedColumn = (String) tableModel.getValueAt(row, 0);
                    removePrimaryKeyItem.setEnabled(currentPK != null && currentPK.equals(selectedColumn));
                    columnMenu.show(fieldsTable, e.getX(), e.getY());
                }
            }
        });

        tableScrollPane = UIComponentUtils.createScrollableContentPanel(fieldsTable);
    }

    public void setOperationHandler(TableOperationHandler handler) {
        this.operationHandler = handler;
    }

    public JPanel getControlPanel() {
        return controlPanel;
    }

    public JScrollPane getTableScrollPane() {
        return tableScrollPane;
    }

    public JComboBox<String> getTableComboBox() {
        return tableComboBox;
    }

    public JTable getFieldsTable() {
        return fieldsTable;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public List<Map<String, String>> getFields() {
        return fields;
    }

    public void showMessageDialog(String title, String message, int messageType) {
        javax.swing.JTextArea textArea = new javax.swing.JTextArea(message);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(textArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(400, 100));
        javax.swing.JOptionPane.showOptionDialog(editor, scrollPane, title, javax.swing.JOptionPane.DEFAULT_OPTION,
            messageType == 0 ? javax.swing.JOptionPane.ERROR_MESSAGE : javax.swing.JOptionPane.INFORMATION_MESSAGE,
            null, new Object[]{"OK"}, "OK");
    }
}