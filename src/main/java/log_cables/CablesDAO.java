package log_cables;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import utils.DatabaseUtils;

public class CablesDAO {
    private static final Logger LOGGER = Logger.getLogger(CablesDAO.class.getName());

    public static class CableEntry {
        public final String cableType;
        public final int count;
        public final String previousLocation;

        public CableEntry(String cableType, int count, String previousLocation) {
            this.cableType = cableType;
            this.count = count;
            this.previousLocation = previousLocation;
        }
    }

    public static void ensureSchema() throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Check if Cables table exists
            boolean tableExists = false;
            try (ResultSet rs = metaData.getTables(null, null, "Cables", new String[]{"TABLE"})) {
                if (rs.next()) {
                    tableExists = true;
                    LOGGER.log(Level.INFO, "Cables table found: {0}", rs.getString("TABLE_NAME"));
                } else {
                    LOGGER.log(Level.INFO, "Cables table does not exist");
                }
            }

            if (!tableExists) {
                String createTableSQL = "CREATE TABLE Cables (Id AUTOINCREMENT PRIMARY KEY, Cable_Type VARCHAR(255), Count INTEGER, Location VARCHAR(255), Parent_Location VARCHAR(255))";
                try (PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
                    stmt.executeUpdate();
                    LOGGER.log(Level.INFO, "Created Cables table");
                }
            } else {
                LOGGER.log(Level.INFO, "Cables table already exists");
            }

            // Check if idx_cable_type_location index exists
            boolean indexExists = false;
            try (ResultSet rs = metaData.getIndexInfo(null, null, "Cables", false, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    LOGGER.log(Level.FINE, "Found index: {0} on table: {1}", new Object[]{indexName, rs.getString("TABLE_NAME")});
                    if (indexName != null && indexName.equalsIgnoreCase("idx_cable_type_location")) {
                        indexExists = true;
                        LOGGER.log(Level.INFO, "Index idx_cable_type_location already exists on Cables table");
                        break;
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error checking index info: {0}", e.getMessage());
            }

            if (!indexExists) {
                try {
                    String createIndexSQL = "CREATE INDEX idx_cable_type_location ON Cables (Cable_Type, Location)";
                    try (PreparedStatement stmt = conn.prepareStatement(createIndexSQL)) {
                        stmt.executeUpdate();
                        LOGGER.log(Level.INFO, "Created index idx_cable_type_location on Cables table");
                    }
                } catch (SQLException e) {
                    if (e.getMessage().contains("duplicate index name")) {
                        LOGGER.log(Level.INFO, "Index idx_cable_type_location already exists, skipping creation");
                    } else {
                        LOGGER.log(Level.SEVERE, "Error creating index: {0}", e.getMessage());
                        throw e;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error ensuring schema: {0}", e.getMessage());
            throw e;
        }
    }

    public static boolean cableExistsAtLocation(String cableType, String location) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Cables WHERE Cable_Type = ? AND Location = ?";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cableType);
            stmt.setString(2, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public static void addCable(String cableType, int quantity, String location, String parentLocation) throws SQLException {
        String sql = "INSERT INTO Cables (Cable_Type, Count, Location, Parent_Location) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cableType);
            stmt.setInt(2, quantity);
            stmt.setString(3, location);
            stmt.setString(4, parentLocation);
            stmt.executeUpdate();
        }
    }

    public static int getCableId(String cableType, String location) throws SQLException {
        String sql = "SELECT Id FROM Cables WHERE Cable_Type = ? AND Location = ?";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cableType);
            stmt.setString(2, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Id");
                }
            }
        }
        return -1;
    }

