import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.*;



public class UIUtils {
    private static final ArrayList<HashMap<String, String>> devices = new ArrayList<>();
    private static final ArrayList<HashMap<String, String>> cables = new ArrayList<>();
    private static final ArrayList<HashMap<String, String>> accessories = new ArrayList<>();
    private static final ArrayList<HashMap<String, String>> templates = new ArrayList<>();
    private static final String DEVICES_FILE = "inventory.txt";
    private static final String CABLES_FILE = "cables.txt";
    private static final String ACCESSORIES_FILE = "accessories.txt";
    private static final String TEMPLATES_FILE = "templates.txt";

    static {
        loadDevices();
        loadCables();
        loadAccessories();
        loadTemplates();
    }

    public static JLabel createAlignedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JTextField createFormattedTextField() {
        JTextField textField = new JTextField();
        textField.setPreferredSize(new Dimension(450, 30));
        textField.setMaximumSize(new Dimension(450, 30));
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        return textField;
    }

    public static JComboBox<String> createFormattedComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setPreferredSize(new Dimension(450, 30));
        comboBox.setMaximumSize(new Dimension(450, 30));
        comboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        return comboBox;
    }

    public static JButton createFormattedButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    public static JCheckBox createFormattedCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        return checkBox;
    }

    public static JPanel createFormattedDatePicker() {
        JPanel datePanel = new JPanel();
        datePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        datePanel.setMaximumSize(new Dimension(450, 30));
        datePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField dateField = new JTextField();
        dateField.setPreferredSize(new Dimension(400, 30));
        dateField.setMaximumSize(new Dimension(400, 30));
        dateField.setEditable(false);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        dateField.setText(dateFormat.format(new Date()));

        JButton calendarButton = new JButton("...");
        calendarButton.setPreferredSize(new Dimension(40, 30));
        calendarButton.setMaximumSize(new Dimension(40, 30));

        calendarButton.addActionListener(e -> {
            JDialog calendarDialog = new JDialog((Frame) null, "Select Date", true);
            calendarDialog.setLayout(new BorderLayout());
            calendarDialog.setSize(250, 200);
            calendarDialog.setLocationRelativeTo(datePanel);

            JPanel calendarPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            calendarPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            Calendar cal = Calendar.getInstance();

            JComboBox<String> monthCombo = new JComboBox<>(new String[]{
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            });
            monthCombo.setPreferredSize(new Dimension(200, 30));
            monthCombo.setMaximumSize(new Dimension(200, 30));
            monthCombo.setSelectedIndex(cal.get(Calendar.MONTH));

            JComboBox<Integer> dayCombo = new JComboBox<>();
            dayCombo.setPreferredSize(new Dimension(200, 30));
            dayCombo.setMaximumSize(new Dimension(200, 30));

            JComboBox<Integer> yearCombo = new JComboBox<>();
            for (int i = cal.get(Calendar.YEAR) - 10; i <= cal.get(Calendar.YEAR) + 10; i++) {
                yearCombo.addItem(i);
            }
            yearCombo.setPreferredSize(new Dimension(200, 30));
            yearCombo.setMaximumSize(new Dimension(200, 30));
            yearCombo.setSelectedItem(cal.get(Calendar.YEAR));

            updateDayCombo(dayCombo, yearCombo, monthCombo);
            ActionListener updateDays = e2 -> updateDayCombo(dayCombo, yearCombo, monthCombo);
            yearCombo.addActionListener(updateDays);
            monthCombo.addActionListener(updateDays);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");
            okButton.setPreferredSize(new Dimension(80, 30));
            cancelButton.setPreferredSize(new Dimension(80, 30));
            okButton.addActionListener(e2 -> {
                int year = (Integer) yearCombo.getSelectedItem();
                int month = monthCombo.getSelectedIndex();
                int day = (Integer) dayCombo.getSelectedItem();
                cal.set(year, month, day);
                dateField.setText(dateFormat.format(cal.getTime()));
                calendarDialog.dispose();
            });
            cancelButton.addActionListener(e2 -> calendarDialog.dispose());
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            calendarPanel.add(new JLabel("Month:"));
            calendarPanel.add(monthCombo);
            calendarPanel.add(new JLabel("Day:"));
            calendarPanel.add(dayCombo);
            calendarPanel.add(new JLabel("Year:"));
            calendarPanel.add(yearCombo);
            calendarDialog.add(calendarPanel, BorderLayout.CENTER);
            calendarDialog.add(buttonPanel, BorderLayout.SOUTH);
            calendarDialog.setVisible(true);
        });

        datePanel.add(dateField);
        datePanel.add(calendarButton);
        return datePanel;
    }

    private static void updateDayCombo(JComboBox<Integer> dayCombo, JComboBox<Integer> yearCombo, JComboBox<String> monthCombo) {
        dayCombo.removeAllItems();
        Calendar cal = Calendar.getInstance();
        cal.set((Integer) yearCombo.getSelectedItem(), monthCombo.getSelectedIndex(), 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            dayCombo.addItem(i);
        }
        dayCombo.setSelectedIndex(0);
    }

    public static JScrollPane createScrollableContentPanel(JPanel contentPanel) {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setBlockIncrement(60);
        scrollPane.setDoubleBuffered(true);
        scrollPane.addMouseWheelListener(e -> {
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            int unitsToScroll = e.getUnitsToScroll() * 10;
            int newPosition = verticalScrollBar.getValue() + unitsToScroll;
            verticalScrollBar.setValue(newPosition);
        });
        return scrollPane;
    }

    public static JFrame createMainFrame(String title, JPanel... tabs) {
        JFrame frame = new JFrame(title);
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        for (JPanel tab : tabs) {
            String tabName = tab.getClass().getSimpleName().replace("Tab", "");
            tabbedPane.addTab(tabName, tab);
        }

        frame.add(tabbedPane);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveDevices();
                saveCables();
                saveAccessories();
                saveTemplates();
                System.exit(0);
            }
        });

        return frame;
    }

    public static String getSelectedPorts(JCheckBox... ports) {
        StringBuilder sb = new StringBuilder();
        for (JCheckBox port : ports) {
            if (port.isSelected()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(port.getText());
            }
        }
        return sb.length() > 0 ? sb.toString() : "None";
    }

    public static String getDateFromPicker(JPanel datePicker) {
        for (Component comp : datePicker.getComponents()) {
            if (comp instanceof JTextField) {
                return ((JTextField) comp).getText();
            }
        }
        return "";
    }

    public static String capitalizeWords(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        return Arrays.stream(input.trim().split("\\s+"))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static String validateDevice(Map<String, String> data) {
        String deviceName = data.get("Device_Name");
        String serialNumber = data.get("Serial_Number");
        String purchaseCost = data.get("Purchase_Cost");
        String networkAddress = data.get("Network_Address");

        if (deviceName == null || deviceName.trim().isEmpty()) {
            return "Device Name is required";
        }
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            return "Serial Number is required";
        }
        for (HashMap<String, String> device : devices) {
            if (device.get("Device_Name").equals(deviceName)) {
                return "Device Name '" + deviceName + "' already exists";
            }
            if (device.get("Serial_Number").equals(serialNumber)) {
                return "Serial Number '" + serialNumber + "' already exists";
            }
        }
        if (purchaseCost != null && !purchaseCost.trim().isEmpty()) {
            try {
                Double.valueOf(purchaseCost);
            } catch (NumberFormatException e) {
                return "Purchase Cost must be a valid number";
            }
        }
        if (networkAddress != null && !networkAddress.trim().isEmpty()) {
            if (!Pattern.matches("^((\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|([0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}))$", networkAddress)) {
                return "Network Address must be a valid IP or MAC address";
            }
        }
        return null;
    }

    public static String validatePeripheral(Map<String, String> data) {
        String peripheralName = data.get("Peripheral_Type");
        String count = data.get("Count");
        if (peripheralName == null || peripheralName.trim().isEmpty()) {
            return "Peripheral Type is required";
        }
        if (count != null && !count.trim().isEmpty()) {
            try {
                int c = Integer.parseInt(count);
                if (c < 0) {
                    return "Count cannot be negative";
                }
            } catch (NumberFormatException e) {
                return "Count must be a valid number";
            }
        }
        return null;
    }

    public static void saveDevice(Map<String, String> data) {
        if (data.containsKey("Device_Name")) {
            data.put("Device_Name", capitalizeWords(data.get("Device_Name")));
        }
        if (data.containsKey("Peripheral_Type")) {
            data.put("Peripheral_Type", capitalizeWords(data.get("Peripheral_Type")));
        }
        if (data.containsKey("Device_Type")) {
            data.put("Device_Type", capitalizeWords(data.get("Device_Type")));
        }
        if (data.containsKey("Brand")) {
            data.put("Brand", capitalizeWords(data.get("Brand")));
        }
        if (data.containsKey("Model")) {
            data.put("Model", capitalizeWords(data.get("Model")));
        }
        devices.add(new HashMap<>(data));
        saveDevices();
    }

    public static void saveCable(Map<String, String> data) {
        cables.add(new HashMap<>(data));
        saveCables();
    }

    public static void saveAccessory(Map<String, String> data) {
        accessories.add(new HashMap<>(data));
        saveAccessories();
    }

    public static void saveTemplate(Map<String, String> data) {
        templates.add(new HashMap<>(data));
        saveTemplates();
    }

    public static ArrayList<HashMap<String, String>> getTemplates() {
        return templates;
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

    public static void loadDevices() {
        try {
            File file = new File(DEVICES_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(file.toPath()));
                String[] lines = content.split("\n");
                devices.clear();
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    HashMap<String, String> item = new HashMap<>();
                    String[] pairs = line.split(", ");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=", 2);
                        if (keyValue.length == 2) {
                            item.put(keyValue[0], keyValue[1]);
                        }
                    }
                    if (item.containsKey("Device_Name")) {
                        devices.add(item);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading devices: " + e.getMessage());
        }
    }

    public static void saveDevices() {
        try {
            StringBuilder sb = new StringBuilder();
            for (HashMap<String, String> device : devices) {
                sb.append(mapToString(device)).append("\n");
            }
            Files.write(new File(DEVICES_FILE).toPath(), sb.toString().trim().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Error saving devices: " + e.getMessage());
        }
    }

    public static void loadCables() {
        try {
            File file = new File(CABLES_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(file.toPath()));
                String[] lines = content.split("\n");
                cables.clear();
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    HashMap<String, String> item = new HashMap<>();
                    String[] pairs = line.split(", ");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=", 2);
                        if (keyValue.length == 2) {
                            item.put(keyValue[0], keyValue[1]);
                        }
                    }
                    cables.add(item);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading cables: " + e.getMessage());
        }
    }

    public static void saveCables() {
        try {
            StringBuilder sb = new StringBuilder();
            for (HashMap<String, String> cable : cables) {
                sb.append(mapToString(cable)).append("\n");
            }
            Files.write(new File(CABLES_FILE).toPath(), sb.toString().trim().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Error saving cables: " + e.getMessage());
        }
    }

    public static void loadAccessories() {
        try {
            File file = new File(ACCESSORIES_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(file.toPath()));
                String[] lines = content.split("\n");
                accessories.clear();
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    HashMap<String, String> item = new HashMap<>();
                    String[] pairs = line.split(", ");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=", 2);
                        if (keyValue.length == 2) {
                            item.put(keyValue[0], keyValue[1]);
                        }
                    }
                    accessories.add(item);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading accessories: " + e.getMessage());
        }
    }

    public static void saveAccessories() {
        try {
            StringBuilder sb = new StringBuilder();
            for (HashMap<String, String> accessory : accessories) {
                sb.append(mapToString(accessory)).append("\n");
            }
            Files.write(new File(ACCESSORIES_FILE).toPath(), sb.toString().trim().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Error saving accessories: " + e.getMessage());
        }
    }

    public static void loadTemplates() {
        try {
            File file = new File(TEMPLATES_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(file.toPath()));
                String[] lines = content.split("\n");
                templates.clear();
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    HashMap<String, String> template = new HashMap<>();
                    String[] pairs = line.split(", ");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=", 2);
                        if (keyValue.length == 2) {
                            template.put(keyValue[0], keyValue[1]);
                        }
                    }
                    templates.add(template);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading templates: " + e.getMessage());
        }
    }

    public static void saveTemplates() {
        try {
            StringBuilder sb = new StringBuilder();
            for (HashMap<String, String> template : templates) {
                sb.append(mapToString(template)).append("\n");
            }
            Files.write(new File(TEMPLATES_FILE).toPath(), sb.toString().trim().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Error saving templates: " + e.getMessage());
        }
    }

    private static String mapToString(HashMap<String, String> map) {
        return map.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

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
        newType = capitalizeWords(newType);
        HashMap<String, String> newPeripheral = new HashMap<>();
        newPeripheral.put("Peripheral_Type", newType);
        newPeripheral.put("Count", "0");
        peripherals.add(newPeripheral);
        existingTypes.add(newType.toLowerCase());
        comboBox.setModel(new DefaultComboBoxModel<>(existingTypes.toArray(new String[0])));
        if (peripherals == accessories) saveAccessories();
        else if (peripherals == cables) saveCables();
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
                if (peripherals == accessories) saveAccessories();
                else if (peripherals == cables) saveCables();
                statusLabel.setText(type + " updated. New count: " + newCount);
                return;
            }
        }
        if (delta > 0) {
            HashMap<String, String> newPeripheral = new HashMap<>();
            newPeripheral.put("Peripheral_Type", type);
            newPeripheral.put("Count", String.valueOf(delta));
            peripherals.add(newPeripheral);
            if (peripherals == accessories) saveAccessories();
            else if (peripherals == cables) saveCables();
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