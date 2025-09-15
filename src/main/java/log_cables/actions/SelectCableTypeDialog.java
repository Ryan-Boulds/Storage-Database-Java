package log_cables.actions;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import log_cables.CablesDAO;

public class SelectCableTypeDialog {
    private final JDialog dialog;
    private final JList<String> cableTypeList;
    private final JTextField searchField;
    private final List<String> allCableTypes;
    private String selectedType = null;
    private static final Logger LOGGER = Logger.getLogger(SelectCableTypeDialog.class.getName());

    public SelectCableTypeDialog(JFrame parent) {
        allCableTypes = new ArrayList<>();
        dialog = new JDialog(parent, "Select Cable Type", true);
        dialog.setSize(400, 400);
        dialog.setLayout(new BorderLayout(10, 10));

        // Load cable types
        try {
            allCableTypes.addAll(CablesDAO.getUniqueCableTypes());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading cable types: {0}", e.getMessage());
            JOptionPane.showMessageDialog(dialog, "Error loading cable types: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Search bar and Add New button
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterList(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterList(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterList(); }
        });
        searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JButton addNewButton = new JButton("Add New");
        addNewButton.addActionListener(e -> {
            String input = searchField.getText().trim();
            if (input.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a cable type in the search bar", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (allCableTypes.stream().anyMatch(t -> t.equalsIgnoreCase(input))) {
                JOptionPane.showMessageDialog(dialog, "Cable type already exists (case-insensitive)", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Show confirmation popup with bold text
            String message = "<html>Do you want to label the cable as: <b>" + input + "</b></html>";
            int confirm = JOptionPane.showConfirmDialog(dialog, message, "Confirm New Cable Type", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                selectedType = input;
                dialog.dispose();
            }
            // No option returns to the dialog (no action needed)
        });
        searchPanel.add(addNewButton, BorderLayout.EAST);

        // Cable type list
        cableTypeList = new JList<>(allCableTypes.toArray(new String[0]));
        cableTypeList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScrollPane = new JScrollPane(cableTypeList);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton selectButton = new JButton("Select");
        selectButton.addActionListener(e -> {
            String selected = cableTypeList.getSelectedValue();
            if (selected != null) {
                selectedType = selected;
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please select a cable type", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);

        dialog.add(searchPanel, BorderLayout.NORTH);
        dialog.add(listScrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(parent);
    }

    public String showDialog() {
        dialog.setVisible(true);
        return selectedType;
    }

    private void filterList() {
        String filter = searchField.getText().trim().toLowerCase();
        List<String> filteredTypes = new ArrayList<>();
        for (String type : allCableTypes) {
            if (type.toLowerCase().contains(filter)) {
                filteredTypes.add(type);
            }
        }
        cableTypeList.setListData(filteredTypes.toArray(new String[0]));
    }
}