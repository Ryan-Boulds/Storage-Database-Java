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
                createDefaultTables();
                statusLabel.setText("Database initialized successfully.");
            } catch (SQLException e) {
                statusLabel.setText("Error: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error initializing database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void createDefaultTables() throws SQLException {
        if (!validateDatabasePath()) {
            statusLabel.setText("Invalid database path.");
            throw new SQLException("Please enter a valid .accdb file path.");
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            createSettingsTable(conn, metaData);
            createAccessoriesTable(conn, metaData);
            createCablesTable(conn, metaData);
            createAdaptersTable(conn, metaData);
            createTemplatesTable(conn, metaData);
            createLicenseKeyRulesTable(conn, metaData);
            statusLabel.setText("Default tables checked/created successfully.");
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

    private void createSettingsTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Settings")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Settings (" +
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

    public void designDatabase() {
        TableEditor editor = new TableEditor();
        JOptionPane.showMessageDialog(this, editor, "Table Editor", JOptionPane.PLAIN_MESSAGE);
    }
}