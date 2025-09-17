package accessories_count;

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

public class AccessoriesDAO {
    private static final Logger LOGGER = Logger.getLogger(AccessoriesDAO.class.getName());
    private static final String DATATYPE_ACCESSORY = "Accessory";

    public static class AccessoryEntry {
        public final String accessoryType;
        public final int count;

        public AccessoryEntry(String accessoryType, int count) {
            this.accessoryType = accessoryType;
            this.count = count;
        }
    }

    public static void ensureSchema() throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            boolean tableExists = false;
            try (ResultSet rs = metaData.getTables(null, null, "Accessories", new String[]{"TABLE"})) {
                if (rs.next()) {
                    tableExists = true;
                    LOGGER.log(Level.INFO, "Accessories table found: {0}", rs.getString("TABLE_NAME"));
                } else {
                    LOGGER.log(Level.INFO, "Accessories table does not exist");
                }
            }

            if (!tableExists) {
                String createTableSQL = "CREATE TABLE Accessories (Id COUNTER PRIMARY KEY, Accessory_Type VARCHAR(255), Count INTEGER, Location VARCHAR(255), UNIQUE(Accessory_Type, Location))";
                try (PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
                    stmt.executeUpdate();
                    LOGGER.log(Level.INFO, "Created Accessories table");
                }
            } else {
                LOGGER.log(Level.INFO, "Accessories table already exists");
                try (ResultSet rs = metaData.getColumns(null, null, "Accessories", "Parent_Location")) {
                    if (rs.next()) {
                        String alterTableSQL = "ALTER TABLE Accessories DROP COLUMN Parent_Location";
                        try (PreparedStatement stmt = conn.prepareStatement(alterTableSQL)) {
                            stmt.executeUpdate();
                            LOGGER.log(Level.INFO, "Dropped Parent_Location column from Accessories table");
                        }
                    }
                }
            }

            String selectOldPlaceholders = "SELECT DISTINCT Location FROM Accessories WHERE Accessory_Type LIKE 'Placeholder_%'";
            try (PreparedStatement selStmt = conn.prepareStatement(selectOldPlaceholders);
                 PreparedStatement insStmt = conn.prepareStatement("INSERT INTO Locations (Datatype, Location) VALUES (?, ?)");
                 PreparedStatement delStmt = conn.prepareStatement("DELETE FROM Accessories WHERE Accessory_Type LIKE 'Placeholder_%' AND Location = ?")) {
                try (ResultSet rs = selStmt.executeQuery()) {
                    while (rs.next()) {
                        String loc = rs.getString("Location");
                        insStmt.setString(1, DATATYPE_ACCESSORY);
                        insStmt.setString(2, loc);
                        insStmt.executeUpdate();
                        delStmt.setString(1, loc);
                        delStmt.executeUpdate();
                        LOGGER.log(Level.INFO, "Migrated placeholder location: {0}", loc);
                    }
                }
            }

