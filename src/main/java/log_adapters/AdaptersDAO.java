package log_adapters;

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

public class AdaptersDAO {
    private static final Logger LOGGER = Logger.getLogger(AdaptersDAO.class.getName());
    private static final String DATATYPE_ADAPTER = "Adapter";

    public static class AdapterEntry {
        public final String adapterType;
        public final int count;

        public AdapterEntry(String adapterType, int count) {
            this.adapterType = adapterType;
            this.count = count;
        }
    }

    public static void ensureSchema() throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            boolean tableExists = false;
            try (ResultSet rs = metaData.getTables(null, null, "Adapters", new String[]{"TABLE"})) {
                if (rs.next()) {
                    tableExists = true;
                    LOGGER.log(Level.INFO, "Adapters table found: {0}", rs.getString("TABLE_NAME"));
                } else {
                    LOGGER.log(Level.INFO, "Adapters table does not exist");
                }
            }

            if (!tableExists) {
                String createTableSQL = "CREATE TABLE Adapters (Id COUNTER PRIMARY KEY, Adapter_Type VARCHAR(255), Count INTEGER, Location VARCHAR(255), UNIQUE(Adapter_Type, Location))";
                try (PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
                    stmt.executeUpdate();
                    LOGGER.log(Level.INFO, "Created Adapters table");
                }
            } else {
                LOGGER.log(Level.INFO, "Adapters table already exists");
                try (ResultSet rs = metaData.getColumns(null, null, "Adapters", "Parent_Location")) {
                    if (rs.next()) {
                        String alterTableSQL = "ALTER TABLE Adapters DROP COLUMN Parent_Location";
                        try (PreparedStatement stmt = conn.prepareStatement(alterTableSQL)) {
                            stmt.executeUpdate();
                            LOGGER.log(Level.INFO, "Dropped Parent_Location column from Adapters table");
                        }
                    }
                }
            }

            String selectOldPlaceholders = "SELECT DISTINCT Location FROM Adapters WHERE Adapter_Type LIKE 'Placeholder_%'";
            try (PreparedStatement selStmt = conn.prepareStatement(selectOldPlaceholders);
                 PreparedStatement insStmt = conn.prepareStatement("INSERT INTO Locations (Datatype, Location) VALUES (?, ?)");
                 PreparedStatement delStmt = conn.prepareStatement("DELETE FROM Adapters WHERE Adapter_Type LIKE 'Placeholder_%' AND Location = ?")) {
                try (ResultSet rs = selStmt.executeQuery()) {
                    while (rs.next()) {
                        String loc = rs.getString("Location");
                        insStmt.setString(1, DATATYPE_ADAPTER);
                        insStmt.setString(2, loc);
                        insStmt.executeUpdate();
                        delStmt.setString(1, loc);
                        delStmt.executeUpdate();
                        LOGGER.log(Level.INFO, "Migrated placeholder location: {0}", loc);
                    }
                }
            }

