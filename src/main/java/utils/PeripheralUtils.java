package utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class PeripheralUtils {
    public static ArrayList<String> getPeripheralTypes(ArrayList<HashMap<String, String>> peripherals) {
        ArrayList<String> types = new ArrayList<>();
        for (HashMap<String, String> peripheral : peripherals) {
            String type = peripheral.get("Peripheral_Type");
            if (type != null && !type.isEmpty() && !types.contains(type)) {
                types.add(type);
            }
        }
        return types;
    }

    public static int getPeripheralCount(String type, ArrayList<HashMap<String, String>> peripherals) {
        for (HashMap<String, String> peripheral : peripherals) {
            if (type.equals(peripheral.get("Peripheral_Type"))) {
                try {
                    return Integer.parseInt(peripheral.get("Count"));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public static void updatePeripheralCount(String peripheralType, int countDelta, ArrayList<HashMap<String, String>> peripherals, JLabel statusLabel) throws SQLException {
        String category = peripherals.equals(DatabaseUtils.loadPeripherals("Cable")) ? "Cable" : "Accessory";
        DatabaseUtils.updatePeripheral(peripheralType, countDelta, category);
        statusLabel.setText("Successfully updated " + peripheralType + " count");
    }

    public static void addNewPeripheralType(JTextField newTypeField, JComboBox<String> comboBox, JLabel statusLabel, ArrayList<HashMap<String, String>> peripherals, ArrayList<String> existingTypes) throws SQLException {
        String newType = newTypeField.getText().trim();
        if (newType.isEmpty()) {
            statusLabel.setText("Error: New type cannot be empty");
            return;
        }
        if (existingTypes.contains(newType)) {
            statusLabel.setText("Error: Type already exists");
            return;
        }
        String category = peripherals.equals(DatabaseUtils.loadPeripherals("Cable")) ? "Cable" : "Accessory";
        DatabaseUtils.updatePeripheral(newType, 0, category);
        existingTypes.add(newType);
        comboBox.addItem(newType);
        comboBox.setSelectedItem(newType);
        newTypeField.setText("");
        newTypeField.setVisible(false);
        statusLabel.setText("Successfully added new type: " + newType);
    }
}