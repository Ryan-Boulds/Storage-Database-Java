package utils;

import java.util.HashMap;

public class DataEntry {
    private final String[] values;
    private final HashMap<String, String> data;
    private boolean isResolved; // New field to track resolved status

    public DataEntry(String[] values, HashMap<String, String> data) {
        this.values = values;
        this.data = data;
        this.isResolved = false; // Default to false
    }

    public String[] getValues() {
        return values;
    }

    public HashMap<String, String> getData() {
        return data;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        this.isResolved = resolved;
    }
}