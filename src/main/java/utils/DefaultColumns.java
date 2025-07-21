package utils;

import java.util.HashMap;
import java.util.Map;

public class DefaultColumns {
    // Default column definitions for inventory data (name -> Access data type)
    private static final Map<String, String> INVENTORY_COLUMN_DEFINITIONS = new HashMap<>();
    static {
        INVENTORY_COLUMN_DEFINITIONS.put("AssetName", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("Proc", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("Processor", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("Memory", "DOUBLE");
        INVENTORY_COLUMN_DEFINITIONS.put("OS", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("Domain", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("SP", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("Username", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("IP_Address", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("Description", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("Manufacturer", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("Model", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("Location", "TEXT");
        INVENTORY_COLUMN_DEFINITIONS.put("Created_at", "DATE");
        INVENTORY_COLUMN_DEFINITIONS.put("Last_Successful_Scan", "DATE");
        INVENTORY_COLUMN_DEFINITIONS.put("Device_Type", "TEXT");
        // Optionally add Department and Status for FilterPanel.java
        // INVENTORY_COLUMN_DEFINITIONS.put("Department", "TEXT");
        // INVENTORY_COLUMN_DEFINITIONS.put("Status", "TEXT");
    }

    // Optional: Definitions for Accessories, Cables, Adapters
    private static final Map<String, String> ACCESSORIES_COLUMN_DEFINITIONS = new HashMap<>();
    static {
        ACCESSORIES_COLUMN_DEFINITIONS.put("Peripheral_Type", "TEXT");
        ACCESSORIES_COLUMN_DEFINITIONS.put("Count", "INTEGER");
    }

    private static final Map<String, String> CABLES_COLUMN_DEFINITIONS = new HashMap<>();
    static {
        CABLES_COLUMN_DEFINITIONS.put("Cable_Type", "TEXT");
        CABLES_COLUMN_DEFINITIONS.put("Count", "INTEGER");
    }

    private static final Map<String, String> ADAPTERS_COLUMN_DEFINITIONS = new HashMap<>();
    static {
        ADAPTERS_COLUMN_DEFINITIONS.put("Adapter_Type", "TEXT");
        ADAPTERS_COLUMN_DEFINITIONS.put("Count", "INTEGER");
    }

    public static Map<String, String> getInventoryColumnDefinitions() {
        return new HashMap<>(INVENTORY_COLUMN_DEFINITIONS);
    }

    public static String[] getInventoryColumns() {
        return INVENTORY_COLUMN_DEFINITIONS.keySet().toArray(new String[0]);
    }

    public static Map<String, String> getAccessoriesColumnDefinitions() {
        return new HashMap<>(ACCESSORIES_COLUMN_DEFINITIONS);
    }

    public static String[] getAccessoriesColumns() {
        return ACCESSORIES_COLUMN_DEFINITIONS.keySet().toArray(new String[0]);
    }

    public static Map<String, String> getCablesColumnDefinitions() {
        return new HashMap<>(CABLES_COLUMN_DEFINITIONS);
    }

    public static String[] getCablesColumns() {
        return CABLES_COLUMN_DEFINITIONS.keySet().toArray(new String[0]);
    }

    public static Map<String, String> getAdaptersColumnDefinitions() {
        return new HashMap<>(ADAPTERS_COLUMN_DEFINITIONS);
    }

    public static String[] getAdaptersColumns() {
        return ADAPTERS_COLUMN_DEFINITIONS.keySet().toArray(new String[0]);
    }
}