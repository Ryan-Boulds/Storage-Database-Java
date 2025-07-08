import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class FileUtils {
    private static final String DEVICES_FILE = "inventory.txt";
    private static final String CABLES_FILE = "cables.txt";
    private static final String ACCESSORIES_FILE = "accessories.txt";
    private static final String TEMPLATES_FILE = "templates.txt";

    public static void loadDevices() {
        try {
            File file = new File(DEVICES_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(file.toPath()));
                String[] lines = content.split("\n");
                InventoryData.getDevices().clear();
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
                        InventoryData.getDevices().add(item);
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
            for (HashMap<String, String> device : InventoryData.getDevices()) {
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
                InventoryData.getCables().clear();
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
                    InventoryData.getCables().add(item);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading cables: " + e.getMessage());
        }
    }

    public static void saveCables() {
        try {
            StringBuilder sb = new StringBuilder();
            for (HashMap<String, String> cable : InventoryData.getCables()) {
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
                InventoryData.getAccessories().clear();
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
                    InventoryData.getAccessories().add(item);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading accessories: " + e.getMessage());
        }
    }

    public static void saveAccessories() {
        try {
            StringBuilder sb = new StringBuilder();
            for (HashMap<String, String> accessory : InventoryData.getAccessories()) {
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
                InventoryData.getTemplates().clear();
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
                    InventoryData.getTemplates().add(template);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading templates: " + e.getMessage());
        }
    }

    public static void saveTemplates() {
        try {
            StringBuilder sb = new StringBuilder();
            for (HashMap<String, String> template : InventoryData.getTemplates()) {
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

    static List<String[]> readCSVFile(File file) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}