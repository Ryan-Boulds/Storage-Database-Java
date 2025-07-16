package database_creator.Table_Editor;

import java.awt.BorderLayout;

import javax.swing.JPanel;

public class TableEditor extends JPanel {
    private final TableUIComponents uiComponents;
    private final TableSchemaManager schemaManager;
    private final TableOperationHandler operationHandler;

    public TableEditor() {
        setLayout(new BorderLayout(10, 10));

        // Initialize components
        uiComponents = new TableUIComponents(this);
        schemaManager = new TableSchemaManager(this, uiComponents.getTableComboBox());
        operationHandler = new TableOperationHandler(this, uiComponents.getTableComboBox(), uiComponents.getFieldsTable(), uiComponents.getTableModel(), uiComponents.getFields(), schemaManager);

        // Set operation handler in UI components
        uiComponents.setOperationHandler(operationHandler);

        // Add components to panel
        add(uiComponents.getControlPanel(), BorderLayout.NORTH);
        add(uiComponents.getTableScrollPane(), BorderLayout.CENTER);

        // Load initial table list
        schemaManager.loadTableList();
    }

    public void showMessageDialog(String title, String message, int messageType) {
        uiComponents.showMessageDialog(title, message, messageType);
    }
}