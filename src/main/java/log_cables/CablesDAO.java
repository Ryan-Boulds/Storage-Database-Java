package log_cables;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import utils.DatabaseUtils;

public final class CablesDAO {

    private static final Logger LOGGER = Logger.getLogger(CablesDAO.class.getName());
    private static final String TABLE_NAME = "Cables";

    public static class CableEntry {

        public final int id;
        public final String cableType;
        public final int count;
        public final String location;
        public final String previousLocation;

        public CableEntry(int id, String cableType, int count, String location, String previousLocation) {
            this.id = id;
            this.cableType = cableType;
            this.count = count;
            this.location = location;
            this.previousLocation = previousLocation;
        }
    }

    public static void ensureSchema() {
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, TABLE_NAME, null)) {
                if (!rs.next()) {
                    // Table doesn't exist, create it
                    try (Statement stmt = conn.createStatement()) {
                        String sql = "CREATE TABLE " + TABLE_NAME + " (ID AUTOINCREMENT PRIMARY KEY, Cable_Type VARCHAR(255), [Count] INTEGER, Location VARCHAR(255), Previous_Location VARCHAR(255))";
                        stmt.executeUpdate(sql);
                        // Check if index exists
                        boolean indexExists = false;
                        try (ResultSet indexes = meta.getIndexInfo(null, null, TABLE_NAME, false, false)) {
                            while (indexes.next()) {
                                String indexName = indexes.getString("INDEX_NAME");
                                if ("idx_cable_type_location".equals(indexName)) {
                                    indexExists = true;
                                    break;
                                }
                            }
                        }
                        if (!indexExists) {
                            stmt.executeUpdate("CREATE INDEX idx_cable_type_location ON " + TABLE_NAME + " (Cable_Type, Location)");
                            LOGGER.log(Level.INFO, "Created Cables table with index idx_cable_type_location");
                        } else {
                            LOGGER.log(Level.INFO, "Created Cables table, index idx_cable_type_location already exists");
                        }
                    }
                } else {
                    // Table exists, check for ID column (new schema)
                    try (ResultSet columns = meta.getColumns(null, null, TABLE_NAME, "ID")) {
                        if (!columns.next()) {
                            // Old schema detected, migrate to new schema
                            try (Statement stmt = conn.createStatement()) {
                                // Create temporary table
                                stmt.executeUpdate(
                                        "CREATE TABLE TempCables (ID AUTOINCREMENT PRIMARY KEY, Cable_Type VARCHAR(255), [Count] INTEGER, Location VARCHAR(255), Previous_Location VARCHAR(255))"
                                );
                                // Migrate data
                                stmt.executeUpdate(
                                        "INSERT INTO TempCables (Cable_Type, [Count], Location, Previous_Location) "
                                        + "SELECT Cable_Type, [Count], 'Unassigned', NULL FROM " + TABLE_NAME
                                );
                                // Drop old table
                                stmt.executeUpdate("DROP TABLE " + TABLE_NAME);
                                // Rename temporary table
                                stmt.executeUpdate("ALTER TABLE TempCables RENAME TO " + TABLE_NAME);
                                // Check if index exists
                                boolean indexExists = false;
                                try (ResultSet indexes = meta.getIndexInfo(null, null, TABLE_NAME, false, false)) {
                                    while (indexes.next()) {
                                        String indexName = indexes.getString("INDEX_NAME");
                                        if ("idx_cable_type_location".equals(indexName)) {
                                            indexExists = true;
                                            break;
                                        }
                                    }
                                }
                                if (!indexExists) {
                                    stmt.executeUpdate("CREATE INDEX idx_cable_type_location ON " + TABLE_NAME + " (Cable_Type, Location)");
                                    LOGGER.log(Level.INFO, "Migrated Cables table to new schema with index idx_cable_type_location");
                                } else {
                                    LOGGER.log(Level.INFO, "Migrated Cables table to new schema, index idx_cable_type_location already exists");
                                }
                            }
                        } else {
                            // New schema exists, ensure index
                            boolean indexExists = false;
                            try (ResultSet indexes = meta.getIndexInfo(null, null, TABLE_NAME, false, false)) {
                                while (indexes.next()) {
                                    String indexName = indexes.getString("INDEX_NAME");
                                    if ("idx_cable_type_location".equals(indexName)) {
                                        indexExists = true;
                                        break;
                                    }
                                }
                            }
                            if (!indexExists) {
                                try (Statement stmt = conn.createStatement()) {
                                    stmt.executeUpdate("CREATE INDEX idx_cable_type_location ON " + TABLE_NAME + " (Cable_Type, Location)");
                                    LOGGER.log(Level.INFO, "Added missing index idx_cable_type_location to existing Cables table");
                                }
                            } else {
                                LOGGER.log(Level.INFO, "Cables table schema already up to date with index idx_cable_type_location");
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error ensuring schema: {0}", e.getMessage());
            throw new RuntimeException("Failed to ensure schema", e);
        }
    }

    public static void addCable(String cableType, int count, String location) throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TABLE_NAME + " (Cable_Type, [Count], Location, Previous_Location) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, cableType);
                ps.setInt(2, count);
                ps.setString(3, location);
                ps.setString(4, null);
                ps.executeUpdate();
                conn.commit();
                LOGGER.log(Level.INFO, "Added {0} {1} cables at {2}", new Object[]{count, cableType, location});
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Error adding cable: {0}", e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static boolean cableExistsAtLocation(String cableType, String location) throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT ID FROM " + TABLE_NAME + " WHERE Cable_Type = ? AND Location = ?")) {
            ps.setString(1, cableType);
            ps.setString(2, location);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static void createLocation(String location) throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TABLE_NAME + " (Cable_Type, [Count], Location, Previous_Location) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, "Placeholder_" + location);
                ps.setInt(2, 0);
                ps.setString(3, location);
                ps.setString(4, null);
                ps.executeUpdate();
                conn.commit();
                LOGGER.log(Level.INFO, "Created new location: {0}", location);
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Error creating location: {0}", e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static boolean locationExists(String location) throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM " + TABLE_NAME + " WHERE Location = ?")) {
            ps.setString(1, location);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static int getCableId(String cableType, String location) throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT ID FROM " + TABLE_NAME + " WHERE Cable_Type = ? AND Location = ?")) {
            ps.setString(1, cableType);
            ps.setString(2, location);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ID");
                }
                return -1;
            }
        }
    }

    public static boolean canRemoveCable(int id) throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT [Count] FROM " + TABLE_NAME + " WHERE ID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Count") > 0;
                }
                return false;
            }
        }
    }

    public static void updateCount(int id, int delta) throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE " + TABLE_NAME + " SET [Count] = [Count] + ? WHERE ID = ?")) {
                ps.setInt(1, delta);
                ps.setInt(2, id);
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("No cable found with ID: " + id);
                }
                conn.commit();
                LOGGER.log(Level.INFO, "Updated cable count by {0} for ID {1}", new Object[]{delta, id});
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Error updating cable count: {0}", e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void deleteCable(int id) throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM " + TABLE_NAME + " WHERE ID = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                conn.commit();
                LOGGER.log(Level.INFO, "Deleted cable with ID {0}", id);
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Error deleting cable: {0}", e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void moveCable(int sourceId, String targetLocation, int quantity) throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Get source details
                int currentCount;
                String cableType;
                String sourceLocation;
                try (PreparedStatement psSource = conn.prepareStatement(
                        "SELECT Cable_Type, [Count], Location FROM " + TABLE_NAME + " WHERE ID = ?")) {
                    psSource.setInt(1, sourceId);
                    try (ResultSet rsSource = psSource.executeQuery()) {
                        if (!rsSource.next()) {
                            throw new SQLException("Source cable not found with ID: " + sourceId);
                        }
                        cableType = rsSource.getString("Cable_Type");
                        currentCount = rsSource.getInt("Count");
                        sourceLocation = rsSource.getString("Location");
                    }
                }
                if (quantity > currentCount) {
                    throw new SQLException("Cannot move " + quantity + " cables; only " + currentCount + " available");
                }
                boolean transferAll = (quantity == currentCount);

                // Check if target exists
                boolean targetExists = false;
                int targetId = -1;
                int targetCount = 0;
                String targetPrev = null;
                String newPrevLoc = "previously in: " + sourceLocation;
                try (PreparedStatement psCheckTarget = conn.prepareStatement(
                        "SELECT ID, [Count], Previous_Location FROM " + TABLE_NAME + " WHERE Cable_Type = ? AND Location = ? AND Cable_Type NOT LIKE '%Placeholder'")) {
                    psCheckTarget.setString(1, cableType);
                    psCheckTarget.setString(2, targetLocation);
                    try (ResultSet rsTarget = psCheckTarget.executeQuery()) {
                        if (rsTarget.next()) {
                            targetExists = true;
                            targetId = rsTarget.getInt("ID");
                            targetCount = rsTarget.getInt("Count");
                            targetPrev = rsTarget.getString("Previous_Location");
                        }
                    }
                }
                if (targetExists) {
                    // If target is Unassigned, update Previous_Location; otherwise, keep existing
                    String updatePrev = targetLocation.equals("Unassigned") ? newPrevLoc : targetPrev;
                    try (PreparedStatement psUpdateTarget = conn.prepareStatement(
                            "UPDATE " + TABLE_NAME + " SET [Count] = ?, Previous_Location = ? WHERE ID = ?")) {
                        psUpdateTarget.setInt(1, targetCount + quantity);
                        psUpdateTarget.setString(2, updatePrev);
                        psUpdateTarget.setInt(3, targetId);
                        psUpdateTarget.executeUpdate();
                    }
                } else {
                    try (PreparedStatement psInsert = conn.prepareStatement(
                            "INSERT INTO " + TABLE_NAME + " (Cable_Type, [Count], Location, Previous_Location) VALUES (?, ?, ?, ?)")) {
                        psInsert.setString(1, cableType);
                        psInsert.setInt(2, quantity);
                        psInsert.setString(3, targetLocation);
                        psInsert.setString(4, newPrevLoc);
                        psInsert.executeUpdate();
                    }
                }

                // Handle source: set to 0 if transferring all, else update count
                try (PreparedStatement psUpdateSource = conn.prepareStatement(
                        "UPDATE " + TABLE_NAME + " SET [Count] = ? WHERE ID = ?")) {
                    psUpdateSource.setInt(1, transferAll ? 0 : currentCount - quantity);
                    psUpdateSource.setInt(2, sourceId);
                    psUpdateSource.executeUpdate();
                }

                conn.commit();
                LOGGER.log(Level.INFO, "Moved {0} of {1} from {2} to {3}", new Object[]{quantity, cableType, sourceLocation, targetLocation});
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Error moving cable: {0}", e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static List<CableEntry> getCablesByLocation(String location) throws SQLException {
        List<CableEntry> cables = new ArrayList<>();
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT ID, Cable_Type, [Count], Location, Previous_Location FROM " + TABLE_NAME + " WHERE Location = ? AND Cable_Type NOT LIKE '%Placeholder'")) {
            ps.setString(1, location);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cables.add(new CableEntry(
                            rs.getInt("ID"),
                            rs.getString("Cable_Type"),
                            rs.getInt("Count"),
                            rs.getString("Location"),
                            rs.getString("Previous_Location")
                    ));
                }
            }
        }
        return cables;
    }

    public static Set<String> getAllLocations() {
        Set<String> locations = new TreeSet<>();
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT Location FROM " + TABLE_NAME)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    locations.add(rs.getString("Location"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting locations: {0}", e.getMessage());
        }
        return locations;
    }

    public static void deleteLocation(String targetLocation) throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement selectPs = conn.prepareStatement(
                        "SELECT ID, Cable_Type, [Count], Previous_Location FROM " + TABLE_NAME + " WHERE Location = ?");
                selectPs.setString(1, targetLocation);
                try (ResultSet rs = selectPs.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("ID");
                        String cableType = rs.getString("Cable_Type");
                        int count = rs.getInt("Count");
                        String prevLoc = rs.getString("Previous_Location");
                        String newPrevLoc = prevLoc != null ? prevLoc : "previously in: " + targetLocation;

                        // If it's a placeholder, delete it
                        if (cableType.startsWith("Placeholder_")) {
                            deleteCable(id);
                            continue;
                        }

                        // Check if there's a matching cable type in Unassigned
                        PreparedStatement checkPs = conn.prepareStatement(
                                "SELECT ID, [Count], Previous_Location FROM " + TABLE_NAME + " WHERE Cable_Type = ? AND Location = ? AND Cable_Type NOT LIKE '%Placeholder'");
                        checkPs.setString(1, cableType);
                        checkPs.setString(2, "Unassigned");
                        try (ResultSet checkRs = checkPs.executeQuery()) {
                            if (checkRs.next()) {
                                // Matching cable type in Unassigned: update count and Previous_Location
                                int existingId = checkRs.getInt("ID");
                                int existingCount = checkRs.getInt("Count");
                                String existingPrevLoc = checkRs.getString("Previous_Location");
                                String updatePrevLoc = existingPrevLoc != null ? existingPrevLoc : newPrevLoc;
                                PreparedStatement updatePs = conn.prepareStatement(
                                        "UPDATE " + TABLE_NAME + " SET [Count] = ?, Previous_Location = ? WHERE ID = ?");
                                updatePs.setInt(1, existingCount + count);
                                updatePs.setString(2, updatePrevLoc);
                                updatePs.setInt(3, existingId);
                                updatePs.executeUpdate();
                                // Delete the original entry
                                deleteCable(id);
                            } else {
                                // No matching cable type in Unassigned: update location to Unassigned
                                PreparedStatement updatePs = conn.prepareStatement(
                                        "UPDATE " + TABLE_NAME + " SET Location = ?, Previous_Location = ? WHERE ID = ?");
                                updatePs.setString(1, "Unassigned");
                                updatePs.setString(2, newPrevLoc);
                                updatePs.setInt(3, id);
                                updatePs.executeUpdate();
                            }
                        }
                    }
                }
                conn.commit();
                LOGGER.log(Level.INFO, "Deleted location {0} and moved entries to Unassigned", targetLocation);
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Error deleting location: {0}", e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}