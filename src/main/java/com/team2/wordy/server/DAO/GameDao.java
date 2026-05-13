package com.team2.wordy.server.DAO;

import com.team2.wordy.server.ServerDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameDao {

    public int createGame(String sessionId) {
        String sql = "INSERT INTO games (session_id, status) VALUES (?, 'WAITING')";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, sessionId);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Create Game error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return -1;
    }

    public Integer getWaitingGameId() {
        String sql = "SELECT game_id FROM games WHERE status = 'WAITING' ORDER BY game_id ASC LIMIT 1";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt("game_id");
        } catch (SQLException e) {
            System.err.println("Get Waiting Game error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return null;
    }

    public void addParticipant(int gameId, int playerId) {
        String sql = "INSERT IGNORE INTO game_participants (game_id, player_id) VALUES (?, ?)";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, playerId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Add Participant error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }
    }

    public void updateGameStatus(int gameId, String status, Integer winnerId) {
        String sql = winnerId != null
                ? "UPDATE games SET status = ?, overall_winner = ? WHERE game_id = ?"
                : "UPDATE games SET status = ? WHERE game_id = ?";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            if (winnerId != null) {
                pstmt.setInt(2, winnerId);
                pstmt.setInt(3, gameId);
            } else {
                pstmt.setInt(2, gameId);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Update Game Status error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }
    }

    public int createRound(int gameId, int roundNumber, String letters) {
        String sql = "INSERT INTO rounds (game_id, round_number, letters) VALUES (?, ?, ?)";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, roundNumber);
            pstmt.setString(3, letters);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Create Round error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return -1;
    }

    public void addWordSubmission(int roundId, int playerId, String word, boolean isValid) {
        String sql = "INSERT INTO word_submissions (round_id, player_id, word, is_valid) VALUES (?, ?, ?, ?)";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roundId);
            pstmt.setInt(2, playerId);
            pstmt.setString(3, word);
            pstmt.setBoolean(4, isValid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Add Word Submission error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }
    }

    public List<String> getValidWordsForRound(int roundId) {
        List<String> words = new ArrayList<>();
        String sql = "SELECT word FROM word_submissions WHERE round_id = ? AND is_valid = 1";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roundId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    words.add(rs.getString("word"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Get Valid Words error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return words;
    }

    public void updateRoundWinner(int roundId, Integer winnerId, String winningWord) {
        String sql = "UPDATE rounds SET round_winner = ?, winning_word = ? WHERE round_id = ?";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (winnerId != null) {
                pstmt.setInt(1, winnerId);
                pstmt.setString(2, winningWord);
            } else {
                pstmt.setNull(1, Types.INTEGER);
                pstmt.setNull(2, Types.VARCHAR);
            }
            pstmt.setInt(3, roundId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Update Round Winner error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }
    }

    public void incrementParticipantWins(int gameId, int playerId) {
        String sql = "UPDATE game_participants SET round_wins = round_wins + 1 WHERE game_id = ? AND player_id = ?";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, playerId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Increment Participant Wins error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }
    }

    public int getParticipantWins(int gameId, int playerId) {
        String sql = "SELECT round_wins FROM game_participants WHERE game_id = ? AND player_id = ?";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, playerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("round_wins");
            }
        } catch (SQLException e) {
            System.err.println("Get Participant Wins error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return 0;
    }
}