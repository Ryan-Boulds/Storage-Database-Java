package log_cables;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import utils.DatabaseUtils;

public class CablesDAO {
    private static final Logger LOGGER = Logger.getLogger(CablesDAO.class.getName());
    private static final String DATATYPE_CABLE = "Cable";

    public static class CableEntry {
        public final String cableType;
        public final int count;

        public CableEntry(String cableType, int count) {
            this.cableType = cableType;
            this.count = count;
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
                String createTableSQL = "CREATE TABLE Cables (Id COUNTER PRIMARY KEY, Cable_Type VARCHAR(255), Count INTEGER, Location VARCHAR(255), UNIQUE(Cable_Type, Location))";
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
                // No need to check or add Previous_Location
            }

            String selectOldPlaceholders = "SELECT DISTINCT Location FROM Cables WHERE Cable_Type LIKE 'Placeholder_%'";
            try (PreparedStatement selStmt = conn.prepareStatement(selectOldPlaceholders);
                 PreparedStatement insStmt = conn.prepareStatement("INSERT INTO Locations (Datatype, Location) VALUES (?, ?)");
                 PreparedStatement delStmt = conn.prepareStatement("DELETE FROM Cables WHERE Cable_Type LIKE 'Placeholder_%' AND Location = ?")) {
                try (ResultSet rs = selStmt.executeQuery()) {
                    while (rs.next()) {
                        String loc = rs.getString("Location");
                        insStmt.setString(1, DATATYPE_CABLE);
                        insStmt.setString(2, loc);
                        insStmt.executeUpdate();
                        delStmt.setString(1, loc);
                        delStmt.executeUpdate();
                        LOGGER.log(Level.INFO, "Migrated placeholder location: {0}", loc);
                    }
                }
            }

