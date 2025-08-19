package database_creator;

import java.util.List;

import javax.swing.table.AbstractTableModel;

public class FieldsTableModel extends AbstractTableModel {
    private final List<Field> fields;
    
    public FieldsTableModel(List<Field> fields) {
        this.fields = fields;
    }

    @Override
    public int getRowCount() {
        return fields.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? "Field Name" : "Field Type";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Field field = fields.get(rowIndex);
        return columnIndex == 0 ? field.getName() : field.getType();
    }
}