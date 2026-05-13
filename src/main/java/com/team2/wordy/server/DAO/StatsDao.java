package com.team2.wordy.server.DAO;

import com.team2.wordy.proto.TopWinEntry;
import com.team2.wordy.proto.TopWordEntry;
import com.team2.wordy.server.ServerDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StatsDao {

    // Insert or Update if user already has a stats row
    public void incrementPlayerWins(int userId) {
        String sql = "INSERT INTO player_stats (user_id, total_wins) VALUES (?, 1) " +
                "ON DUPLICATE KEY UPDATE total_wins = total_wins + 1";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Increment Player Wins error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }
    }

    public void updateLongestWord(int userId, String word) {
        String sql = "INSERT INTO player_stats (user_id, longest_word, longest_word_length) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE longest_word = ?, longest_word_length = ?";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, word);
            pstmt.setInt(3, word.length());
            pstmt.setString(4, word);
            pstmt.setInt(5, word.length());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Update Longest Word error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }
    }

    public void addLongestWordRecord(String word, int userId) {
        String sql = "INSERT INTO longest_words_record (word, word_length, submitted_by) VALUES (?, ?, ?)";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, word);
            pstmt.setInt(2, word.length());
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Add Longest Word Record error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }
    }

    public List<TopWinEntry> getTopWinners() {
        List<TopWinEntry> list = new ArrayList<>();
        String sql = "SELECT u.username, ps.total_wins " +
                "FROM player_stats ps JOIN users u ON ps.user_id = u.user_id " +
                "ORDER BY ps.total_wins DESC LIMIT 5";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(TopWinEntry.newBuilder()
                        .setUsername(rs.getString("username"))
                        .setWinCount(rs.getInt("total_wins"))
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Get Top Winners error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return list;
    }

    public List<TopWordEntry> getTopWords() {
        List<TopWordEntry> list = new ArrayList<>();
        String sql = "SELECT lw.word, lw.word_length, u.username " +
                "FROM longest_words_record lw JOIN users u ON lw.submitted_by = u.user_id " +
                "ORDER BY lw.word_length DESC, lw.rec_id ASC LIMIT 5";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(TopWordEntry.newBuilder()
                        .setWord(rs.getString("word"))
                        .setWordLength(rs.getInt("word_length"))
                        .setSubmittedBy(rs.getString("username"))
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Get Top Words error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
        }

        return list;
    }
}