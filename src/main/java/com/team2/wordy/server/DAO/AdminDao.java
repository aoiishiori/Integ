package com.team2.wordy.server.DAO;

import com.team2.wordy.proto.PlayerRecord;
import com.team2.wordy.server.ServerDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminDao {

    // Admin token validation
    public int validateAdminToken(String token) {
        if (token == null || token.isBlank()) {
            return -1;
        }

        String sql = "SELECT user_id FROM users "
                + "WHERE session_token = ? AND role = 'ADMIN' AND is_archived = 0";

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, token);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }

        } catch (SQLException e) {
            System.err.println("Validating Admin Token error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return -1;
    }

    // Create Player
    public PlayerRecord createPlayer(String username, String password, String role) {
        String checkSql = "SELECT user_id FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = ServerDatabase.getConnection()) {
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return null;
                    }
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.setString(3, role);

                int affectedRows = insertStmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int newId = rs.getInt(1);
                            return PlayerRecord.newBuilder()
                                    .setId(newId)
                                    .setUsername(username)
                                    .setRole(role)
                                    .setArchived(false) // FIXED
                                    .build();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Create Player error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return null;
    }

    // Read (Get) All Players
    public List<PlayerRecord> getAllPlayers(boolean includeArchived) {
        List<PlayerRecord> results = new ArrayList<>();
        String sql = includeArchived
                ? "SELECT user_id, username, role, is_archived FROM users"
                : "SELECT user_id, username, role, is_archived FROM users WHERE is_archived = 0";

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                results.add(mapRowToPlayerRecord(rs));
            }

        } catch (SQLException e) {
            System.err.println("Get All Players error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return results;
    }

    // Update Player
    public PlayerRecord updatePlayer(int playerId, String newUsername, String newPassword, String newRole, boolean activate) {
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        boolean hasUpdate = false;
        List<String> params = new ArrayList<>();

        if (!newUsername.isBlank()) {
            sql.append("username = ?, ");
            params.add(newUsername);
            hasUpdate = true;
        }
        if (!newPassword.isBlank()) {
            sql.append("password = ?, ");
            params.add(newPassword);
            hasUpdate = true;
        }
        if (!newRole.isBlank()) {
            sql.append("role = ?, ");
            params.add(newRole);
            hasUpdate = true;
        }
        if (activate) {
            sql.append("is_archived = 0, ");
            hasUpdate = true;
        }

        if (!hasUpdate) {
            return readPlayer(playerId, true);
        }

        sql.setLength(sql.length() - 2);
        sql.append(" WHERE user_id = ?");

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, params.get(i));
            }
            pstmt.setInt(params.size() + 1, playerId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                return readPlayer(playerId, true);
            }

        } catch (SQLException e) {
            System.err.println("Update Player error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return null;
    }

    // Delete (Archive) Player
    public String archivePlayer(int playerId) {
        String sql = "UPDATE users SET is_archived = 1, session_token = NULL WHERE user_id = ? AND is_archived = 0";
        String getUsernameSql = "SELECT username FROM users WHERE user_id = ?";

        try (Connection conn = ServerDatabase.getConnection()) {

            // Get username before archiving
            String archivedUsername = null;
            try (PreparedStatement getStmt = conn.prepareStatement(getUsernameSql)) {
                getStmt.setInt(1, playerId);
                try (ResultSet rs = getStmt.executeQuery()) {
                    if (rs.next()) {
                        archivedUsername = rs.getString("username");
                    }
                }
            }

            // Archive the player
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, playerId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    return archivedUsername; // Return the username if successfully archived
                }
            }

        } catch (SQLException e) {
            System.err.println("Archive Player error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return null;
    }

    // Read player using ID
    public PlayerRecord readPlayer(int playerId, boolean includeArchived) {
        String sql = includeArchived
                ? "SELECT user_id, username, role, is_archived FROM users WHERE user_id = ?"
                : "SELECT user_id, username, role, is_archived FROM users WHERE user_id = ? AND is_archived = 0";

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPlayerRecord(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Read Player error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return null;
    }

    // Search players using their username
    public List<PlayerRecord> searchPlayer(String query, boolean includeArchived) {
        List<PlayerRecord> results = new ArrayList<>();
        boolean hasQuery = query != null && !query.isBlank();

        String sql;
        if (hasQuery && includeArchived) {
            sql = "SELECT user_id, username, role, is_archived FROM users WHERE username LIKE ?";
        } else if (hasQuery) {
            sql = "SELECT user_id, username, role, is_archived FROM users WHERE username LIKE ? AND is_archived = 0";
        } else if (includeArchived) {
            sql = "SELECT user_id, username, role, is_archived FROM users";
        } else {
            sql = "SELECT user_id, username, role, is_archived FROM users WHERE is_archived = 0";
        }

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (hasQuery) {
                pstmt.setString(1, "%" + query + "%");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToPlayerRecord(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Search Player error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return results;
    }

    // Check count of online players (Not NULL = online)
    public int countOnlinePlayers() {
        String sql = "SELECT COUNT(*) AS cnt FROM users WHERE session_token IS NOT NULL AND is_archived = 0";

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("cnt");
            }

        } catch (SQLException e) {
            System.err.println("Count Online Players error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return 0;
    }

    // Check count of offline players (NULL = offline)
    public int countOfflinePlayers() {
        String sql = "SELECT COUNT(*) AS cnt FROM users WHERE session_token IS NULL AND is_archived = 0";

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("cnt");
            }

        } catch (SQLException e) {
            System.err.println("Count Offline Players error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return 0;
    }

    // Row mapper
    private PlayerRecord mapRowToPlayerRecord(ResultSet rs) throws SQLException {
        return PlayerRecord.newBuilder()
                .setId(rs.getInt("user_id"))
                .setUsername(rs.getString("username"))
                .setRole(rs.getString("role"))
                .setArchived(rs.getBoolean("is_archived")) // FIXED
                .build();
    }
}