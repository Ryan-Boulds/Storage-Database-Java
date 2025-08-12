// New file: view_software_list_tab/license_key_tracker/LicenseKeyTracker.java
package view_software_list_tab.license_key_tracker;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import utils.DatabaseUtils;
import utils.UIComponentUtils;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

public class LicenseKeyTracker extends JDialog {
    private final String tableName;
    private JList<String> keyList;
    private JTable detailTable;
    private DefaultListModel<String> keyModel;

    public LicenseKeyTracker(JFrame parent, String tableName) {
        super(parent, "License Key Tracker - " + tableName, false);
        this.tableName = tableName;
        buildUI();
        loadKeys();
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        split.setDividerLocation(300);

        JPanel left = new JPanel(new BorderLayout());
        keyModel = new DefaultListModel<>();
        keyList = new JList<>(keyModel);
        JScrollPane keyScroll = new JScrollPane(keyList);
        left.add(keyScroll, BorderLayout.CENTER);

        JButton rulesButton = UIComponentUtils.createFormattedButton("Key Rules");
        rulesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                KeyRulesDialog settings = new KeyRulesDialog(LicenseKeyTracker.this);
                settings.setLocationRelativeTo(LicenseKeyTracker.this);
                settings.setVisible(true);
            }
        });
        left.add(rulesButton, BorderLayout.NORTH);

        split.setLeftComponent(left);

        detailTable = new JTable();
        JScrollPane detailScroll = new JScrollPane(detailTable);
        split.setRightComponent(detailScroll);

        content.add(split, BorderLayout.CENTER);
        setContentPane(content);

        keyList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selectedItem = keyList.getSelectedValue();
                    if (selectedItem != null) {
                        String selectedKey = selectedItem.substring(0, selectedItem.lastIndexOf(" ("));
                        loadDetails(selectedKey);
                    }
                }
            }
        });
    }

    private void loadKeys() {
        keyModel.clear();
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT License_Key, COUNT(*) as count FROM " + tableName + 
                 " WHERE License_Key IS NOT NULL GROUP BY License_Key ORDER BY License_Key")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String key = rs.getString("License_Key");
                int count = rs.getInt("count");
                keyModel.addElement(key + " (" + count + ")");
            }
        } catch (SQLException e) {
            System.err.println("LicenseKeyTracker: Error loading keys for table " + tableName + ": " + e.getMessage());
        }
    }

    private void loadDetails(String selectedKey) {
        DefaultTableModel model = new DefaultTableModel();
        Map<String, Integer> colTypes = new HashMap<>();
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM " + tableName + " WHERE License_Key = ?")) {
            pstmt.setString(1, selectedKey);
            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                String colName = meta.getColumnName(i);
                model.addColumn(colName);
                colTypes.put(colName, meta.getColumnType(i));
            }

            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
            inputFormat.setLenient(false);

            while (rs.next()) {
                Object[] row = new Object[colCount];
                for (int i = 1; i <= colCount; i++) {
                    String colName = meta.getColumnName(i);
                    Integer sqlType = colTypes.get(colName);
                    String value = rs.getString(i);
                    if (value != null && (sqlType == Types.DATE || sqlType == Types.TIMESTAMP ||
                        colName.equals("Warranty_Expiry_Date") || colName.equals("Last_Maintenance") ||
                        colName.equals("Maintenance_Due") || colName.equals("Date_Of_Purchase"))) {
                        try {
                            row[i - 1] = outputFormat.format(inputFormat.parse(value));
                        } catch (ParseException e) {
                            row[i - 1] = value;
                        }
                    } else {
                        row[i - 1] = value;
                    }
                }
                model.addRow(row);
            }
            detailTable.setModel(model);
            detailTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            // Optional: adjust column widths similar to TableManager
        } catch (SQLException e) {
            System.err.println("LicenseKeyTracker: Error loading details for key " + selectedKey + " in table " + tableName + ": " + e.getMessage());
        }
    }
}