package view_software_list_tab;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import utils.UIComponentUtils;

public class FilterPanel {
    private final JPanel filterPanel;
    private final JTextField searchField;

    public FilterPanel(TriConsumer<String, String, String> filterAction, Runnable refreshAction) {
        filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = UIComponentUtils.createFormattedTextField();
        searchField.setPreferredSize(new Dimension(200, 30));

        JButton searchButton = UIComponentUtils.createFormattedButton("Search");
        JButton refreshButton = UIComponentUtils.createFormattedButton("Refresh");

        filterPanel.add(UIComponentUtils.createAlignedLabel("Search:"));
        filterPanel.add(searchField);
        filterPanel.add(searchButton);
        filterPanel.add(refreshButton);

        searchButton.addActionListener(e -> filterAction.accept(getSearchText(), "All", "All"));
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            refreshAction.run();
        });
    }

    public JPanel getPanel() {
        return filterPanel;
    }

    public String getSearchText() {
        return searchField.getText().toLowerCase();
    }
}