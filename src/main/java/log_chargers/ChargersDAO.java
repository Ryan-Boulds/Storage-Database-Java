package log_chargers;

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

public class ChargersDAO {
    private static final Logger LOGGER = Logger.getLogger(ChargersDAO.class.getName());
    private static final String DATATYPE_CHARGER = "Charger";

    public static class ChargerEntry {
        public final String chargerType;
        public final int count;

        public ChargerEntry(String chargerType, int count) {
            this.chargerType = chargerType;
            this.count = count;
        }
    }

    public static void ensureSchema() throws SQLException {
        try (Connection conn = DatabaseUtils.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            boolean tableExists = false;
            try (ResultSet rs = metaData.getTables(null, null, "Chargers", new String[]{"TABLE"})) {
                if (rs.next()) {
                    tableExists = true;
                    LOGGER.log(Level.INFO, "Chargers table found: {0}", rs.getString("TABLE_NAME"));
                } else {
                    LOGGER.log(Level.INFO, "Chargers table does not exist");
                }
            }

            if (!tableExists) {
                String createTableSQL = "CREATE TABLE Chargers (Id COUNTER PRIMARY KEY, Charger_Type VARCHAR(255), Count INTEGER, Location VARCHAR(255), UNIQUE(Charger_Type, Location))";
                try (PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
                    stmt.executeUpdate();
                    LOGGER.log(Level.INFO, "Created Chargers table");
                }
            } else {
                LOGGER.log(Level.INFO, "Chargers table already exists");
                try (ResultSet rs = metaData.getColumns(null, null, "Chargers", "Parent_Location")) {
                    if (rs.next()) {
                        String alterTableSQL = "ALTER TABLE Chargers DROP COLUMN Parent_Location";
                        try (PreparedStatement stmt = conn.prepareStatement(alterTableSQL)) {
                            stmt.executeUpdate();
                            LOGGER.log(Level.INFO, "Dropped Parent_Location column from Chargers table");
                        }
                    }
                }
            }

            String selectOldPlaceholders = "SELECT DISTINCT Location FROM Chargers WHERE Charger_Type LIKE 'Placeholder_%'";
            try (PreparedStatement selStmt = conn.prepareStatement(selectOldPlaceholders);
                 PreparedStatement insStmt = conn.prepareStatement("INSERT INTO Locations (Datatype, Location) VALUES (?, ?)");
                 PreparedStatement delStmt = conn.prepareStatement("DELETE FROM Chargers WHERE Charger_Type LIKE 'Placeholder_%' AND Location = ?")) {
                try (ResultSet rs = selStmt.executeQuery()) {
                    while (rs.next()) {
                        String loc = rs.getString("Location");
                        insStmt.setString(1, DATATYPE_CHARGER);
                        insStmt.setString(2, loc);
                        insStmt.executeUpdate();
                        delStmt.setString(1, loc);
                        delStmt.executeUpdate();
                        LOGGER.log(Level.INFO, "Migrated placeholder location: {0}", loc);
                    }
                }
            }

            try (ResultSet rs = metaData.getIndexInfo(null, null, "Chargers", false, false)) {
                boolean indexExists = false;
                while (rs.next()) {
                    if ("idx_chargers_location".equals(rs.getString("INDEX_NAME"))) {
                        indexExists = true;
                        break;
                    }
                }
                if (!indexExists) {
                    try {
                        String createIndexSQL = "CREATE INDEX idx_chargers_location ON Chargers(Location)";
                        try (PreparedStatement stmt = conn.prepareStatement(createIndexSQL)) {
                            stmt.executeUpdate();
                            LOGGER.log(Level.INFO, "Created index idx_chargers_location on Chargers table");
                        }
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Failed to create index idx_chargers_location: {0}", e.getMessage());
                    }
                }
            }
        }
    }

