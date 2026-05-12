package com.bankmgmt.database;

import com.bankmgmt.exceptions.DAOException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton JDBC bootstrap — loads {@code database.properties} from the classpath.
 */
public final class DatabaseConnection {

    private static volatile DatabaseConnection instance;
    private final Properties props = new Properties();

    private DatabaseConnection() {
        try (InputStream in = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (in == null) {
                throw new DAOException("database.properties missing from classpath");
            }
            props.load(in);
            Class.forName(props.getProperty("db.driver"));
        } catch (IOException | ClassNotFoundException e) {
            throw new DAOException("Failed to load JDBC configuration", e);
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.username");
        String password = props.getProperty("db.password", "");
        return DriverManager.getConnection(url, user, password);
    }
}
