package utils;
import java.util.*;
import javax.swing.*;

public class PeripheralUtils {
    public static ArrayList<String> getPeripheralTypes(ArrayList<HashMap<String, String>> peripherals) {
        ArrayList<String> types = new ArrayList<>();
        for (HashMap<String, String> peripheral : peripherals) {
            String type = peripheral.getOrDefault("Peripheral_Type", "");
            if (!type.isEmpty() && !types.contains(type) && !type.equalsIgnoreCase("headset")) {
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
        String capitalizedType = DataUtils.capitalizeWords(newType);
        if (existingTypes.stream().anyMatch(t -> t.equalsIgnoreCase(capitalizedType))) {
            statusLabel.setText("Error: " + capitalizedType + " already exists");
            return;
        }
        HashMap<String, String> newPeripheral = new HashMap<>();
        newPeripheral.put("Peripheral_Type", capitalizedType);
        newPeripheral.put("Count", "0");
        peripherals.add(newPeripheral);
        existingTypes.add(capitalizedType);
        comboBox.setModel(new DefaultComboBoxModel<>(existingTypes.toArray(new String[] {})));
        if (peripherals == InventoryData.getAccessories()) FileUtils.saveAccessories();
        else if (peripherals == InventoryData.getCables()) FileUtils.saveCables();
        newTypeField.setText("");
        newTypeField.setVisible(false);
        statusLabel.setText(capitalizedType + " added with count 0");
    }

    public static void updatePeripheralCount(String type, int delta, ArrayList<HashMap<String, String>> peripherals, JLabel statusLabel) {
        String capitalizedType = DataUtils.capitalizeWords(type);
        for (HashMap<String, String> peripheral : peripherals) {
            if (peripheral.getOrDefault("Peripheral_Type", "").equals(capitalizedType)) {
                int currentCount = Integer.parseInt(peripheral.getOrDefault("Count", "0"));
                int newCount = currentCount + delta;
                if (newCount < 0) {
                    statusLabel.setText("Error: Cannot reduce " + capitalizedType + " below 0");
                    return;
                }
                peripheral.put("Count", String.valueOf(newCount));
                if (peripherals == InventoryData.getAccessories()) FileUtils.saveAccessories();
                else if (peripherals == InventoryData.getCables()) FileUtils.saveCables();
                statusLabel.setText(capitalizedType + " updated. New count: " + newCount);
                return;
            }
        }
        if (delta > 0) {
            HashMap<String, String> newPeripheral = new HashMap<>();
            newPeripheral.put("Peripheral_Type", capitalizedType);
            newPeripheral.put("Count", String.valueOf(delta));
            peripherals.add(newPeripheral);
            if (peripherals == InventoryData.getAccessories()) FileUtils.saveAccessories();
            else if (peripherals == InventoryData.getCables()) FileUtils.saveCables();
            statusLabel.setText(capitalizedType + " added. New count: " + delta);
        } else {
            statusLabel.setText("Error: Cannot reduce " + capitalizedType + " below 0");
        }
    }

    public static int getPeripheralCount(String type, ArrayList<HashMap<String, String>> peripherals) {
        String capitalizedType = DataUtils.capitalizeWords(type);
        for (HashMap<String, String> peripheral : peripherals) {
            if (peripheral.getOrDefault("Peripheral_Type", "").equals(capitalizedType)) {
                return Integer.parseInt(peripheral.getOrDefault("Count", "0"));
            }
        }
        return 0;
    }
}