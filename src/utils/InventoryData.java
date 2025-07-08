package utils;
import java.util.*;

public class InventoryData {
    private static final ArrayList<HashMap<String, String>> devices = new ArrayList<>();
    private static final ArrayList<HashMap<String, String>> cables = new ArrayList<>();
    private static final ArrayList<HashMap<String, String>> accessories = new ArrayList<>();
    private static final ArrayList<HashMap<String, String>> templates = new ArrayList<>();

    static {
        FileUtils.loadDevices();
        FileUtils.loadCables();
        FileUtils.loadAccessories();
        FileUtils.loadTemplates();
    }

    public static ArrayList<HashMap<String, String>> getDevices() {
        return devices;
    }

    public static ArrayList<HashMap<String, String>> getCables() {
        return cables;
    }

    public static ArrayList<HashMap<String, String>> getAccessories() {
        return accessories;
    }

    public static ArrayList<HashMap<String, String>> getTemplates() {
        return templates;
    }

    public static void saveDevice(Map<String, String> data) {
        if (data.containsKey("Device_Name")) {
            data.put("Device_Name", DataUtils.capitalizeWords(data.get("Device_Name")));
        }
        if (data.containsKey("Peripheral_Type")) {
            data.put("Peripheral_Type", DataUtils.capitalizeWords(data.get("Peripheral_Type")));
        }
        if (data.containsKey("Device_Type")) {
            data.put("Device_Type", DataUtils.capitalizeWords(data.get("Device_Type")));
        }
        if (data.containsKey("Brand")) {
            data.put("Brand", DataUtils.capitalizeWords(data.get("Brand")));
        }
        if (data.containsKey("Model")) {
            data.put("Model", DataUtils.capitalizeWords(data.get("Model")));
        }
        devices.add(new HashMap<>(data));
        FileUtils.saveDevices();
    }

    public static void saveCable(Map<String, String> data) {
        cables.add(new HashMap<>(data));
        FileUtils.saveCables();
    }

    public static void saveAccessory(Map<String, String> data) {
        accessories.add(new HashMap<>(data));
        FileUtils.saveAccessories();
    }

    public static void saveTemplate(Map<String, String> data) {
        templates.add(new HashMap<>(data));
        FileUtils.saveTemplates();
    }
}