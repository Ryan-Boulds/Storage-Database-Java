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
    private final JLabel statusLabel;
    private static final String DEFAULT_DB_PATH = "C:\\Users\\ami6985\\OneDrive - AISIN WORLD CORP\\Documents\\InventoryManagement.accdb";

    public DatabaseCreatorTab() {
        setLayout(new BorderLayout(10, 10));

        // Input panel for database path
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dbPathField = UIComponentUtils.createFormattedTextField();
        dbPathField.setColumns(30);
        dbPathField.setText(DEFAULT_DB_PATH);
        JButton browseButton = UIComponentUtils.createFormattedButton("Browse");
        browseButton.addActionListener(e -> browseDatabaseFile());
        JButton createButton = UIComponentUtils.createFormattedButton("Create Tables");
        createButton.addActionListener(e -> createMissingTables());
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
            createMissingTables();
        } else {
            statusLabel.setText("Invalid database path. Please select a valid .accdb file.");
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
            statusLabel.setText("Selected database: " + selectedFile.getName());
            createMissingTables();
        }
    }

    public void createMissingTables() {
        if (!validateDatabasePath()) {
            statusLabel.setText("Invalid database path.");
            JOptionPane.showMessageDialog(this, "Please enter a valid .accdb file path.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            createInventoryTable(conn, metaData);
            createAccessoriesTable(conn, metaData);
            createCablesTable(conn, metaData);
            createTemplatesTable(conn, metaData);
            statusLabel.setText("Tables checked/created successfully.");
            JOptionPane.showMessageDialog(this, "Database tables checked and created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error creating tables: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void createInventoryTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Inventory")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Inventory (" +
                    "Device_Name VARCHAR(255), " +
                    "Device_Type VARCHAR(255), " +
                    "Brand VARCHAR(255), " +
                    "Model VARCHAR(255), " +
                    "Serial_Number VARCHAR(255) PRIMARY KEY, " +
                    "Building_Location VARCHAR(255), " +
                    "Room_Desk VARCHAR(255), " +
                    "Specification VARCHAR(255), " +
                    "Processor_Type VARCHAR(255), " +
                    "Storage_Capacity VARCHAR(255), " +
                    "Network_Address VARCHAR(255), " +
                    "OS_Version VARCHAR(255), " +
                    "Department VARCHAR(255), " +
                    "Added_Memory VARCHAR(255), " +
                    "Status VARCHAR(255), " +
                    "Assigned_User VARCHAR(255), " +
                    "Warranty_Expiry_Date DATE, " +
                    "Last_Maintenance DATE, " +
                    "Maintenance_Due DATE, " +
                    "Date_Of_Purchase DATE, " +
                    "Purchase_Cost DOUBLE, " +
                    "Vendor VARCHAR(255), " +
                    "Memory_RAM VARCHAR(255))"
                );
            }
        }
    }

    private void createAccessoriesTable(Connection conn, DatabaseMetaData metaData) throws SQLException {
        if (!tableExists(metaData, "Accessories")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE Accessories (" +
                    "Peripheral_Type VARCHAR(255) PRIMARY KEY, " +
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
                    "Cable_Type VARCHAR(255) PRIMARY KEY, " +
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
                    "Template_Name VARCHAR(255) PRIMARY KEY, " +
                    "Description VARCHAR(255))"
                );
            }
        }
    }

    // Database designer feature
    public void designDatabase() {
        // TODO: Extend with additional designer features (e.g., edit relationships)
        TableEditor editor = new TableEditor();
        JOptionPane.showMessageDialog(this, editor, "Table Editor", JOptionPane.PLAIN_MESSAGE);
    }
}