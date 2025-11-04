import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {

    // db path is set by GUI at startup
    private static String dbPath = null;

    // set by GUI
    public static void setDatabasePath(String path) { dbPath = path; }

    // get connection
    public static Connection getConnection() throws SQLException {
        if (dbPath == null || dbPath.isBlank())
            throw new SQLException("Database path not set");
        try {
            Class.forName("org.sqlite.JDBC"); // load driver
        } catch (ClassNotFoundException ignore) { }
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }
}