            try (ResultSet rs = metaData.getIndexInfo(null, null, "Cables", false, false)) {
                boolean indexExists = false;
                while (rs.next()) {
                    if ("idx_cables_location".equals(rs.getString("INDEX_NAME"))) {
                        indexExists = true;
                        break;
                    }
                }
                if (!indexExists) {
                    try {
                        String createIndexSQL = "CREATE INDEX idx_cables_location ON Cables(Location)";
                        try (PreparedStatement stmt = conn.prepareStatement(createIndexSQL)) {
                            stmt.executeUpdate();
                            LOGGER.log(Level.INFO, "Created index idx_cables_location on Cables table");
                        }
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Failed to create index idx_cables_location: {0}", e.getMessage());
                    }
                }
            }
        }
    }

    private static boolean locationExistsInLocations(String location) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Locations WHERE Datatype = ? AND Location = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DATATYPE_CABLE);
            stmt.setString(2, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private static boolean locationExistsInCables(String location) throws SQLException {
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

    public static boolean locationExists(String location) throws SQLException {
        if (location == null || location.isEmpty()) return false;
        return locationExistsInLocations(location) || locationExistsInCables(location);
    }

    public static List<String> getSubLocations(String parentLocation) throws SQLException {
        Set<String> subLocations = new HashSet<>();
        String locSql;
        if (parentLocation == null) {
            locSql = "SELECT Location FROM Locations WHERE Datatype = ? AND Location NOT LIKE '%/%'";
        } else {
            locSql = "SELECT Location FROM Locations WHERE Datatype = ? AND Location LIKE ? || '/%'";
        }
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(locSql)) {
            stmt.setString(1, DATATYPE_CABLE);
            if (parentLocation != null) {
                stmt.setString(2, parentLocation);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    subLocations.add(rs.getString("Location"));
                }
            }
        }

        String cableSql;
        if (parentLocation == null) {
            cableSql = "SELECT DISTINCT Location FROM Cables WHERE Location NOT LIKE '%/%' AND Location IS NOT NULL AND Cable_Type NOT LIKE 'Placeholder_%'";
        } else {
            cableSql = "SELECT DISTINCT Location FROM Cables WHERE Location LIKE ? || '/%' AND Location IS NOT NULL AND Cable_Type NOT LIKE 'Placeholder_%'";
        }
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(cableSql)) {
            if (parentLocation != null) {
                stmt.setString(1, parentLocation);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    subLocations.add(rs.getString("Location"));
                }
            }
        }

        List<String> result = new ArrayList<>(subLocations);
        Collections.sort(result);
        return result;
    }

    public static void createLocation(String fullPath, String parentLocation) throws SQLException {
        if (locationExists(fullPath)) {
            LOGGER.log(Level.INFO, "Location already exists: {0}", fullPath);
            return;
        }
        String sql = "INSERT INTO Locations (Datatype, Location) VALUES (?, ?)";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DATATYPE_CABLE);
            stmt.setString(2, fullPath);
            stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Created location in Locations table: {0}", fullPath);
        }
    }

    public static List<String> getAllLocations() throws SQLException {
        Set<String> locations = new HashSet<>();
        String sql = "SELECT Location FROM Locations WHERE Datatype = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DATATYPE_CABLE);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    locations.add(rs.getString("Location"));
                }
            }
        }

        sql = "SELECT DISTINCT Location FROM Cables WHERE Location IS NOT NULL AND Cable_Type NOT LIKE 'Placeholder_%'";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    locations.add(rs.getString("Location"));
                }
            }
        }

        List<String> result = new ArrayList<>(locations);
        Collections.sort(result);
        return result;
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

    public static int getCableId(String cableType, String location) throws SQLException {
        String sql = "SELECT Id FROM Cables WHERE Cable_Type = ? AND Location = ? AND Count > 0";
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
            int cableId = -1;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, cableType);
                selectStmt.setString(2, location);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        cableId = rs.getInt("Id");
                    }
                }
            }

            if (cableId != -1) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, count);
                    updateStmt.setInt(2, cableId);
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
        }
    }

    public static void updateCount(int cableId, int delta) throws SQLException {
        String selectSql = "SELECT Count, Cable_Type, Location FROM Cables WHERE Id = ?";
        String updateSql = "UPDATE Cables SET Count = ? WHERE Id = ?";
        String deleteSql = "DELETE FROM Cables WHERE Id = ?";

        try (Connection conn = DatabaseUtils.getConnection()) {
            int currentCount;
            String cableType = null;
            String location = null;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, cableId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        currentCount = rs.getInt("Count");
                        cableType = rs.getString("Cable_Type");
                        location = rs.getString("Location");
                    } else {
                        throw new SQLException("Cable ID not found: " + cableId);
                    }
                }
            }

            int newCount = currentCount + delta;
            if (newCount < 0) {
                throw new SQLException("Cannot reduce count below 0 for cable ID: " + cableId);
            }

            if (newCount == 0) {
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, cableId);
                    deleteStmt.executeUpdate();
                    LOGGER.log(Level.INFO, "Deleted cable with ID {0} due to zero count", cableId);
                }
            } else {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, newCount);
                    updateStmt.setInt(2, cableId);
                    updateStmt.executeUpdate();
                }
            }
            LOGGER.log(Level.INFO, "Updated count for cable ID {0} ({1} at {2}) by {3}, new count: {4}",
                    new Object[]{cableId, cableType, location, delta, newCount});
        }
    }

    public static boolean canRemoveCable(int cableId) throws SQLException {
        String sql = "SELECT Count FROM Cables WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cableId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Count") > 0;
                }
            }
        }
        return false;
    }

    public static List<CableEntry> getCablesByLocation(String location) throws SQLException {
        List<CableEntry> cables = new ArrayList<>();
        String sql = "SELECT Cable_Type, Count FROM Cables WHERE Location = ? AND Count > 0 AND Cable_Type NOT LIKE 'Placeholder_%'";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cables.add(new CableEntry(
                            rs.getString("Cable_Type"),
                            rs.getInt("Count")
                    ));
                }
            }
        }
        return cables;
    }

    public static List<CableEntry> getCablesSummary(String parentLocation) throws SQLException {
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
                            rs.getInt("TotalCount")
                    ));
                }
            }
        }
        return cables;
    }

    public static void deleteCable(int cableId) throws SQLException {
        String sql = "DELETE FROM Cables WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cableId);
            stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Deleted cable with ID {0}", cableId);
        }
    }

    public static void deleteLocation(String fullPath) throws SQLException {
        String parentPath = getParentPath(fullPath);
        String targetLocation = (parentPath != null) ? parentPath : LogCablesTab.getUnassignedLocation();

        String updateSql = "UPDATE Cables SET Location = ? WHERE Location = ? OR Location LIKE ? || ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, targetLocation);
            stmt.setString(2, fullPath);
            stmt.setString(3, fullPath);
            stmt.setString(4, LogCablesTab.getPathSeparator() + "%");
            int updated = stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Moved {0} cables to {1}", new Object[]{updated, targetLocation});
        }

        String deleteSql = "DELETE FROM Locations WHERE Datatype = ? AND Location = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, DATATYPE_CABLE);
            stmt.setString(2, fullPath);
            stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Deleted location from Locations table: {0}", fullPath);
        }
    }

    public static void moveCable(int cableId, String targetLocation, int countToMove) throws SQLException {
        if (countToMove <= 0) {
            throw new SQLException("Count to move must be positive");
        }
        if (!locationExists(targetLocation)) {
            throw new SQLException("Target location does not exist: " + targetLocation);
        }

        String selectSql = "SELECT Cable_Type, Count, Location FROM Cables WHERE Id = ?";
        String updateSourceSql = "UPDATE Cables SET Count = Count - ? WHERE Id = ?";
        String selectTargetSql = "SELECT Id FROM Cables WHERE Cable_Type = ? AND Location = ?";
        String updateTargetSql = "UPDATE Cables SET Count = Count + ? WHERE Id = ?";
        String insertTargetSql = "INSERT INTO Cables (Cable_Type, Count, Location) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseUtils.getConnection()) {
            String cableType = null;
            int currentCount = 0;
            String sourceLocation = null;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, cableId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        cableType = rs.getString("Cable_Type");
                        currentCount = rs.getInt("Count");
                        sourceLocation = rs.getString("Location");
                    } else {
                        throw new SQLException("Cable ID not found: " + cableId);
                    }
                }
            }

            if (currentCount < countToMove) {
                throw new SQLException("Not enough cables to move: " + currentCount + " available, " + countToMove + " requested");
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSourceSql)) {
                updateStmt.setInt(1, countToMove);
                updateStmt.setInt(2, cableId);
                updateStmt.executeUpdate();
            }

            int targetCableId = -1;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectTargetSql)) {
                selectStmt.setString(1, cableType);
                selectStmt.setString(2, targetLocation);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        targetCableId = rs.getInt("Id");
                    }
                }
            }

            if (targetCableId != -1) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateTargetSql)) {
                    updateStmt.setInt(1, countToMove);
                    updateStmt.setInt(2, targetCableId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertTargetSql)) {
                    insertStmt.setString(1, cableType);
                    insertStmt.setInt(2, countToMove);
                    insertStmt.setString(3, targetLocation);
                    insertStmt.executeUpdate();
                }
            }

            if (currentCount == countToMove) {
                deleteCable(cableId);
            }

            LOGGER.log(Level.INFO, "Moved {0} cables of type {1} from {2} to {3}", new Object[]{countToMove, cableType, sourceLocation, targetLocation});
        }
    }

    private static String getParentPath(String fullPath) {
        if (fullPath == null || fullPath.equals(LogCablesTab.getUnassignedLocation())) {
            return null;
        }
        int idx = fullPath.lastIndexOf(LogCablesTab.getPathSeparator());
        if (idx == -1) {
            return null;
        }
        return fullPath.substring(0, idx);
    }

    public static void cleanUpUnassignedSublocations() throws SQLException {
        String sql = "UPDATE Cables SET Location = ? WHERE Location LIKE ? || ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LogCablesTab.getUnassignedLocation());
            stmt.setString(2, "%");
            stmt.setString(3, LogCablesTab.getPathSeparator() + LogCablesTab.getUnassignedLocation());
            stmt.executeUpdate();
        }
    }
}