            try (ResultSet rs = metaData.getIndexInfo(null, null, "Accessories", false, false)) {
                boolean indexExists = false;
                while (rs.next()) {
                    if ("idx_accessories_location".equals(rs.getString("INDEX_NAME"))) {
                        indexExists = true;
                        break;
                    }
                }
                if (!indexExists) {
                    try {
                        String createIndexSQL = "CREATE INDEX idx_accessories_location ON Accessories(Location)";
                        try (PreparedStatement stmt = conn.prepareStatement(createIndexSQL)) {
                            stmt.executeUpdate();
                            LOGGER.log(Level.INFO, "Created index idx_accessories_location on Accessories table");
                        }
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Failed to create index idx_accessories_location: {0}", e.getMessage());
                    }
                }
            }
        }
    }

    private static boolean locationExistsInLocations(String location) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Locations WHERE Datatype = ? AND Location = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DATATYPE_ACCESSORY);
            stmt.setString(2, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private static boolean locationExistsInAccessories(String location) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Accessories WHERE Location = ? AND Accessory_Type NOT LIKE 'Placeholder_%'";
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
        return locationExistsInLocations(location) || locationExistsInAccessories(location);
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
            stmt.setString(1, DATATYPE_ACCESSORY);
            if (parentLocation != null) {
                stmt.setString(2, parentLocation);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fullLoc = rs.getString("Location");
                    String subLoc = (parentLocation == null) ? fullLoc : fullLoc.substring(parentLocation.length() + 1);
                    int sepIdx = subLoc.indexOf(LogAccessoriesTab.getPathSeparator());
                    if (sepIdx != -1) {
                        subLoc = subLoc.substring(0, sepIdx);
                    }
                    subLocations.add(subLoc);
                }
            }
        }

        String cabSql;
        if (parentLocation == null) {
            cabSql = "SELECT DISTINCT Location FROM Accessories WHERE Accessory_Type NOT LIKE 'Placeholder_%' AND Location NOT LIKE '%/%'";
        } else {
            cabSql = "SELECT DISTINCT Location FROM Accessories WHERE Accessory_Type NOT LIKE 'Placeholder_%' AND Location LIKE ? || '/%'";
        }
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(cabSql)) {
            if (parentLocation != null) {
                stmt.setString(1, parentLocation);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fullLoc = rs.getString("Location");
                    String subLoc = (parentLocation == null) ? fullLoc : fullLoc.substring(parentLocation.length() + 1);
                    int sepIdx = subLoc.indexOf(LogAccessoriesTab.getPathSeparator());
                    if (sepIdx != -1) {
                        subLoc = subLoc.substring(0, sepIdx);
                    }
                    subLocations.add(subLoc);
                }
            }
        }

        List<String> result = new ArrayList<>(subLocations);
        Collections.sort(result);
        return result;
    }

    public static List<String> getAllLocations() throws SQLException {
        List<String> locations = new ArrayList<>();
        String sql = "SELECT DISTINCT Location FROM Accessories WHERE Accessory_Type NOT LIKE 'Placeholder_%' UNION SELECT Location FROM Locations WHERE Datatype = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DATATYPE_ACCESSORY);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String loc = rs.getString("Location");
                    if (loc != null && !loc.isEmpty()) {
                        locations.add(loc);
                    }
                }
            }
        }
        Collections.sort(locations);
        return locations;
    }

    public static void createLocation(String fullPath, String parentLocation) throws SQLException {
        if (fullPath == null || fullPath.isEmpty()) {
            throw new SQLException("Location cannot be empty");
        }
        if (locationExists(fullPath)) {
            throw new SQLException("Location already exists: " + fullPath);
        }
        if (parentLocation != null && !locationExists(parentLocation)) {
            throw new SQLException("Parent location does not exist: " + parentLocation);
        }
        String sql = "INSERT INTO Locations (Datatype, Location) VALUES (?, ?)";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DATATYPE_ACCESSORY);
            stmt.setString(2, fullPath);
            stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Created location: {0}", fullPath);
        }
    }

    public static void addAccessory(String accessoryType, int count, String location) throws SQLException {
        if (count <= 0) {
            throw new SQLException("Count must be positive");
        }
        if (!locationExists(location)) {
            createLocation(location, getParentPath(location));
        }
        String selectSql = "SELECT Id FROM Accessories WHERE Accessory_Type = ? AND Location = ?";
        String updateSql = "UPDATE Accessories SET Count = Count + ? WHERE Id = ?";
        String insertSql = "INSERT INTO Accessories (Accessory_Type, Count, Location) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtils.getConnection()) {
            int accessoryId = -1;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, accessoryType);
                selectStmt.setString(2, location);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        accessoryId = rs.getInt("Id");
                    }
                }
            }
            if (accessoryId != -1) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, count);
                    updateStmt.setInt(2, accessoryId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, accessoryType);
                    insertStmt.setInt(2, count);
                    insertStmt.setString(3, location);
                    insertStmt.executeUpdate();
                }
            }
            LOGGER.log(Level.INFO, "Added {0} accessories of type {1} to {2}", new Object[]{count, accessoryType, location});
        }
    }

    public static void removeAccessory(int accessoryId, int count) throws SQLException {
        if (count <= 0) {
            throw new SQLException("Count to remove must be positive");
        }
        String selectSql = "SELECT Count FROM Accessories WHERE Id = ?";
        String updateSql = "UPDATE Accessories SET Count = Count - ? WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection()) {
            int currentCount = 0;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, accessoryId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        currentCount = rs.getInt("Count");
                    } else {
                        throw new SQLException("Accessory ID not found: " + accessoryId);
                    }
                }
            }
            if (currentCount < count) {
                throw new SQLException("Not enough accessories to remove: " + currentCount + " available, " + count + " requested");
            }
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, count);
                updateStmt.setInt(2, accessoryId);
                updateStmt.executeUpdate();
            }
            if (currentCount - count == 0) {
                deleteAccessory(accessoryId);
            }
        }
    }

    public static boolean canRemoveAccessory(int accessoryId) throws SQLException {
        String sql = "SELECT Count FROM Accessories WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, accessoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Count") > 0;
                }
            }
        }
        return false;
    }

    public static List<AccessoryEntry> getAccessoriesByLocation(String location) throws SQLException {
        List<AccessoryEntry> accessories = new ArrayList<>();
        String sql = "SELECT Accessory_Type, Count FROM Accessories WHERE Location = ? AND Accessory_Type NOT LIKE 'Placeholder_%' ORDER BY Count DESC, Accessory_Type ASC";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    accessories.add(new AccessoryEntry(
                            rs.getString("Accessory_Type"),
                            rs.getInt("Count")
                    ));
                }
            }
        }
        return accessories;
    }

    public static int getAccessoryId(String accessoryType, String location) throws SQLException {
        String sql = "SELECT Id FROM Accessories WHERE Accessory_Type = ? AND Location = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accessoryType);
            stmt.setString(2, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Id");
                }
            }
        }
        return -1;
    }

    public static List<String> getUniqueAccessoryTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        String sql = "SELECT DISTINCT Accessory_Type FROM Accessories WHERE Accessory_Type NOT LIKE 'Placeholder_%' ORDER BY Accessory_Type ASC";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                types.add(rs.getString("Accessory_Type"));
            }
        }
        return types;
    }

    public static List<AccessoryEntry> getAccessoriesSummary(String parentLocation) throws SQLException {
        List<AccessoryEntry> accessories = new ArrayList<>();
        String sql = "SELECT Accessory_Type, SUM(Count) as TotalCount FROM Accessories WHERE Accessory_Type NOT LIKE 'Placeholder_%' AND (Location = ? OR Location LIKE ? || ?) GROUP BY Accessory_Type";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, parentLocation);
            stmt.setString(2, parentLocation);
            stmt.setString(3, LogAccessoriesTab.getPathSeparator() + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    accessories.add(new AccessoryEntry(
                            rs.getString("Accessory_Type"),
                            rs.getInt("TotalCount")
                    ));
                }
            }
        }
        return accessories;
    }

    public static void deleteAccessory(int accessoryId) throws SQLException {
        String sql = "DELETE FROM Accessories WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, accessoryId);
            stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Deleted accessory with ID {0}", accessoryId);
        }
    }

    public static void deleteLocation(String fullPath) throws SQLException {
        if (fullPath == null || fullPath.isEmpty()) {
            throw new SQLException("Invalid location");
        }
        if (fullPath.equals(LogAccessoriesTab.getUnassignedLocation())) {
            throw new SQLException("Cannot delete the Unassigned location");
        }

        String parentPath = getParentPath(fullPath);
        String targetLocation = (parentPath != null) ? parentPath : LogAccessoriesTab.getUnassignedLocation();

        List<AccessoryEntry> accessoriesToMove = new ArrayList<>();
        try (Connection conn = DatabaseUtils.getConnection()) {
            accessoriesToMove.addAll(getAccessoriesByLocation(fullPath));

            String subLocationSql = "SELECT Accessory_Type, Count, Location FROM Accessories WHERE Accessory_Type NOT LIKE 'Placeholder_%' AND Location LIKE ? || ?";
            try (PreparedStatement subStmt = conn.prepareStatement(subLocationSql)) {
                subStmt.setString(1, fullPath);
                subStmt.setString(2, LogAccessoriesTab.getPathSeparator() + "%");
                try (ResultSet rs = subStmt.executeQuery()) {
                    while (rs.next()) {
                        accessoriesToMove.add(new AccessoryEntry(
                                rs.getString("Accessory_Type"),
                                rs.getInt("Count")
                        ));
                    }
                }
            }

            conn.setAutoCommit(false);
            try {
                for (AccessoryEntry ae : accessoriesToMove) {
                    int targetId = getAccessoryId(ae.accessoryType, targetLocation);
                    if (targetId != -1) {
                        String updateSql = "UPDATE Accessories SET Count = Count + ? WHERE Id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, ae.count);
                            updateStmt.setInt(2, targetId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        String insertSql = "INSERT INTO Accessories (Accessory_Type, Count, Location) VALUES (?, ?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, ae.accessoryType);
                            insertStmt.setInt(2, ae.count);
                            insertStmt.setString(3, targetLocation);
                            insertStmt.executeUpdate();
                        }
                    }
                }

                String deleteAccessoriesSql = "DELETE FROM Accessories WHERE Location = ? OR Location LIKE ? || ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteAccessoriesSql)) {
                    deleteStmt.setString(1, fullPath);
                    deleteStmt.setString(2, fullPath);
                    deleteStmt.setString(3, LogAccessoriesTab.getPathSeparator() + "%");
                    deleteStmt.executeUpdate();
                }

                String deleteLocationSql = "DELETE FROM Locations WHERE Datatype = ? AND (Location = ? OR Location LIKE ? || ?)";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteLocationSql)) {
                    deleteStmt.setString(1, DATATYPE_ACCESSORY);
                    deleteStmt.setString(2, fullPath);
                    deleteStmt.setString(3, fullPath);
                    deleteStmt.setString(4, LogAccessoriesTab.getPathSeparator() + "%");
                    deleteStmt.executeUpdate();
                }

                conn.commit();
                LOGGER.log(Level.INFO, "Deleted location {0} and moved accessories to {1}", new Object[]{fullPath, targetLocation});
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void moveAccessory(int accessoryId, String targetLocation, int countToMove) throws SQLException {
        if (countToMove <= 0) {
            throw new SQLException("Count to move must be positive");
        }
        if (!locationExists(targetLocation)) {
            throw new SQLException("Target location does not exist: " + targetLocation);
        }

        String selectSql = "SELECT Accessory_Type, Count, Location FROM Accessories WHERE Id = ?";
        String updateSourceSql = "UPDATE Accessories SET Count = Count - ? WHERE Id = ?";
        String selectTargetSql = "SELECT Id FROM Accessories WHERE Accessory_Type = ? AND Location = ?";
        String updateTargetSql = "UPDATE Accessories SET Count = Count + ? WHERE Id = ?";
        String insertTargetSql = "INSERT INTO Accessories (Accessory_Type, Count, Location) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseUtils.getConnection()) {
            String accessoryType = null;
            int currentCount = 0;
            String sourceLocation = null;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, accessoryId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        accessoryType = rs.getString("Accessory_Type");
                        currentCount = rs.getInt("Count");
                        sourceLocation = rs.getString("Location");
                    } else {
                        throw new SQLException("Accessory ID not found: " + accessoryId);
                    }
                }
            }

            if (currentCount < countToMove) {
                throw new SQLException("Not enough accessories to move: " + currentCount + " available, " + countToMove + " requested");
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSourceSql)) {
                updateStmt.setInt(1, countToMove);
                updateStmt.setInt(2, accessoryId);
                updateStmt.executeUpdate();
            }

            int targetAccessoryId = -1;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectTargetSql)) {
                selectStmt.setString(1, accessoryType);
                selectStmt.setString(2, targetLocation);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        targetAccessoryId = rs.getInt("Id");
                    }
                }
            }

            if (targetAccessoryId != -1) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateTargetSql)) {
                    updateStmt.setInt(1, countToMove);
                    updateStmt.setInt(2, targetAccessoryId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertTargetSql)) {
                    insertStmt.setString(1, accessoryType);
                    insertStmt.setInt(2, countToMove);
                    insertStmt.setString(3, targetLocation);
                    insertStmt.executeUpdate();
                }
            }

            if (currentCount == countToMove) {
                deleteAccessory(accessoryId);
            }

            LOGGER.log(Level.INFO, "Moved {0} accessories of type {1} from {2} to {3}", new Object[]{countToMove, accessoryType, sourceLocation, targetLocation});
        }
    }

    private static String getParentPath(String fullPath) {
        if (fullPath == null || fullPath.equals(LogAccessoriesTab.getUnassignedLocation())) {
            return null;
        }
        int idx = fullPath.lastIndexOf(LogAccessoriesTab.getPathSeparator());
        if (idx == -1) {
            return null;
        }
        return fullPath.substring(0, idx);
    }

    public static void cleanUpUnassignedSublocations() throws SQLException {
        String sql = "UPDATE Accessories SET Location = ? WHERE Location LIKE ? || ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LogAccessoriesTab.getUnassignedLocation());
            stmt.setString(2, "%");
            stmt.setString(3, LogAccessoriesTab.getPathSeparator() + LogAccessoriesTab.getUnassignedLocation());
            stmt.executeUpdate();
        }
    }
}