            try (ResultSet rs = metaData.getIndexInfo(null, null, "Adapters", false, false)) {
                boolean indexExists = false;
                while (rs.next()) {
                    if ("idx_adapters_location".equals(rs.getString("INDEX_NAME"))) {
                        indexExists = true;
                        break;
                    }
                }
                if (!indexExists) {
                    try {
                        String createIndexSQL = "CREATE INDEX idx_adapters_location ON Adapters(Location)";
                        try (PreparedStatement stmt = conn.prepareStatement(createIndexSQL)) {
                            stmt.executeUpdate();
                            LOGGER.log(Level.INFO, "Created index idx_adapters_location on Adapters table");
                        }
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Failed to create index idx_adapters_location: {0}", e.getMessage());
                    }
                }
            }
        }
    }

    private static boolean locationExistsInLocations(String location) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Locations WHERE Datatype = ? AND Location = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DATATYPE_ADAPTER);
            stmt.setString(2, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private static boolean locationExistsInAdapters(String location) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Adapters WHERE Location = ? AND Adapter_Type NOT LIKE 'Placeholder_%'";
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
        return locationExistsInLocations(location) || locationExistsInAdapters(location);
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
            stmt.setString(1, DATATYPE_ADAPTER);
            if (parentLocation != null) {
                stmt.setString(2, parentLocation);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fullLoc = rs.getString("Location");
                    String subLoc = (parentLocation == null) ? fullLoc : fullLoc.substring(parentLocation.length() + 1);
                    int sepIdx = subLoc.indexOf(LogAdaptersTab.getPathSeparator());
                    if (sepIdx != -1) {
                        subLoc = subLoc.substring(0, sepIdx);
                    }
                    subLocations.add(subLoc);
                }
            }
        }

        String cabSql;
        if (parentLocation == null) {
            cabSql = "SELECT DISTINCT Location FROM Adapters WHERE Adapter_Type NOT LIKE 'Placeholder_%' AND Location NOT LIKE '%/%'";
        } else {
            cabSql = "SELECT DISTINCT Location FROM Adapters WHERE Adapter_Type NOT LIKE 'Placeholder_%' AND Location LIKE ? || '/%'";
        }
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(cabSql)) {
            if (parentLocation != null) {
                stmt.setString(1, parentLocation);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fullLoc = rs.getString("Location");
                    String subLoc = (parentLocation == null) ? fullLoc : fullLoc.substring(parentLocation.length() + 1);
                    int sepIdx = subLoc.indexOf(LogAdaptersTab.getPathSeparator());
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
        String sql = "SELECT DISTINCT Location FROM Adapters WHERE Adapter_Type NOT LIKE 'Placeholder_%' UNION SELECT Location FROM Locations WHERE Datatype = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DATATYPE_ADAPTER);
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
            stmt.setString(1, DATATYPE_ADAPTER);
            stmt.setString(2, fullPath);
            stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Created location: {0}", fullPath);
        }
    }

    public static void addAdapter(String adapterType, int count, String location) throws SQLException {
        if (count <= 0) {
            throw new SQLException("Count must be positive");
        }
        if (!locationExists(location)) {
            createLocation(location, getParentPath(location));
        }
        String selectSql = "SELECT Id FROM Adapters WHERE Adapter_Type = ? AND Location = ?";
        String updateSql = "UPDATE Adapters SET Count = Count + ? WHERE Id = ?";
        String insertSql = "INSERT INTO Adapters (Adapter_Type, Count, Location) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtils.getConnection()) {
            int adapterId = -1;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, adapterType);
                selectStmt.setString(2, location);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        adapterId = rs.getInt("Id");
                    }
                }
            }
            if (adapterId != -1) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, count);
                    updateStmt.setInt(2, adapterId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, adapterType);
                    insertStmt.setInt(2, count);
                    insertStmt.setString(3, location);
                    insertStmt.executeUpdate();
                }
            }
            LOGGER.log(Level.INFO, "Added {0} adapters of type {1} to {2}", new Object[]{count, adapterType, location});
        }
    }

    public static void removeAdapter(int adapterId, int count) throws SQLException {
        if (count <= 0) {
            throw new SQLException("Count to remove must be positive");
        }
        String selectSql = "SELECT Count FROM Adapters WHERE Id = ?";
        String updateSql = "UPDATE Adapters SET Count = Count - ? WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection()) {
            int currentCount = 0;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, adapterId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        currentCount = rs.getInt("Count");
                    } else {
                        throw new SQLException("Adapter ID not found: " + adapterId);
                    }
                }
            }
            if (currentCount < count) {
                throw new SQLException("Not enough adapters to remove: " + currentCount + " available, " + count + " requested");
            }
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, count);
                updateStmt.setInt(2, adapterId);
                updateStmt.executeUpdate();
            }
            if (currentCount - count == 0) {
                deleteAdapter(adapterId);
            }
            LOGGER.log(Level.INFO, "Removed {0} adapters with ID: {1}", new Object[]{count, adapterId});
        }
    }

    public static boolean canRemoveAdapter(int adapterId) throws SQLException {
        String sql = "SELECT Count FROM Adapters WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, adapterId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Count") > 0;
                }
            }
        }
        return false;
    }

    public static List<AdapterEntry> getAdaptersByLocation(String location) throws SQLException {
        List<AdapterEntry> adapters = new ArrayList<>();
        String sql = "SELECT Adapter_Type, Count FROM Adapters WHERE Location = ? AND Adapter_Type NOT LIKE 'Placeholder_%' ORDER BY Count DESC, Adapter_Type ASC";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    adapters.add(new AdapterEntry(
                            rs.getString("Adapter_Type"),
                            rs.getInt("Count")
                    ));
                }
            }
        }
        return adapters;
    }

    public static int getAdapterId(String adapterType, String location) throws SQLException {
        String sql = "SELECT Id FROM Adapters WHERE Adapter_Type = ? AND Location = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adapterType);
            stmt.setString(2, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Id");
                }
            }
        }
        return -1;
    }

    public static List<String> getUniqueAdapterTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        String sql = "SELECT DISTINCT Adapter_Type FROM Adapters WHERE Adapter_Type NOT LIKE 'Placeholder_%' ORDER BY Adapter_Type ASC";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                types.add(rs.getString("Adapter_Type"));
            }
        }
        return types;
    }

    public static List<AdapterEntry> getAdaptersSummary(String parentLocation) throws SQLException {
        List<AdapterEntry> adapters = new ArrayList<>();
        String sql = "SELECT Adapter_Type, SUM(Count) as TotalCount FROM Adapters WHERE Adapter_Type NOT LIKE 'Placeholder_%' AND (Location = ? OR Location LIKE ? || ?) GROUP BY Adapter_Type";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, parentLocation);
            stmt.setString(2, parentLocation);
            stmt.setString(3, LogAdaptersTab.getPathSeparator() + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    adapters.add(new AdapterEntry(
                            rs.getString("Adapter_Type"),
                            rs.getInt("TotalCount")
                    ));
                }
            }
        }
        return adapters;
    }

    public static void deleteAdapter(int adapterId) throws SQLException {
        String sql = "DELETE FROM Adapters WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, adapterId);
            stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Deleted adapter with ID {0}", adapterId);
        }
    }

    public static void deleteLocation(String fullPath) throws SQLException {
        if (fullPath == null || fullPath.isEmpty()) {
            throw new SQLException("Invalid location");
        }
        if (fullPath.equals(LogAdaptersTab.getUnassignedLocation())) {
            throw new SQLException("Cannot delete the Unassigned location");
        }

        String parentPath = getParentPath(fullPath);
        String targetLocation = (parentPath != null) ? parentPath : LogAdaptersTab.getUnassignedLocation();

        List<AdapterEntry> adaptersToMove = new ArrayList<>();
        try (Connection conn = DatabaseUtils.getConnection()) {
            adaptersToMove.addAll(getAdaptersByLocation(fullPath));

            String subLocationSql = "SELECT Adapter_Type, Count, Location FROM Adapters WHERE Adapter_Type NOT LIKE 'Placeholder_%' AND Location LIKE ? || ?";
            try (PreparedStatement subStmt = conn.prepareStatement(subLocationSql)) {
                subStmt.setString(1, fullPath);
                subStmt.setString(2, LogAdaptersTab.getPathSeparator() + "%");
                try (ResultSet rs = subStmt.executeQuery()) {
                    while (rs.next()) {
                        adaptersToMove.add(new AdapterEntry(
                                rs.getString("Adapter_Type"),
                                rs.getInt("Count")
                        ));
                    }
                }
            }

            conn.setAutoCommit(false);
            try {
                for (AdapterEntry ae : adaptersToMove) {
                    int targetId = getAdapterId(ae.adapterType, targetLocation);
                    if (targetId != -1) {
                        String updateSql = "UPDATE Adapters SET Count = Count + ? WHERE Id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, ae.count);
                            updateStmt.setInt(2, targetId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        String insertSql = "INSERT INTO Adapters (Adapter_Type, Count, Location) VALUES (?, ?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, ae.adapterType);
                            insertStmt.setInt(2, ae.count);
                            insertStmt.setString(3, targetLocation);
                            insertStmt.executeUpdate();
                        }
                    }
                }

                String deleteAdaptersSql = "DELETE FROM Adapters WHERE Location = ? OR Location LIKE ? || ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteAdaptersSql)) {
                    deleteStmt.setString(1, fullPath);
                    deleteStmt.setString(2, fullPath);
                    deleteStmt.setString(3, LogAdaptersTab.getPathSeparator() + "%");
                    deleteStmt.executeUpdate();
                }

                String deleteLocationSql = "DELETE FROM Locations WHERE Datatype = ? AND (Location = ? OR Location LIKE ? || ?)";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteLocationSql)) {
                    deleteStmt.setString(1, DATATYPE_ADAPTER);
                    deleteStmt.setString(2, fullPath);
                    deleteStmt.setString(3, fullPath);
                    deleteStmt.setString(4, LogAdaptersTab.getPathSeparator() + "%");
                    deleteStmt.executeUpdate();
                }

                conn.commit();
                LOGGER.log(Level.INFO, "Deleted location {0} and moved adapters to {1}", new Object[]{fullPath, targetLocation});
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void moveAdapter(int adapterId, String targetLocation, int countToMove) throws SQLException {
        if (countToMove <= 0) {
            throw new SQLException("Count to move must be positive");
        }
        if (!locationExists(targetLocation)) {
            throw new SQLException("Target location does not exist: " + targetLocation);
        }

        String selectSql = "SELECT Adapter_Type, Count, Location FROM Adapters WHERE Id = ?";
        String updateSourceSql = "UPDATE Adapters SET Count = Count - ? WHERE Id = ?";
        String selectTargetSql = "SELECT Id FROM Adapters WHERE Adapter_Type = ? AND Location = ?";
        String updateTargetSql = "UPDATE Adapters SET Count = Count + ? WHERE Id = ?";
        String insertTargetSql = "INSERT INTO Adapters (Adapter_Type, Count, Location) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseUtils.getConnection()) {
            String adapterType = null;
            int currentCount = 0;
            String sourceLocation = null;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, adapterId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        adapterType = rs.getString("Adapter_Type");
                        currentCount = rs.getInt("Count");
                        sourceLocation = rs.getString("Location");
                    } else {
                        throw new SQLException("Adapter ID not found: " + adapterId);
                    }
                }
            }

            if (currentCount < countToMove) {
                throw new SQLException("Not enough adapters to move: " + currentCount + " available, " + countToMove + " requested");
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSourceSql)) {
                updateStmt.setInt(1, countToMove);
                updateStmt.setInt(2, adapterId);
                updateStmt.executeUpdate();
            }

            int targetAdapterId = -1;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectTargetSql)) {
                selectStmt.setString(1, adapterType);
                selectStmt.setString(2, targetLocation);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        targetAdapterId = rs.getInt("Id");
                    }
                }
            }

            if (targetAdapterId != -1) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateTargetSql)) {
                    updateStmt.setInt(1, countToMove);
                    updateStmt.setInt(2, targetAdapterId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertTargetSql)) {
                    insertStmt.setString(1, adapterType);
                    insertStmt.setInt(2, countToMove);
                    insertStmt.setString(3, targetLocation);
                    insertStmt.executeUpdate();
                }
            }

            if (currentCount == countToMove) {
                deleteAdapter(adapterId);
            }

            LOGGER.log(Level.INFO, "Moved {0} adapters of type {1} from {2} to {3}", new Object[]{countToMove, adapterType, sourceLocation, targetLocation});
        }
    }

    private static String getParentPath(String fullPath) {
        if (fullPath == null || fullPath.equals(LogAdaptersTab.getUnassignedLocation())) {
            return null;
        }
        int idx = fullPath.lastIndexOf(LogAdaptersTab.getPathSeparator());
        if (idx == -1) {
            return null;
        }
        return fullPath.substring(0, idx);
    }

    public static void cleanUpUnassignedSublocations() throws SQLException {
        String sql = "UPDATE Adapters SET Location = ? WHERE Location LIKE ? || ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LogAdaptersTab.getUnassignedLocation());
            stmt.setString(2, "%");
            stmt.setString(3, LogAdaptersTab.getPathSeparator() + LogAdaptersTab.getUnassignedLocation());
            stmt.executeUpdate();
        }
    }
}