import java.util.*;
import javax.swing.*;
import utils.DataUtils;
import utils.FileUtils;
import utils.InventoryData;

public class PeripheralUtils {
    public static ArrayList<String> getPeripheralTypes(ArrayList<HashMap<String, String>> peripherals) {
        ArrayList<String> types = new ArrayList<>();
        for (HashMap<String, String> peripheral : peripherals) {
            String type = peripheral.getOrDefault("Peripheral_Type", "").toLowerCase();
            if (!type.isEmpty() && !types.contains(type) && !type.equals("headset")) {
                types.add(type);
            }
        }
        return types;
    }

    public static void addNewPeripheralType(JTextField newTypeField, JComboBox<String> comboBox, JLabel statusLabel, ArrayList<HashMap<String, String>> peripherals, ArrayList<String> existingTypes) {
        String newType = newTypeField.getText().trim();
        if (newType.isEmpty()) {
            statusLabel.setText("Error: Enter a new peripheral type");
            return;
        }
        if (existingTypes.contains(newType.toLowerCase())) {
            statusLabel.setText("Error: " + newType + " already exists");
            return;
        }
        newType = DataUtils.capitalizeWords(newType);
        HashMap<String, String> newPeripheral = new HashMap<>();
        newPeripheral.put("Peripheral_Type", newType);
        newPeripheral.put("Count", "0");
        peripherals.add(newPeripheral);
        existingTypes.add(newType.toLowerCase());
        comboBox.setModel(new DefaultComboBoxModel<>(existingTypes.toArray(new String[0])));
        if (peripherals == InventoryData.getAccessories()) FileUtils.saveAccessories();
        else if (peripherals == InventoryData.getCables()) FileUtils.saveCables();
        newTypeField.setText("");
        newTypeField.setVisible(false);
        statusLabel.setText(newType + " added with count 0");
    }

    public static void updatePeripheralCount(String type, int delta, ArrayList<HashMap<String, String>> peripherals, JLabel statusLabel) {
        for (HashMap<String, String> peripheral : peripherals) {
            if (peripheral.getOrDefault("Peripheral_Type", "").equals(type)) {
                int currentCount = Integer.parseInt(peripheral.getOrDefault("Count", "0"));
                int newCount = currentCount + delta;
                if (newCount < 0) newCount = 0;
                peripheral.put("Count", String.valueOf(newCount));
                if (peripherals == InventoryData.getAccessories()) FileUtils.saveAccessories();
                else if (peripherals == InventoryData.getCables()) FileUtils.saveCables();
                statusLabel.setText(type + " updated. New count: " + newCount);
                return;
            }
        }
        if (delta > 0) {
            HashMap<String, String> newPeripheral = new HashMap<>();
            newPeripheral.put("Peripheral_Type", type);
            newPeripheral.put("Count", String.valueOf(delta));
            peripherals.add(newPeripheral);
            if (peripherals == InventoryData.getAccessories()) FileUtils.saveAccessories();
            else if (peripherals == InventoryData.getCables()) FileUtils.saveCables();
            statusLabel.setText(type + " added. New count: " + delta);
        }
    }

    public static int getPeripheralCount(String type, ArrayList<HashMap<String, String>> peripherals) {
        for (HashMap<String, String> peripheral : peripherals) {
            if (peripheral.getOrDefault("Peripheral_Type", "").equals(type)) {
                return Integer.parseInt(peripheral.getOrDefault("Count", "0"));
            }
        }
        return 0;
    }
}