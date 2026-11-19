import java.sql.*;
import java.util.*;

public class main {
    public static void main(String[] args) {
        Connection con = null;
        Statement st = null;
        Scanner sc = new Scanner(System.in);

        try {
            // (a) Database Connection
            Class.forName("com.mysql.cj.jdbc.Driver");

            con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/ecommerce_db",
                "root",
                "Admin@123"
            );

            st = con.createStatement();

            // Create the Orders table if it does not already exist.
            // Existing records are preserved (no TRUNCATE / DROP).
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Orders (" +
                "order_id INT PRIMARY KEY, " +
                "customer_name VARCHAR(100), " +
                "product_name VARCHAR(100), " +
                "quantity INT, " +
                "unit_price DOUBLE, " +
                "status VARCHAR(30))"
            );

            int choice;
            do {
                System.out.println("\n========== ORDER MANAGEMENT SYSTEM ==========");
                System.out.println();
                System.out.println("1. Insert Order Records");
                System.out.println("2. Search Orders by Status");
                System.out.println("3. Increase Order Quantity");
                System.out.println("4. Search and Update Order Status");
                System.out.println("5. Display Total Order Value");
                System.out.println("6. Delete Cancelled Order");
                System.out.println("0. Exit");
                System.out.println();
                System.out.print("Enter your choice: ");

                choice = -1;
                try {
                    choice = Integer.parseInt(sc.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a numeric choice.");
                }

                switch (choice) {
                    case 1:
                        insertOrders(con, sc);
                        break;
                    case 2:
                        searchOrdersByStatus(con, sc);
                        break;
                    case 3:
                        increaseQuantity(con, sc);
                        break;
                    case 4:
                        updateOrderStatus(con, sc);
                        break;
                    case 5:
                        displayTotalOrderValue(con);
                        break;
                    case 6:
                        deleteCancelledOrder(con, sc);
                        break;
                    case 0:
                        System.out.println("Exiting Order Management System. Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a valid option.");
                }
            } while (choice != 0);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            // Close JDBC resources and Scanner when the application exits.
            try {
                if (st != null) {
                    st.close();
                }
            } catch (SQLException e) {
                // ignore
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                // ignore
            }
            sc.close();
        }
    }

