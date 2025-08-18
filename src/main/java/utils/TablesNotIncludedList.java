package utils;

import java.util.Arrays;
import java.util.List;

//This class should make it easy to manage tables that are not included in tabs like Software Importer or Other Tab.
//It provides a centralized way to access the lists of excluded tables, making it easier to maintain and update.
//This is useful for keeping the code clean and avoiding hardcoded table names scattered throughout the codebase.
public class TablesNotIncludedList {
    private static final List<String> EXCLUDED_TABLES_FOR_SOFTWARE_IMPORTER = Arrays.asList(
        "Inventory", "Accessories", "Adapters", "Cables", "Templates", "LicenseKeyRules"
    );
    

    
    private static final List<String> INCLUDED_TABLES_FOR_INVENTORY = Arrays.asList(
        "Computers", "Printers", "Docks", "Switches"
    );

    //This is an example of another list for a different tab, such as "Other Tab".
    // private static final List<String> EXCLUDED_TABLES_FOR_OTHER_TAB = Arrays.asList(
    //     "OtherTable1", "OtherTable2"
    // );

    public static List<String> getExcludedTablesForSoftwareImporter() {
        return EXCLUDED_TABLES_FOR_SOFTWARE_IMPORTER;
    }

    public static List<String> getIncludedTablesForInventory() {
        return INCLUDED_TABLES_FOR_INVENTORY;
    }
    
    // This method can be used to get the list of excluded tables for other tabs if needed.
    // public static List<String> getExcludedTablesForOtherTab() {
    //     return EXCLUDED_TABLES_FOR_OTHER_TAB;
    // }
}