package database_creator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import database_creator.Table_Editor.TableEditor;
import utils.DatabaseUtils;
import utils.DefaultColumns;
import utils.UIComponentUtils;

public class DatabaseCreatorTab extends JPanel {
    private final JTextField dbPathField;
    private final JLabel statusLabel;

    public DatabaseCreatorTab() {
        setLayout(new BorderLayout(10, 10));

        // Input panel for database path
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dbPathField = UIComponentUtils.createFormattedTextField();
        dbPathField.setColumns(30);
        dbPathField.setText(DatabaseUtils.getDatabasePath());
        JButton browseButton = UIComponentUtils.createFormattedButton("Browse");
        browseButton.addActionListener(e -> browseDatabaseFile());
        JButton createButton = UIComponentUtils.createFormattedButton("Create Tables");
        createButton.addActionListener(e -> {
            try {
                createMissingTables();
            } catch (SQLException ex) {
            }
        });
        JButton designButton = UIComponentUtils.createFormattedButton("Design Database");
        designButton.addActionListener(e -> designDatabase());
        inputPanel.add(new JLabel("Database Path:"));
        inputPanel.add(dbPathField);
        inputPanel.add(browseButton);
        inputPanel.add(createButton);
        inputPanel.add(designButton);

        // Status panel
        statusLabel = new JLabel("Checking database...");
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Status:"));
        statusPanel.add(statusLabel);

        add(inputPanel, BorderLayout.NORTH);
        add(statusPanel, BorderLayout.SOUTH);

        // Initialize database tables on tab creation
        initializeDatabase();
    }

    private void initializeDatabase() {
        if (validateDatabasePath()) {
            DatabaseUtils.setDatabasePath(dbPathField.getText().trim());
            try {
                createMissingTables();
                statusLabel.setText("Database initialized successfully.");
            } catch (SQLException e) {
                statusLabel.setText("Error: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error initializing database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            statusLabel.setText("No database path set. Please select a valid .accdb file.");
            JOptionPane.showMessageDialog(this, "Please select a valid .accdb file to initialize the database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateDatabasePath() {
        String dbPath = dbPathField.getText().trim();
        if (dbPath.isEmpty() || !dbPath.toLowerCase().endsWith(".accdb")) {
            return false;
        }
        File dbFile = new File(dbPath);
        return dbFile.exists() || dbFile.getParentFile().canWrite();
    }

    private void browseDatabaseFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Access Database Files (*.accdb)", "accdb");
        fileChooser.setFileFilter(filter);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            dbPathField.setText(selectedFile.getAbsolutePath());
            DatabaseUtils.setDatabasePath(selectedFile.getAbsolutePath());
            statusLabel.setText("Selected database: " + selectedFile.getName());
            try {
                createMissingTables();
                statusLabel.setText("Database initialized successfully.");
            } catch (SQLException e) {
                statusLabel.setText("Error: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error initializing database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void createMissingTables() throws SQLException {
        if (!validateDatabasePath()) {
            statusLabel.setText("Invalid database path.");
            throw new SQLException("Please enter a valid .accdb file path.");
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            createInventoryTable(conn, metaData);
            createAccessoriesTable(conn, metaData);
            createCablesTable(conn, metaData);
            createAdaptersTable(conn, metaData);
            createTemplatesTable(conn, metaData);
            statusLabel.setText("Tables checked/created successfully.");
        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
            throw e;
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void createInventoryTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Inventory")) {
            Map<String, String> columnDefs = DefaultColumns.getInventoryColumnDefinitions();
            StringBuilder sql = new StringBuilder("CREATE TABLE Inventory (");
            int i = 0;
            for (Map.Entry<String, String> entry : columnDefs.entrySet()) {
                String column = entry.getKey().equals("IP Address") || entry.getKey().equals("Created at") || 
                               entry.getKey().equals("Last Successful Scan") || entry.getKey().equals("Device Type") ? 
                               "[" + entry.getKey() + "]" : entry.getKey();
                sql.append(column).append(" ").append(entry.getValue());
                if (entry.getKey().equals("AssetName")) {
                    sql.append(" PRIMARY KEY");
                }
                if (i < columnDefs.size() - 1) {
                    sql.append(", ");
                }
                i++;
            }
            sql.append(")");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql.toString());
            }
        }
    }

    private void createAccessoriesTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Accessories")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Accessories (" +
                    "Peripheral_Type TEXT PRIMARY KEY, " +
                    "[Count] INTEGER)"
                );
            }
        }
    }

    private void createCablesTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Cables")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Cables (" +
                    "Cable_Type TEXT PRIMARY KEY, " +
                    "[Count] INTEGER)"
                );
            }
        }
    }

    private void createAdaptersTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Adapters")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Adapters (" +
                    "Adapter_Type TEXT PRIMARY KEY, " +
                    "[Count] INTEGER)"
                );
            }
        }
    }

    private void createTemplatesTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Templates")) {
            Map<String, String> columnDefs = DefaultColumns.getInventoryColumnDefinitions();
            StringBuilder sql = new StringBuilder("CREATE TABLE Templates (Template_Name TEXT PRIMARY KEY, ");
            int i = 0;
            for (Map.Entry<String, String> entry : columnDefs.entrySet()) {
                String column = entry.getKey().equals("IP Address") || entry.getKey().equals("Created at") || 
                               entry.getKey().equals("Last Successful Scan") || entry.getKey().equals("Device Type") ? 
                               "[" + entry.getKey() + "]" : entry.getKey();
                sql.append(column).append(" ").append(entry.getValue());
                if (i < columnDefs.size() - 1) {
                    sql.append(", ");
                }
                i++;
            }
            sql.append(")");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql.toString());
            }
        }
    }

    public void designDatabase() {
        TableEditor editor = new TableEditor();
        JOptionPane.showMessageDialog(this, editor, "Table Editor", JOptionPane.PLAIN_MESSAGE);
    }
}