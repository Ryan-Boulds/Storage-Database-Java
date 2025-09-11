package database_creator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import database_creator.Table_Editor.TableEditor;
import utils.DatabaseUtils;
import utils.UIComponentUtils;

public class DatabaseCreatorTab extends JPanel {
    private final JTextField dbPathField;
    private JLabel statusLabel = new JLabel();

    public DatabaseCreatorTab() {
        setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dbPathField = UIComponentUtils.createFormattedTextField();
        dbPathField.setColumns(30);
        dbPathField.setText(DatabaseUtils.getDatabasePath());
        JButton browseButton = UIComponentUtils.createFormattedButton("Browse");
        browseButton.addActionListener(e -> browseDatabaseFile());
        JButton createButton = UIComponentUtils.createFormattedButton("Create Tables");
        createButton.addActionListener(e -> {
            try {
                createDefaultTables();
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Error creating tables: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton designButton = UIComponentUtils.createFormattedButton("Design Database");
        designButton.addActionListener(e -> designDatabase());
        inputPanel.add(new JLabel("Database Path:"));
        inputPanel.add(dbPathField);
        inputPanel.add(browseButton);
        inputPanel.add(createButton);
        inputPanel.add(designButton);

        statusLabel = new JLabel("Checking database...");
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Status:"));
        statusPanel.add(statusLabel);

        add(inputPanel, BorderLayout.NORTH);
        add(statusPanel, BorderLayout.SOUTH);

        initializeDatabase();
    }

    private void initializeDatabase() {
        if (validateDatabasePath()) {
            DatabaseUtils.setDatabasePath(dbPathField.getText().trim());
            try {
                createDefaultTables();
                statusLabel.setText("Database initialized successfully.");
            } catch (SQLException e) {
                statusLabel.setText("Error: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error initializing database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            statusLabel.setText("No database path set. Please select a valid .accdb file.");
        }
    }

    private boolean validateDatabasePath() {
        String path = dbPathField.getText().trim();
        if (path.isEmpty()) return false;
        File file = new File(path);
        return file.exists() && path.toLowerCase().endsWith(".accdb");
    }

    private void browseDatabaseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Access Database Files (*.accdb)", "accdb"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            dbPathField.setText(path);
            DatabaseUtils.setDatabasePath(path);
            initializeDatabase();
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private void createDefaultTables() throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            createAccessoriesTable(conn, metaData);
            createCablesTable(conn, metaData);
            createChargersTable(conn, metaData);
            createAdaptersTable(conn, metaData);
            createTemplatesTable(conn, metaData);
            createTableInformationTable(conn, metaData);
            createLicenseKeyRulesTable(conn, metaData);
            createLocationsTable(conn, metaData); // Added call to create Locations table
        }
    }

    private void createTableInformationTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "TableInformation")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE TableInformation (" +
                    "ID INTEGER PRIMARY KEY, " +
                    "InventoryTables TEXT, " +
                    "SoftwareTables TEXT)"
                );
            }
        }
    }

    private void createAccessoriesTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Accessories")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Accessories (" +
                    "ID AUTOINCREMENT PRIMARY KEY, " +
                    "Peripheral_Type VARCHAR(255), " +
                    "[Count] INTEGER, " +
                    "Location VARCHAR(255), " +
                    "Previous_Location VARCHAR(255))"
                );
            }
        }
    }

    private void createCablesTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Cables")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Cables (" +
                    "ID AUTOINCREMENT PRIMARY KEY, " +
                    "Cable_Type VARCHAR(255), " +
                    "[Count] INTEGER, " +
                    "Location VARCHAR(255), " +
                    "Previous_Location VARCHAR(255))"
                );
            }
        }
    }

    private void createChargersTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Chargers")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Chargers (" +
                    "ID AUTOINCREMENT PRIMARY KEY, " +
                    "Charger_Type VARCHAR(255), " +
                    "[Count] INTEGER, " +
                    "Location VARCHAR(255), " +
                    "Previous_Location VARCHAR(255))"
                );
            }
        }
    }

    private void createAdaptersTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Adapters")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Adapters (" +
                    "ID AUTOINCREMENT PRIMARY KEY, " +
                    "Adapter_Type VARCHAR(255), " +
                    "[Count] INTEGER, " +
                    "Location VARCHAR(255), " +
                    "Previous_Location VARCHAR(255))"
                );
            }
        }
    }

    private void createTemplatesTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Templates")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Templates (" +
                    "Template_Name TEXT PRIMARY KEY)"
                );
            }
        }
    }

    private void createLicenseKeyRulesTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "LicenseKeyRules")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE LicenseKeyRules (" +
                    "Rule_ID INTEGER PRIMARY KEY, " +
                    "Rule_Name TEXT, " +
                    "Rule_Description TEXT)"
                );
            }
        }
    }

    private void createLocationsTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Locations")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Locations (" +
                    "ID AUTOINCREMENT PRIMARY KEY, " +
                    "Datatype VARCHAR(255), " +
                    "Location VARCHAR(255))"
                );
            }
        }
    }

    public void designDatabase() {
        TableEditor editor = new TableEditor();
        JOptionPane.showMessageDialog(this, editor, "Table Editor", JOptionPane.PLAIN_MESSAGE);
    }
}