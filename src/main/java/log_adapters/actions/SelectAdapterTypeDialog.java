package log_adapters.actions;

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

import log_adapters.AdaptersDAO;

public class SelectAdapterTypeDialog {
    private final JDialog dialog;
    private final JList<String> adapterTypeList;
    private final JTextField searchField;
    private final List<String> allAdapterTypes;
    private String selectedType = null;
    private static final Logger LOGGER = Logger.getLogger(SelectAdapterTypeDialog.class.getName());

    public SelectAdapterTypeDialog(JFrame parent) {
        allAdapterTypes = new ArrayList<>();
        dialog = new JDialog(parent, "Select Adapter Type", true);
        dialog.setSize(400, 400);
        dialog.setLayout(new BorderLayout(10, 10));

        // Load adapter types
        try {
            allAdapterTypes.addAll(AdaptersDAO.getUniqueAdapterTypes());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading adapter types: {0}", e.getMessage());
            JOptionPane.showMessageDialog(dialog, "Error loading adapter types: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(dialog, "Please enter an adapter type in the search bar", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (allAdapterTypes.stream().anyMatch(t -> t.equalsIgnoreCase(input))) {
                JOptionPane.showMessageDialog(dialog, "Adapter type already exists (case-insensitive)", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Show confirmation popup with bold text
            String message = "<html>Do you want to label the adapter as: <b>" + input + "</b></html>";
            int confirm = JOptionPane.showConfirmDialog(dialog, message, "Confirm New Adapter Type", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                selectedType = input;
                dialog.dispose();
            }
            // No option returns to the dialog (no action needed)
        });
        searchPanel.add(addNewButton, BorderLayout.EAST);

        // Adapter type list
        adapterTypeList = new JList<>(allAdapterTypes.toArray(new String[0]));
        adapterTypeList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScrollPane = new JScrollPane(adapterTypeList);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton selectButton = new JButton("Select");
        selectButton.addActionListener(e -> {
            String selected = adapterTypeList.getSelectedValue();
            if (selected != null) {
                selectedType = selected;
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please select an adapter type", "Error", JOptionPane.ERROR_MESSAGE);
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
        for (String type : allAdapterTypes) {
            if (type.toLowerCase().contains(filter)) {
                filteredTypes.add(type);
            }
        }
        adapterTypeList.setListData(filteredTypes.toArray(new String[0]));
    }
}