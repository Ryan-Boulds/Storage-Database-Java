package software_data_importer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import utils.DataEntry;
import utils.DatabaseUtils;

public class DatabaseHandler {
    private static final Logger LOGGER = Logger.getLogger(DatabaseHandler.class.getName());

    public DatabaseHandler() {
    }

    public void saveDevice(String tableName, HashMap<String, String> device) throws SQLException {
        try {
            DatabaseUtils.saveDevice(tableName, device);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving device to table {0}: {1}", new Object[]{tableName, e.getMessage()});
            throw e;
        }
    }

    public void updateDeviceInDB(String tableName, HashMap<String, String> device) throws SQLException {
        try {
            DatabaseUtils.updateDevice(tableName, device);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating device in table {0}: {1}", new Object[]{tableName, e.getMessage()});
            throw e;
        }
    }

    public HashMap<String, String> getDeviceByAssetNameFromDB(String tableName, String assetName) throws SQLException {
        try {
            return DatabaseUtils.getDeviceByAssetName(tableName, assetName);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving device with AssetName {0} from table {1}: {2}", 
                       new Object[]{assetName, tableName, e.getMessage()});
            throw e;
        }
    }

    public void addNewField(String tableName, String fieldName, String fieldType) throws SQLException {
        try {
            DatabaseUtils.addNewField(tableName, fieldName, fieldType);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding new field {0} to table {1}: {2}", 
                       new Object[]{fieldName, tableName, e.getMessage()});
            throw e;
        }
    }

    public List<String> getDeviceTypesFromDB(String tableName) throws SQLException {
        try {
            return DatabaseUtils.getDeviceTypes(tableName);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving device types from table {0}: {1}", 
                       new Object[]{tableName, e.getMessage()});
            throw e;
        }
    }

    public List<DataEntry> loadAllDevices(String tableName) throws SQLException {
        List<DataEntry> devices = new ArrayList<>();
        try {
            List<HashMap<String, String>> deviceList = DatabaseUtils.loadDevices(tableName);
            String[] columns = DatabaseUtils.getInventoryColumnNames(tableName).toArray(new String[0]);
            for (HashMap<String, String> device : deviceList) {
                String[] values = new String[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    values[i] = device.getOrDefault(columns[i], "");
                }
                devices.add(new DataEntry(values, device));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading devices from table {0}: {1}", 
                       new Object[]{tableName, e.getMessage()});
            throw e;
        }
        return devices;
    }
}