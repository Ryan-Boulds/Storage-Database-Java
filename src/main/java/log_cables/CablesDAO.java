package log_cables;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
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
                String createTableSQL = "CREATE TABLE Cables (Id AUTOINCREMENT PRIMARY KEY, Cable_Type VARCHAR(255), Count INTEGER, Location VARCHAR(255), UNIQUE(Cable_Type, Location))";
                try (PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
                    stmt.executeUpdate();
                    LOGGER.log(Level.INFO, "Created Cables table");
                }
            } else {
                LOGGER.log(Level.INFO, "Cables table already exists");
                try (ResultSet rs = metaData.getColumns(null, null, "Cables", "Parent_Location")) {
                    if (rs.next()) {
                        String alterTableSQL = "ALTER TABLE Cables DROP COLUMN Parent_Location";
                        try (PreparedStatement stmt = conn.prepareStatement(alterTableSQL)) {
                            stmt.executeUpdate();
                            LOGGER.log(Level.INFO, "Dropped Parent_Location column from Cables table");
                        }
                    }
                }
            }
        }
    }

    public static int getCableId(String cableType, String location) throws SQLException {
        String sql = "SELECT Id FROM Cables WHERE Cable_Type = ? AND Location = ? AND Count > 0 LIMIT 1";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
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

    public static void addCable(String cableType, int count, String location) throws SQLException {
        String selectSql = "SELECT Id FROM Cables WHERE Cable_Type = ? AND Location = ?";
        String updateSql = "UPDATE Cables SET Count = Count + ? WHERE Id = ?";
        String insertSql = "INSERT INTO Cables (Cable_Type, Count, Location) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseUtils.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int existingId = -1;
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, cableType);
                    selectStmt.setString(2, location);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            existingId = rs.getInt("Id");
                        }
                    }
                }

                if (existingId != -1) {
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, count);
                        updateStmt.setInt(2, existingId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, cableType);
                        insertStmt.setInt(2, count);
                        insertStmt.setString(3, location);
                        insertStmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void updateCount(int id, int delta) throws SQLException {
        String sql = "UPDATE Cables SET Count = Count + ? WHERE Id = ? AND Count + ? >= 0";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, id);
            stmt.setInt(3, delta);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No rows updated or count would go negative");
            }
        }
    }

    public static boolean canRemoveCable(int id) throws SQLException {
        String sql = "SELECT Count FROM Cables WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Count") > 0;
                }
            }
        }
        return false;
    }

    public static void moveCable(int cableId, String newLocation, int quantity) throws SQLException {
        String sqlSelect = "SELECT Cable_Type, Count FROM Cables WHERE Id = ?";
        String sqlSelectTarget = "SELECT Id, Count FROM Cables WHERE Cable_Type = ? AND Location = ?";
        String sqlUpdate = "UPDATE Cables SET Count = ? WHERE Id = ?";
        String sqlInsert = "INSERT INTO Cables (Cable_Type, Count, Location) VALUES (?, ?, ?)";
        String sqlDelete = "DELETE FROM Cables WHERE Id = ? AND Count = 0";

        try (Connection conn = DatabaseUtils.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtSelect = conn.prepareStatement(sqlSelect)) {
                stmtSelect.setInt(1, cableId);
                try (ResultSet rs = stmtSelect.executeQuery()) {
                    if (rs.next()) {
                        String cableType = rs.getString("Cable_Type");
                        int currentCount = rs.getInt("Count");

                        if (quantity > currentCount) {
                            throw new SQLException("Quantity exceeds available count");
                        }

                        // Update source count
                        int newSourceCount = currentCount - quantity;
                        try (PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate)) {
                            stmtUpdate.setInt(1, newSourceCount);
                            stmtUpdate.setInt(2, cableId);
                            stmtUpdate.executeUpdate();
                        }

                        // Find or create target
                        int targetId = -1;
                        int targetCount = 0;
                        try (PreparedStatement stmtSelectTarget = conn.prepareStatement(sqlSelectTarget)) {
                            stmtSelectTarget.setString(1, cableType);
                            stmtSelectTarget.setString(2, newLocation);
                            try (ResultSet rsTarget = stmtSelectTarget.executeQuery()) {
                                if (rsTarget.next()) {
                                    targetId = rsTarget.getInt("Id");
                                    targetCount = rsTarget.getInt("Count");
                                }
                            }
                        }

                        if (targetId != -1) {
                            try (PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate)) {
                                stmtUpdate.setInt(1, targetCount + quantity);
                                stmtUpdate.setInt(2, targetId);
                                stmtUpdate.executeUpdate();
                            }
                        } else {
                            try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert)) {
                                stmtInsert.setString(1, cableType);
                                stmtInsert.setInt(2, quantity);
                                stmtInsert.setString(3, newLocation);
                                stmtInsert.executeUpdate();
                            }
                        }

                        // Delete if source count is 0
                        if (newSourceCount == 0) {
                            try (PreparedStatement stmtDelete = conn.prepareStatement(sqlDelete)) {
                                stmtDelete.setInt(1, cableId);
                                stmtDelete.executeUpdate();
                            }
                        }
                        conn.commit();
                    } else {
                        throw new SQLException("Cable ID not found");
                    }
                }
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void deleteCable(int id) throws SQLException {
        String sql = "DELETE FROM Cables WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public static void deleteLocation(String location) throws SQLException {
        String sql = "UPDATE Cables SET Location = ? WHERE Location = ? OR Location LIKE ? || ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LogCablesTab.getUnassignedLocation());
            stmt.setString(2, location);
            stmt.setString(3, location);
            stmt.setString(4, LogCablesTab.getPathSeparator() + "%");
            stmt.executeUpdate();
        }
    }

    public static List<String> getSubLocations(String parentLocation) throws SQLException {
        List<String> locations = new ArrayList<>();
        String sql = "SELECT DISTINCT Location FROM Cables WHERE Location IS NOT NULL";
        if (parentLocation != null) {
            sql += " AND Location LIKE ? ESCAPE '\\'";
        } else {
            sql += " AND (Location NOT LIKE ? OR Location = ?)";
        }
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (parentLocation != null) {
                stmt.setString(1, parentLocation + LogCablesTab.getPathSeparator() + "%");
            } else {
                stmt.setString(1, "%" + LogCablesTab.getPathSeparator() + "%");
                stmt.setString(2, LogCablesTab.getUnassignedLocation());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String location = rs.getString("Location");
                    if (parentLocation == null) {
                        if (!location.contains(LogCablesTab.getPathSeparator()) || location.equals(LogCablesTab.getUnassignedLocation())) {
                            locations.add(location);
                        }
                    } else {
                        locations.add(location);
                    }
                }
            }
        }
        return locations;
    }

    public static List<CableEntry> getCablesByLocation(String location) throws SQLException {
        List<CableEntry> cables = new ArrayList<>();
        String sql = "SELECT Cable_Type, Count, Location FROM Cables WHERE Cable_Type NOT LIKE 'Placeholder_%' AND Location = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cables.add(new CableEntry(
                            rs.getString("Cable_Type"),
                            rs.getInt("Count"),
                            null
                    ));
                }
            }
        }
        return cables;
    }

    public static List<CableEntry> getCablesByParentLocation(String parentLocation) throws SQLException {
        List<CableEntry> cables = new ArrayList<>();
        String sql = "SELECT Cable_Type, SUM(Count) as TotalCount FROM Cables WHERE Cable_Type NOT LIKE 'Placeholder_%' AND (Location = ? OR Location LIKE ? || ?) GROUP BY Cable_Type";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, parentLocation);
            stmt.setString(2, parentLocation);
            stmt.setString(3, LogCablesTab.getPathSeparator() + "%");
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
        String sql = "SELECT COUNT(*) FROM Cables WHERE Location = ? AND Cable_Type NOT LIKE 'Placeholder_%'";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        String sql = "INSERT INTO Cables (Cable_Type, Count, Location) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "Placeholder_" + fullPath);
            stmt.setInt(2, 0);
            stmt.setString(3, fullPath);
            stmt.executeUpdate();
        }
    }

    public static List<String> getAllLocations() throws SQLException {
        List<String> locations = new ArrayList<>();
        String sql = "SELECT DISTINCT Location FROM Cables WHERE Location IS NOT NULL";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    locations.add(rs.getString("Location"));
                }
            }
        }
        return locations;
    }

    public static List<String> getUniqueCableTypes() throws SQLException {
        List<String> cableTypes = new ArrayList<>();
        String sql = "SELECT DISTINCT Cable_Type FROM Cables WHERE Cable_Type NOT LIKE 'Placeholder_%'";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cableTypes.add(rs.getString("Cable_Type"));
                }
            }
        }
        Collections.sort(cableTypes);
        return cableTypes;
    }
}