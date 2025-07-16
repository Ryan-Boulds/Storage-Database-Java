package database_creator;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class FieldDialog extends JDialog {
    private final JTextField fieldNameField;
    private final JComboBox<String> fieldTypeCombo;
    private boolean confirmed = false;
    private Field field;

    public FieldDialog() {
        setTitle("Add/Edit Field");
        setSize(300, 200);
        setLocationRelativeTo(null);
        setModal(true);
        
        setLayout(new GridLayout(3, 2));
        
        // Field Name
        add(new JLabel("Field Name:"));
        fieldNameField = new JTextField();
        add(fieldNameField);
        
        // Field Type
        add(new JLabel("Field Type:"));
        fieldTypeCombo = new JComboBox<>(new String[]{"VARCHAR", "INTEGER", "BOOLEAN"});
        add(fieldTypeCombo);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Button actions
        okButton.addActionListener(e -> onOk());
        cancelButton.addActionListener(e -> onCancel());
    }

    private void onOk() {
        field = new Field(fieldNameField.getText(), (String) fieldTypeCombo.getSelectedItem());
        confirmed = true;
        setVisible(false);
    }

    private void onCancel() {
        confirmed = false;
        setVisible(false);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Field getField() {
        return field;
    }
}
