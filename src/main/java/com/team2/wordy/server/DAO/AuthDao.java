package com.team2.wordy.server.DAO;

import com.team2.wordy.server.ServerDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AuthDao {

    // Inner class to hold user data returned from DB
    public static class UserInfo {
        public final int userId;
        public final String username;
        public final String role;
        public final boolean isArchived;

        public UserInfo(int userId, String username, String role, boolean isArchived) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.isArchived = isArchived;
        }
    }

    // Verify username and password against the database. Returns user info if valid, null otherwise
    public UserInfo verifyCredentials(String username, String password) {
        String sql = "SELECT user_id, username, role, is_archived FROM users WHERE username = ? AND password = ?";

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new UserInfo(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getBoolean("is_archived")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("Verify Credentials error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return null;
    }

    // Hardcodes role to PLAYER to prevent admin account creation
    public UserInfo registerPlayer(String username, String password) {
        String checkSql = "SELECT user_id FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, password, role) VALUES (?, ?, 'PLAYER')";

        try (Connection conn = ServerDatabase.getConnection()) {

            // Check if username already exists
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return null; // Username already taken
                    }
                }
            }

            // Insert new player with hardcoded role
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);

                int affectedRows = insertStmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int newId = rs.getInt(1);
                            return new UserInfo(newId, username, "PLAYER", false);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Register Player error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return null;
    }

    // Update the session token in the database when a user logs in
    public boolean updateSessionToken(int userId, String token) {
        String sql = "UPDATE users SET session_token = ? WHERE user_id = ?";

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, token);
            pstmt.setInt(2, userId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Update Session Token error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return false;
    }

    // Set the session_token to NULL in the database when a user logs out
    public boolean clearSessionToken(int userId) {
        String sql = "UPDATE users SET session_token = NULL WHERE user_id = ?";

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Clear Session Token error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return false;
    }

    // Fetch a username by their user_id (is used for token sync on archive)
    public String getUsernameById(int userId) {
        String sql = "SELECT username FROM users WHERE user_id = ?";

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }

        } catch (SQLException e) {
            System.err.println("Get Username By ID error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return null;
    }
}