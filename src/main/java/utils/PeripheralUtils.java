package utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class PeripheralUtils {
    public static ArrayList<String> getPeripheralTypes(ArrayList<HashMap<String, String>> peripherals, String category) {
        ArrayList<String> types = new ArrayList<>();
        String typeColumn;
        switch (category) {
            case "Cable":
                typeColumn = "Cable_Type";
                break;
            case "Accessory":
                typeColumn = "Peripheral_Type";
                break;
            case "Adapter":
                typeColumn = "Adapter_Type";
                break;
            case "Charger":
                typeColumn = "Charger_Type";
                break;
            default:
                return types;
        }
        for (HashMap<String, String> peripheral : peripherals) {
            String type = peripheral.get(typeColumn);
            if (type != null && !type.isEmpty() && !types.contains(type)) {
                types.add(type);
            }
        }
        return types;
    }

    public static int getPeripheralCount(String type, ArrayList<HashMap<String, String>> peripherals, String category) {
        String typeColumn;
        switch (category) {
            case "Cable":
                typeColumn = "Cable_Type";
                break;
            case "Accessory":
                typeColumn = "Peripheral_Type";
                break;
            case "Adapter":
                typeColumn = "Adapter_Type";
                break;
            case "Charger":
                typeColumn = "Charger_Type";
                break;
            default:
                return 0;
        }
        for (HashMap<String, String> peripheral : peripherals) {
            if (type.equals(peripheral.get(typeColumn))) {
                try {
                    return Integer.parseInt(peripheral.get("Count"));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public static void updatePeripheralCount(String peripheralType, int countDelta, ArrayList<HashMap<String, String>> peripherals, String category, JLabel statusLabel) throws SQLException {
        DatabaseUtils.updatePeripheral(peripheralType, countDelta, category);
        statusLabel.setText("Successfully updated " + peripheralType + " count");
    }

    public static void addNewPeripheralType(JTextField newTypeField, JComboBox<String> comboBox, JLabel statusLabel, ArrayList<HashMap<String, String>> peripherals, ArrayList<String> existingTypes, String category) throws SQLException {
        String newType = newTypeField.getText().trim();
        if (newType.isEmpty()) {
            statusLabel.setText("Error: New type cannot be empty");
            return;
        }
        if (existingTypes.contains(newType)) {
            statusLabel.setText("Error: Type already exists");
            return;
        }
        DatabaseUtils.updatePeripheral(newType, 0, category);
        existingTypes.add(newType);
        comboBox.addItem(newType);
        comboBox.setSelectedItem(newType);
        newTypeField.setText("");
        newTypeField.setVisible(false);
        statusLabel.setText("Successfully added new type: " + newType);
    }
}