package com.team2.wordy.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

public class ServerDatabase {

    private static final String URL      = "jdbc:mysql://localhost:3306/wordy_game_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private static final int POOL_SIZE = 5;
    private static final Queue<Connection> connectionPool = new LinkedList<>();

    // Initialize connection pool
    static {
        try {
            for (int i = 0; i < POOL_SIZE; i++) {
                connectionPool.add(createConnection());
            }
            System.out.println("Connection pool initialized (" + POOL_SIZE + " connections).");
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing connection pool", e);
        }
    }

    // Create new DB connection
    private static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    // Get connection from pool
    public static synchronized Connection getConnection() throws SQLException {
        Connection con;

        if (connectionPool.isEmpty()) {
            con = createConnection();
        } else {
            con = connectionPool.poll();
        }

        // Validate connection (avoid stale connections)
        if (con != null && con.isClosed()) {
            con = createConnection();
        }

        return con;
    }

    // Return connection back to pool
    public static synchronized void releaseConnection(Connection con) {
        if (con != null) {
            try {
                if (!con.isClosed()) {
                    connectionPool.offer(con);
                }
            } catch (SQLException e) {
                System.out.println("Connection already closed, not returned to pool.");
            }
        }
    }

    // Proper cleanup
    public static void shutdown() {
        while (!connectionPool.isEmpty()) {
            try {
                connectionPool.poll().close();
            } catch (SQLException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
        System.out.println("All pool connections closed.");
    }
}