package database_creator.Table_Editor;

import java.awt.BorderLayout;
import java.sql.SQLException;

import javax.swing.JPanel;

public final class TableEditor extends JPanel {
    private final TableUIComponents uiComponents;
    private final TableSchemaManager schemaManager;
    private final TableOperationHandler operationHandler;

    public TableEditor() {
        setLayout(new BorderLayout(10, 10));

        // Initialize components
        uiComponents = new TableUIComponents(this);
        schemaManager = new TableSchemaManager(uiComponents.getTableComboBox());
        operationHandler = new TableOperationHandler(this, uiComponents.getTableComboBox(), uiComponents.getFieldsTable(), uiComponents.getTableModel(), uiComponents.getFields(), schemaManager);

        // Set operation handler in UI components
        uiComponents.setOperationHandler(operationHandler);

        // Add components to panel
        add(uiComponents.getControlPanel(), BorderLayout.NORTH);
        add(uiComponents.getTableScrollPane(), BorderLayout.CENTER);

        // Load initial table list
        try {
            schemaManager.loadTableList();
        } catch (SQLException e) {
            showMessageDialog("Error", "Failed to load table list: " + e.getMessage(), 0);
        }
    }

    public void showMessageDialog(String title, String message, int messageType) {
        uiComponents.showMessageDialog(title, message, messageType);
    }
}