    public static void updateCount(int cableId, int countDelta) throws SQLException {
        String sql = "UPDATE Cables SET Count = Count + ? WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, countDelta);
            stmt.setInt(2, cableId);
            stmt.executeUpdate();
        }
    }

    public static boolean canRemoveCable(int cableId) throws SQLException {
        String sql = "SELECT Count FROM Cables WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cableId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Count") > 0;
                }
            }
        }
        return false;
    }

    public static void deleteCable(int cableId) throws SQLException {
        String sql = "DELETE FROM Cables WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cableId);
            stmt.executeUpdate();
        }
    }

    public static void deleteLocation(String location) throws SQLException {
        String sql = "UPDATE Cables SET Location = ?, Parent_Location = NULL WHERE Location = ?";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "Unassigned");
            stmt.setString(2, location);
            stmt.executeUpdate();
        }
    }

    public static List<String> getSubLocations(String parentLocation) throws SQLException {
        List<String> subLocations = new ArrayList<>();
        String sql = parentLocation == null
                ? "SELECT DISTINCT Location FROM Cables WHERE Parent_Location IS NULL AND Location != ?"
                : "SELECT DISTINCT Location FROM Cables WHERE Parent_Location = ?";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, parentLocation == null ? "Unassigned" : parentLocation);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String location = rs.getString("Location");
                    if (location != null && !location.isEmpty() && !location.startsWith("Placeholder_")) {
                        subLocations.add(location);
                    }
                }
            }
        }
        return subLocations;
    }

    public static List<String> getAllLocations() throws SQLException {
        List<String> locations = new ArrayList<>();
        String sql = "SELECT DISTINCT Location FROM Cables WHERE Cable_Type NOT LIKE 'Placeholder_%'";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String location = rs.getString("Location");
                    if (location != null && !location.isEmpty()) {
                        locations.add(location);
                    }
                }
            }
        }
        return locations;
    }

    public static void moveCable(int cableId, String newLocation, int quantity) throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Get current cable details
                String selectSQL = "SELECT Cable_Type, Count FROM Cables WHERE Id = ?";
                String cableType = null;
                int currentCount = 0;
                try (PreparedStatement stmt = conn.prepareStatement(selectSQL)) {
                    stmt.setInt(1, cableId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            cableType = rs.getString("Cable_Type");
                            currentCount = rs.getInt("Count");
                        } else {
                            throw new SQLException("Cable with ID " + cableId + " not found");
                        }
                    }
                }

                // Validate quantity
                if (quantity <= 0 || quantity > currentCount) {
                    throw new SQLException("Invalid quantity: " + quantity + ". Must be positive and not exceed current count: " + currentCount);
                }

                // Calculate new parent location for the new location
                String newParentLocation = newLocation.contains(LogCablesTab.getPathSeparator())
                        ? newLocation.substring(0, newLocation.lastIndexOf(LogCablesTab.getPathSeparator()))
                        : null;

                // Check if cable exists at the new location
                int existingCableId = getCableId(cableType, newLocation);
                if (existingCableId != -1) {
                    // Update existing cable at new location
                    updateCount(existingCableId, quantity);
                } else {
                    // Add new cable entry at new location
                    addCable(cableType, quantity, newLocation, newParentLocation);
                }

                // Update or delete source cable
                if (quantity == currentCount) {
                    deleteCable(cableId);
                } else {
                    updateCount(cableId, -quantity);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static List<CableEntry> getCablesByLocation(String location) throws SQLException {
        List<CableEntry> cables = new ArrayList<>();
        String sql = "SELECT Cable_Type, Count, Parent_Location FROM Cables WHERE Location = ?";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cables.add(new CableEntry(
                            rs.getString("Cable_Type"),
                            rs.getInt("Count"),
                            rs.getString("Parent_Location")
                    ));
                }
            }
        }
        return cables;
    }

    public static List<CableEntry> getCablesByParentLocation(String parentLocation) throws SQLException {
        List<CableEntry> cables = new ArrayList<>();
        String sql = "SELECT Cable_Type, SUM(Count) as TotalCount FROM Cables WHERE Parent_Location = ? OR Location = ? GROUP BY Cable_Type";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, parentLocation);
            stmt.setString(2, parentLocation);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cables.add(new CableEntry(
                            rs.getString("Cable_Type"),
                            rs.getInt("TotalCount"),
                            null
                    ));
                }
            }
        }
        return cables;
    }

    public static boolean locationExists(String location) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Cables WHERE Location = ?";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public static void createLocation(String fullPath, String parentLocation) throws SQLException {
        // Insert a placeholder to ensure the location exists in the database
        String sql = "INSERT INTO Cables (Cable_Type, Count, Location, Parent_Location) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "Placeholder_" + fullPath);
            stmt.setInt(2, 0);
            stmt.setString(3, fullPath);
            stmt.setString(4, parentLocation);
            stmt.executeUpdate();
        }
    }
}