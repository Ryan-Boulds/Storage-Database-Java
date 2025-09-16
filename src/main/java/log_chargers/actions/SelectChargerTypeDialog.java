package log_chargers.actions;

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

import log_chargers.ChargersDAO;

public class SelectChargerTypeDialog {
    private final JDialog dialog;
    private final JList<String> chargerTypeList;
    private final JTextField searchField;
    private final List<String> allChargerTypes;
    private String selectedType = null;
    private static final Logger LOGGER = Logger.getLogger(SelectChargerTypeDialog.class.getName());

    public SelectChargerTypeDialog(JFrame parent) {
        allChargerTypes = new ArrayList<>();
        dialog = new JDialog(parent, "Select Charger Type", true);
        dialog.setSize(400, 400);
        dialog.setLayout(new BorderLayout(10, 10));

        // Load charger types
        try {
            allChargerTypes.addAll(ChargersDAO.getUniqueChargerTypes());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading charger types: {0}", e.getMessage());
            JOptionPane.showMessageDialog(dialog, "Error loading charger types: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(dialog, "Please enter a charger type in the search bar", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (allChargerTypes.stream().anyMatch(t -> t.equalsIgnoreCase(input))) {
                JOptionPane.showMessageDialog(dialog, "Charger type already exists (case-insensitive)", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Show confirmation popup with bold text
            String message = "<html>Do you want to label the charger as: <b>" + input + "</b></html>";
            int confirm = JOptionPane.showConfirmDialog(dialog, message, "Confirm New Charger Type", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                selectedType = input;
                dialog.dispose();
            }
            // No option returns to the dialog (no action needed)
        });
        searchPanel.add(addNewButton, BorderLayout.EAST);

        // Charger type list
        chargerTypeList = new JList<>(allChargerTypes.toArray(new String[0]));
        chargerTypeList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScrollPane = new JScrollPane(chargerTypeList);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton selectButton = new JButton("Select");
        selectButton.addActionListener(e -> {
            String selected = chargerTypeList.getSelectedValue();
            if (selected != null) {
                selectedType = selected;
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please select a charger type", "Error", JOptionPane.ERROR_MESSAGE);
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
        for (String type : allChargerTypes) {
            if (type.toLowerCase().contains(filter)) {
                filteredTypes.add(type);
            }
        }
        chargerTypeList.setListData(filteredTypes.toArray(new String[0]));
    }
}