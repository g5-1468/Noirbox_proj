package noirbox.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Creates the JDBC connection.
 * The URL, user and password are read from config/db.properties so they
 * are not hard-coded inside the Java source.
 */
public final class DBConnection {

    private static final Properties CONFIG = loadConfig();

    private DBConnection() {
        // utility class - no objects needed
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        String[] paths = {"config/db.properties", "java-app/config/db.properties"};

        for (String path : paths) {
            try (InputStream input = new FileInputStream(path)) {
                properties.load(input);
                return properties;
            } catch (IOException ignored) {
                // try the next path
            }
        }
        throw new IllegalStateException(
                "config/db.properties not found. Run the program from the java-app folder.");
    }

    /** Opens a new connection to MySQL. Caller closes it (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL JDBC driver not found. Put mysql-connector-j-8.x.jar in the lib folder.", e);
        }
        return DriverManager.getConnection(
                CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.user"),
                CONFIG.getProperty("db.password"));
    }
}
