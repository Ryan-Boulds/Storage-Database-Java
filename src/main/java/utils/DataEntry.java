package utils;

import java.util.HashMap;

public class DataEntry {
    private final String[] values;
    private final HashMap<String, String> data;

    public DataEntry(String[] values, HashMap<String, String> data) {
        this.values = values;
        this.data = data;
    }

    public String[] getValues() {
        return values;
    }

    public HashMap<String, String> getData() {
        return data;
    }
}