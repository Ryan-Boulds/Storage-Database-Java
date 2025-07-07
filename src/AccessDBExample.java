// import java.sql.*;

// public class AccessDBExample {
//     public static void main(String[] args) {
//         try {
//             // Path to your Access database file
//             String dbPath = "C:/Users/ami6985/OneDrive - AISIN WORLD CORP/Documents/InventoryManagement.accdb";
//             String url = "jdbc:ucanaccess://" + dbPath;

//             Connection conn = DriverManager.getConnection(url);
//             System.out.println("✅ Successfully connected to the database!\n");

//             DatabaseMetaData meta = conn.getMetaData();

//             // Get all tables
//             ResultSet tables = meta.getTables(null, null, null, new String[]{"TABLE"});

//             while (tables.next()) {
//                 String tableName = tables.getString("TABLE_NAME");
//                 System.out.println("📦 Table: " + tableName);

//                 // Get columns for each table
//                 ResultSet columns = meta.getColumns(null, null, tableName, null);
//                 while (columns.next()) {
//                     String columnName = columns.getString("COLUMN_NAME");
//                     String columnType = columns.getString("TYPE_NAME");
//                     System.out.println("   🧱 Column: " + columnName + " (" + columnType + ")");
//                 }
//                 columns.close();
//                 System.out.println();
//             }

//             tables.close();
//             conn.close();
//         } catch (Exception e) {
//             System.out.println("❌ Failed to connect or retrieve schema.");
//             e.printStackTrace();
//         }
//     }
// }