    // (a) Insert Order Records using PreparedStatement with duplicate ID handling.
    static void insertOrders(Connection con, Scanner sc) {
        System.out.println("\n========== INSERT ORDER RECORDS ==========");
        System.out.println();

        // Sample order records (at least three as required).
        int[] ids = {101, 102, 103};
        String[] customers = {"Rahul", "Aman", "Priya"};
        String[] products = {"Laptop", "Mouse", "Keyboard"};
        int[] quantities = {4, 5, 1};
        double[] unitPrices = {55000.0, 500.0, 1200.0};
        String[] statuses = {"Pending", "Pending", "Cancelled"};

        String sql = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        for (int i = 0; i < ids.length; i++) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, ids[i]);
                ps.setString(2, customers[i]);
                ps.setString(3, products[i]);
                ps.setInt(4, quantities[i]);
                ps.setDouble(5, unitPrices[i]);
                ps.setString(6, statuses[i]);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("Order ID: " + ids[i] + " | Order inserted successfully");
                }
            } catch (SQLException e) {
                // MySQL error code 1062 = duplicate entry for PRIMARY KEY
                if (e.getErrorCode() == 1062) {
                    System.out.println("Duplicate Order ID: " + ids[i]);
                } else {
                    System.out.println("Error inserting Order ID " + ids[i] + ": " + e.getMessage());
                }
            }
        }
    }

    // (b) Conditional Retrieval and Sorting (status = ? AND quantity > 2, order by unit_price DESC).
    static void searchOrdersByStatus(Connection con, Scanner sc) {
        System.out.println("\n========== CONDITIONAL ORDER RETRIEVAL ==========");
        System.out.println();

        System.out.print("Enter Order Status: ");
        String status = sc.nextLine().trim();

        String sql = "SELECT * FROM Orders WHERE status = ? AND quantity > 2 ORDER BY unit_price DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                System.out.println();
                System.out.println("Orders with quantity greater than 2 (Price: High to Low):");
                System.out.println();

                while (rs.next()) {
                    found = true;
                    System.out.print("ID: " + rs.getInt("order_id"));
                    System.out.print(" | Customer: " + rs.getString("customer_name"));
                    System.out.print(" | Product: " + rs.getString("product_name"));
                    System.out.print(" | Quantity: " + rs.getInt("quantity"));
                    System.out.print(" | Unit Price: " + rs.getDouble("unit_price"));
                    System.out.println(" | Status: " + rs.getString("status"));
                }

                if (!found) {
                    System.out.println("No orders found with the given status and quantity greater than 2.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // (c) Increase Order Quantity (quantity = quantity + additional, never replace).
    static void increaseQuantity(Connection con, Scanner sc) {
        System.out.println("\n========== UPDATE ORDER QUANTITY ==========");
        System.out.println();

        Integer id = readInt(sc, "Enter Order ID: ");
        if (id == null) {
            return;
        }

        Integer additional = readInt(sc, "Enter additional quantity: ");
        if (additional == null) {
            return;
        }

        if (additional <= 0) {
            System.out.println("Additional quantity must be a positive number.");
            return;
        }

        // Check whether the order exists before updating.
        String checkSql = "SELECT quantity FROM Orders WHERE order_id = ?";
        boolean exists = false;
        try (PreparedStatement ps = con.prepareStatement(checkSql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    exists = true;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        if (!exists) {
            System.out.println("Order ID does not exist.");
            return;
        }

        String updateSql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(updateSql)) {
            ps.setInt(1, additional);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println();
                System.out.println("Order quantity updated successfully");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // (d) Search and Update Order Status (only Pending / Shipped / Delivered allowed).
    static void updateOrderStatus(Connection con, Scanner sc) {
        System.out.println("\n========== SEARCH AND MODIFY ORDER STATUS ==========");
        System.out.println();

        Integer id = readInt(sc, "Enter Order ID: ");
        if (id == null) {
            return;
        }

        // Retrieve the corresponding order details.
        String selectSql = "SELECT * FROM Orders WHERE order_id = ?";
        String currentStatus = null;
        try (PreparedStatement ps = con.prepareStatement(selectSql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Order ID does not exist.");
                    return;
                }
                currentStatus = rs.getString("status");
                System.out.println("Order Found!");
                System.out.print("ID: " + rs.getInt("order_id"));
                System.out.print(" | Customer: " + rs.getString("customer_name"));
                System.out.print(" | Product: " + rs.getString("product_name"));
                System.out.print(" | Quantity: " + rs.getInt("quantity"));
                System.out.print(" | Unit Price: " + rs.getDouble("unit_price"));
                System.out.println(" | Status: " + currentStatus);
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        System.out.print("Enter new status: ");
        String newStatus = sc.nextLine().trim();

        String normalized = normalizeStatus(newStatus);
        if (normalized == null) {
            System.out.println("Invalid status. Valid statuses are: Pending, Shipped, Delivered.");
            return;
        }

        String updateSql = "UPDATE Orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(updateSql)) {
            ps.setString(1, normalized);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println();
                System.out.println("Order status updated successfully");
                // Retrieve the record again and display complete updated details.
                displayOrder(con, id);
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Normalize user-entered status to one of the valid values (case-insensitive).
    static String normalizeStatus(String s) {
        if (s == null) {
            return null;
        }
        String lower = s.toLowerCase();
        if (lower.equals("pending")) {
            return "Pending";
        }
        if (lower.equals("shipped")) {
            return "Shipped";
        }
        if (lower.equals("delivered")) {
            return "Delivered";
        }
        return null;
    }

    // (e-1) Aggregate Query: total value using SQL SUM(quantity * unit_price).
    static void displayTotalOrderValue(Connection con) {
        System.out.println("\n========== TOTAL ORDER VALUE ==========");
        System.out.println();

        String sql = "SELECT SUM(quantity * unit_price) AS total FROM Orders";

        try (Statement localSt = con.createStatement();
             ResultSet rs = localSt.executeQuery(sql)) {
            if (rs.next()) {
                double total = rs.getDouble("total");
                System.out.println("Total Order Value: " + total);
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // (e-2) Conditional Deletion: delete only if status is Cancelled.
    static void deleteCancelledOrder(Connection con, Scanner sc) {
        System.out.println("\n========== CONDITIONAL ORDER DELETION ==========");
        System.out.println();

        Integer id = readInt(sc, "Enter Order ID to delete: ");
        if (id == null) {
            return;
        }

        // Retrieve order details and check existence.
        String selectSql = "SELECT * FROM Orders WHERE order_id = ?";
        String currentStatus = null;
        try (PreparedStatement ps = con.prepareStatement(selectSql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Order ID does not exist");
                    return;
                }
                currentStatus = rs.getString("status");
                System.out.println("Order Found!");
                System.out.println("Status: " + currentStatus);
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        // Delete only if the current status is Cancelled.
        if (!"Cancelled".equalsIgnoreCase(currentStatus)) {
            System.out.println("Order cannot be deleted because its status is " + currentStatus);
            return;
        }

        String deleteSql = "DELETE FROM Orders WHERE order_id = ? AND status = 'Cancelled'";
        try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println();
                System.out.println("Order deleted successfully");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Display complete order details for a given order ID.
    static void displayOrder(Connection con, int id) {
        String sql = "SELECT * FROM Orders WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println();
                    System.out.println("Updated Order Details:");
                    System.out.println();
                    System.out.print("ID: " + rs.getInt("order_id"));
                    System.out.print(" | Customer: " + rs.getString("customer_name"));
                    System.out.print(" | Product: " + rs.getString("product_name"));
                    System.out.print(" | Quantity: " + rs.getInt("quantity"));
                    System.out.print(" | Unit Price: " + rs.getDouble("unit_price"));
                    System.out.println(" | Status: " + rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Read an integer from the console; returns null on invalid input.
    static Integer readInt(Scanner sc, String prompt) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number entered.");
            return null;
        }
    }
}