    private static boolean locationExistsInLocations(String location) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Locations WHERE Datatype = ? AND Location = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DATATYPE_CHARGER);
            stmt.setString(2, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private static boolean locationExistsInChargers(String location) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Chargers WHERE Location = ? AND Charger_Type NOT LIKE 'Placeholder_%'";
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
        return locationExistsInLocations(location) || locationExistsInChargers(location);
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
            stmt.setString(1, DATATYPE_CHARGER);
            if (parentLocation != null) {
                stmt.setString(2, parentLocation);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fullLoc = rs.getString("Location");
                    String subLoc = (parentLocation == null) ? fullLoc : fullLoc.substring(parentLocation.length() + 1);
                    int sepIdx = subLoc.indexOf(LogChargersTab.getPathSeparator());
                    if (sepIdx != -1) {
                        subLoc = subLoc.substring(0, sepIdx);
                    }
                    subLocations.add(subLoc);
                }
            }
        }

        String cabSql;
        if (parentLocation == null) {
            cabSql = "SELECT DISTINCT Location FROM Chargers WHERE Charger_Type NOT LIKE 'Placeholder_%' AND Location NOT LIKE '%/%'";
        } else {
            cabSql = "SELECT DISTINCT Location FROM Chargers WHERE Charger_Type NOT LIKE 'Placeholder_%' AND Location LIKE ? || '/%'";
        }
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(cabSql)) {
            if (parentLocation != null) {
                stmt.setString(1, parentLocation);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fullLoc = rs.getString("Location");
                    String subLoc = (parentLocation == null) ? fullLoc : fullLoc.substring(parentLocation.length() + 1);
                    int sepIdx = subLoc.indexOf(LogChargersTab.getPathSeparator());
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
        String sql = "SELECT DISTINCT Location FROM Chargers WHERE Charger_Type NOT LIKE 'Placeholder_%' UNION SELECT Location FROM Locations WHERE Datatype = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DATATYPE_CHARGER);
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
            stmt.setString(1, DATATYPE_CHARGER);
            stmt.setString(2, fullPath);
            stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Created location: {0}", fullPath);
        }
    }

    public static void addCharger(String chargerType, int count, String location) throws SQLException {
        if (count <= 0) {
            throw new SQLException("Count must be positive");
        }
        if (!locationExists(location)) {
            createLocation(location, getParentPath(location));
        }
        String selectSql = "SELECT Id FROM Chargers WHERE Charger_Type = ? AND Location = ?";
        String updateSql = "UPDATE Chargers SET Count = Count + ? WHERE Id = ?";
        String insertSql = "INSERT INTO Chargers (Charger_Type, Count, Location) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtils.getConnection()) {
            int chargerId = -1;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, chargerType);
                selectStmt.setString(2, location);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        chargerId = rs.getInt("Id");
                    }
                }
            }
            if (chargerId != -1) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, count);
                    updateStmt.setInt(2, chargerId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, chargerType);
                    insertStmt.setInt(2, count);
                    insertStmt.setString(3, location);
                    insertStmt.executeUpdate();
                }
            }
            LOGGER.log(Level.INFO, "Added {0} chargers of type {1} to {2}", new Object[]{count, chargerType, location});
        }
    }

    public static void removeCharger(int chargerId, int count) throws SQLException {
        if (count <= 0) {
            throw new SQLException("Count to remove must be positive");
        }
        String selectSql = "SELECT Count FROM Chargers WHERE Id = ?";
        String updateSql = "UPDATE Chargers SET Count = Count - ? WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection()) {
            int currentCount = 0;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, chargerId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        currentCount = rs.getInt("Count");
                    } else {
                        throw new SQLException("Charger ID not found: " + chargerId);
                    }
                }
            }
            if (currentCount < count) {
                throw new SQLException("Not enough chargers to remove: " + currentCount + " available, " + count + " requested");
            }
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, count);
                updateStmt.setInt(2, chargerId);
                updateStmt.executeUpdate();
            }
            if (currentCount - count == 0) {
                deleteCharger(chargerId);
            }
        }
    }

    public static boolean canRemoveCharger(int chargerId) throws SQLException {
        String sql = "SELECT Count FROM Chargers WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chargerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Count") > 0;
                }
            }
        }
        return false;
    }

    public static List<ChargerEntry> getChargersByLocation(String location) throws SQLException {
        List<ChargerEntry> chargers = new ArrayList<>();
        String sql = "SELECT Charger_Type, Count FROM Chargers WHERE Location = ? AND Charger_Type NOT LIKE 'Placeholder_%' ORDER BY Count DESC, Charger_Type ASC";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    chargers.add(new ChargerEntry(
                            rs.getString("Charger_Type"),
                            rs.getInt("Count")
                    ));
                }
            }
        }
        return chargers;
    }

    public static int getChargerId(String chargerType, String location) throws SQLException {
        String sql = "SELECT Id FROM Chargers WHERE Charger_Type = ? AND Location = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chargerType);
            stmt.setString(2, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Id");
                }
            }
        }
        return -1;
    }

    public static List<String> getUniqueChargerTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        String sql = "SELECT DISTINCT Charger_Type FROM Chargers WHERE Charger_Type NOT LIKE 'Placeholder_%' ORDER BY Charger_Type ASC";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                types.add(rs.getString("Charger_Type"));
            }
        }
        return types;
    }

    public static List<ChargerEntry> getChargersSummary(String parentLocation) throws SQLException {
        List<ChargerEntry> chargers = new ArrayList<>();
        String sql = "SELECT Charger_Type, SUM(Count) as TotalCount FROM Chargers WHERE Charger_Type NOT LIKE 'Placeholder_%' AND (Location = ? OR Location LIKE ? || ?) GROUP BY Charger_Type";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, parentLocation);
            stmt.setString(2, parentLocation);
            stmt.setString(3, LogChargersTab.getPathSeparator() + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    chargers.add(new ChargerEntry(
                            rs.getString("Charger_Type"),
                            rs.getInt("TotalCount")
                    ));
                }
            }
        }
        return chargers;
    }

    public static void deleteCharger(int chargerId) throws SQLException {
        String sql = "DELETE FROM Chargers WHERE Id = ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chargerId);
            stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Deleted charger with ID {0}", chargerId);
        }
    }

    public static void deleteLocation(String fullPath) throws SQLException {
        if (fullPath == null || fullPath.isEmpty()) {
            throw new SQLException("Invalid location");
        }
        if (fullPath.equals(LogChargersTab.getUnassignedLocation())) {
            throw new SQLException("Cannot delete the Unassigned location");
        }

        String parentPath = getParentPath(fullPath);
        String targetLocation = (parentPath != null) ? parentPath : LogChargersTab.getUnassignedLocation();

        List<ChargerEntry> chargersToMove = new ArrayList<>();
        try (Connection conn = DatabaseUtils.getConnection()) {
            chargersToMove.addAll(getChargersByLocation(fullPath));

            String subLocationSql = "SELECT Charger_Type, Count, Location FROM Chargers WHERE Charger_Type NOT LIKE 'Placeholder_%' AND Location LIKE ? || ?";
            try (PreparedStatement subStmt = conn.prepareStatement(subLocationSql)) {
                subStmt.setString(1, fullPath);
                subStmt.setString(2, LogChargersTab.getPathSeparator() + "%");
                try (ResultSet rs = subStmt.executeQuery()) {
                    while (rs.next()) {
                        chargersToMove.add(new ChargerEntry(
                                rs.getString("Charger_Type"),
                                rs.getInt("Count")
                        ));
                    }
                }
            }

            conn.setAutoCommit(false);
            try {
                for (ChargerEntry ce : chargersToMove) {
                    int targetId = getChargerId(ce.chargerType, targetLocation);
                    if (targetId != -1) {
                        String updateSql = "UPDATE Chargers SET Count = Count + ? WHERE Id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, ce.count);
                            updateStmt.setInt(2, targetId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        String insertSql = "INSERT INTO Chargers (Charger_Type, Count, Location) VALUES (?, ?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, ce.chargerType);
                            insertStmt.setInt(2, ce.count);
                            insertStmt.setString(3, targetLocation);
                            insertStmt.executeUpdate();
                        }
                    }
                }

                String deleteChargersSql = "DELETE FROM Chargers WHERE Location = ? OR Location LIKE ? || ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteChargersSql)) {
                    deleteStmt.setString(1, fullPath);
                    deleteStmt.setString(2, fullPath);
                    deleteStmt.setString(3, LogChargersTab.getPathSeparator() + "%");
                    deleteStmt.executeUpdate();
                }

                String deleteLocationSql = "DELETE FROM Locations WHERE Datatype = ? AND (Location = ? OR Location LIKE ? || ?)";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteLocationSql)) {
                    deleteStmt.setString(1, DATATYPE_CHARGER);
                    deleteStmt.setString(2, fullPath);
                    deleteStmt.setString(3, fullPath);
                    deleteStmt.setString(4, LogChargersTab.getPathSeparator() + "%");
                    deleteStmt.executeUpdate();
                }

                conn.commit();
                LOGGER.log(Level.INFO, "Deleted location {0} and moved chargers to {1}", new Object[]{fullPath, targetLocation});
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void moveCharger(int chargerId, String targetLocation, int countToMove) throws SQLException {
        if (countToMove <= 0) {
            throw new SQLException("Count to move must be positive");
        }
        if (!locationExists(targetLocation)) {
            throw new SQLException("Target location does not exist: " + targetLocation);
        }

        String selectSql = "SELECT Charger_Type, Count, Location FROM Chargers WHERE Id = ?";
        String updateSourceSql = "UPDATE Chargers SET Count = Count - ? WHERE Id = ?";
        String selectTargetSql = "SELECT Id FROM Chargers WHERE Charger_Type = ? AND Location = ?";
        String updateTargetSql = "UPDATE Chargers SET Count = Count + ? WHERE Id = ?";
        String insertTargetSql = "INSERT INTO Chargers (Charger_Type, Count, Location) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseUtils.getConnection()) {
            String chargerType = null;
            int currentCount = 0;
            String sourceLocation = null;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, chargerId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        chargerType = rs.getString("Charger_Type");
                        currentCount = rs.getInt("Count");
                        sourceLocation = rs.getString("Location");
                    } else {
                        throw new SQLException("Charger ID not found: " + chargerId);
                    }
                }
            }

            if (currentCount < countToMove) {
                throw new SQLException("Not enough chargers to move: " + currentCount + " available, " + countToMove + " requested");
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSourceSql)) {
                updateStmt.setInt(1, countToMove);
                updateStmt.setInt(2, chargerId);
                updateStmt.executeUpdate();
            }

            int targetChargerId = -1;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectTargetSql)) {
                selectStmt.setString(1, chargerType);
                selectStmt.setString(2, targetLocation);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        targetChargerId = rs.getInt("Id");
                    }
                }
            }

            if (targetChargerId != -1) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateTargetSql)) {
                    updateStmt.setInt(1, countToMove);
                    updateStmt.setInt(2, targetChargerId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertTargetSql)) {
                    insertStmt.setString(1, chargerType);
                    insertStmt.setInt(2, countToMove);
                    insertStmt.setString(3, targetLocation);
                    insertStmt.executeUpdate();
                }
            }

            if (currentCount == countToMove) {
                deleteCharger(chargerId);
            }

            LOGGER.log(Level.INFO, "Moved {0} chargers of type {1} from {2} to {3}", new Object[]{countToMove, chargerType, sourceLocation, targetLocation});
        }
    }

    private static String getParentPath(String fullPath) {
        if (fullPath == null || fullPath.equals(LogChargersTab.getUnassignedLocation())) {
            return null;
        }
        int idx = fullPath.lastIndexOf(LogChargersTab.getPathSeparator());
        if (idx == -1) {
            return null;
        }
        return fullPath.substring(0, idx);
    }

    public static void cleanUpUnassignedSublocations() throws SQLException {
        String sql = "UPDATE Chargers SET Location = ? WHERE Location LIKE ? || ?";
        try (Connection conn = DatabaseUtils.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LogChargersTab.getUnassignedLocation());
            stmt.setString(2, "%");
            stmt.setString(3, LogChargersTab.getPathSeparator() + LogChargersTab.getUnassignedLocation());
            stmt.executeUpdate();
        }
    }
}