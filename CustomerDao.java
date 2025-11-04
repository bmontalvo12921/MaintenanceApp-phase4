import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDao {

    // create table
    public static void ensureTable() throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS customers (
                  phone TEXT PRIMARY KEY,
                  name TEXT NOT NULL,
                  address TEXT NOT NULL,
                  email TEXT
                )
            """);
        }
    }

    // insert (ignore on dup)
    public static boolean insert(Customer c) throws SQLException {
        String sql = "INSERT OR IGNORE INTO customers(phone,name,address,email) VALUES(?,?,?,?)";
        try (Connection cn = ConnectionManager.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getPhoneNumber());
            ps.setString(2, c.getName());
            ps.setString(3, c.getAddress());
            ps.setString(4, c.getEmail());
            return ps.executeUpdate() > 0;
        }
    }

    // update
    public static boolean update(Customer c) throws SQLException {
        String sql = "UPDATE customers SET name=?, address=?, email=? WHERE phone=?";
        try (Connection cn = ConnectionManager.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getAddress());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getPhoneNumber());
            return ps.executeUpdate() > 0;
        }
    }

    // delete
    public static boolean delete(String phone) throws SQLException {
        String sql = "DELETE FROM customers WHERE phone=?";
        try (Connection cn = ConnectionManager.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, phone);
            return ps.executeUpdate() > 0;
        }
    }

    // find by phone
    public static Customer find(String phone) throws SQLException {
        String sql = "SELECT phone,name,address,email FROM customers WHERE phone=?";
        try (Connection cn = ConnectionManager.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                            rs.getString(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4)
                    );
                }
                return null;
            }
        }
    }

    // list all
    public static List<Customer> listAll() throws SQLException {
        String sql = "SELECT phone,name,address,email FROM customers ORDER BY name";
        List<Customer> out = new ArrayList<>();
        try (Connection cn = ConnectionManager.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Customer(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4)
                ));
            }
        }
        return out;
    }
}
