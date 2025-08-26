package utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class InventoryData {
    private static ArrayList<HashMap<String, String>> cables;
    private static ArrayList<HashMap<String, String>> accessories;
    private static ArrayList<HashMap<String, String>> chargers;

    public static ArrayList<HashMap<String, String>> getCables() {
        if (cables == null) {
            try {
                cables = DatabaseUtils.loadPeripherals("Cable");
            } catch (SQLException e) {
                cables = new ArrayList<>();
            }
        }
        return cables;
    }

    public static ArrayList<HashMap<String, String>> getAccessories() {
        if (accessories == null) {
            try {
                accessories = DatabaseUtils.loadPeripherals("Accessory");
            } catch (SQLException e) {
                accessories = new ArrayList<>();
            }
        }
        return accessories;
    }

    public static ArrayList<HashMap<String, String>> getChargers() {
        if (chargers == null) {
            try {
                chargers = DatabaseUtils.loadPeripherals("Charger");
            } catch (SQLException e) {
                chargers = new ArrayList<>();
            }
        }
        return chargers;
    }
}