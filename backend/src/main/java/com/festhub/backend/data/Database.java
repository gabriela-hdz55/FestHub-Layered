package com.festhub.backend.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class Database {

    // Set using the environment variables DB_URL, DB_USER, and DB_PASSWORD.
    private static String url;
    private static String user;
    private static String password;

    private Database() {
    }

    // ---------------------------------------------------------
    // INITIALIZATION
    // ---------------------------------------------------------

    /**
     * Reads DB_URL, DB_USER and DB_PASSWORD and checks that the database
     * is reachable. Called once by BackendServer at startup.
     * Fails (and stops the backend) if a setting is missing or the
     * connection doesn't work.
     */
    public static void initialize() {
        url = requireEnv("DB_URL");
        user = requireEnv("DB_USER");
        password = requireEnv("DB_PASSWORD");

        try (Connection connection = connect()) {
            System.out.println("Connected to FestHub database.");
        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Failed to connect to FestHub database.", exception);
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }

    // ---------------------------------------------------------
    // CONNECTION
    // ---------------------------------------------------------

    // Opens a new connection to the database
    public static Connection connect() throws SQLException {
        if (url == null) {
            throw new IllegalStateException("Database.initialize() was not called");
        }
        return DriverManager.getConnection(url, user, password);
    }
}