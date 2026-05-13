package com.team2.wordy.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * TimeConfigManager — live timer settings in memory.
 *   - Load join_wait_seconds and round_duration_seconds from DB at startup
 *   - Provide fast in-memory reads for Member 6's timer logic
 *   - Update both memory AND database when admin changes settings
 *
 * Singleton — one shared instance across the whole server.
 */
public class TimeConfigManager {

    // ── Singleton ──────────────────────────────────────────────────
    private static final TimeConfigManager INSTANCE = new TimeConfigManager();
    public static TimeConfigManager getInstance() { return INSTANCE; }

    // ── Defaults (used if DB is unavailable) ───────────────────────
    private static final int DEFAULT_JOIN_WAIT    = 10;
    private static final int DEFAULT_ROUND_DURATION = 30;

    // ── Live values (volatile for thread visibility) ────────────────
    private volatile int joinWaitSeconds      = DEFAULT_JOIN_WAIT;
    private volatile int roundDurationSeconds = DEFAULT_ROUND_DURATION;

    // ── Constructor — loads from DB immediately ─────────────────────
    private TimeConfigManager() {
        loadFromDatabase();
    }

    // ── loadFromDatabase() ─────────────────────────────────────────
    /**
     * Reads game_config row (con_id = 1) from the database.
     * Called once at server startup.
     * Falls back to defaults if DB is unavailable.
     */
    private void loadFromDatabase() {
        String sql = "SELECT join_wait_seconds, round_duration_seconds "
                + "FROM game_config WHERE con_id = 1";

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                joinWaitSeconds      = rs.getInt("join_wait_seconds");
                roundDurationSeconds = rs.getInt("round_duration_seconds");
                System.out.println("[TimeConfigManager] Loaded from DB — "
                        + "join_wait=" + joinWaitSeconds + "s, "
                        + "round_duration=" + roundDurationSeconds + "s");
            } else {
                System.out.println("[TimeConfigManager] No config row found — using defaults.");
            }

        } catch (SQLException e) {
            System.err.println("[TimeConfigManager] DB unavailable — using defaults. " + e.getMessage());
        }
    }

    // ── Getters — used by Member 6 in every game timer ─────────────

    /**
     * How many seconds to wait in the lobby for a second player.
     * Default: 10. Admin can change via SetTimeConfig.
     */
    public int getJoinWaitSeconds() {
        return joinWaitSeconds;
    }

    /**
     * How many seconds each round lasts.
     * Default: 30. Admin can change via SetTimeConfig.
     */
    public int getRoundDurationSeconds() {
        return roundDurationSeconds;
    }

    // ── update() — called by AdminServiceImpl on SetTimeConfig ──────
    /**
     * Updates both in-memory values AND the database.
     * Pass 0 for either parameter to keep its current value.
     */
    public synchronized boolean update(int newJoinWait, int newRoundDuration) {
        // Apply only non-zero values
        int updatedJoin  = (newJoinWait      > 0) ? newJoinWait      : joinWaitSeconds;
        int updatedRound = (newRoundDuration > 0) ? newRoundDuration : roundDurationSeconds;

        String sql = "UPDATE game_config "
                + "SET join_wait_seconds = ?, round_duration_seconds = ? "
                + "WHERE con_id = 1";

        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, updatedJoin);
            pstmt.setInt(2, updatedRound);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                // Update in-memory only after DB confirms success
                joinWaitSeconds      = updatedJoin;
                roundDurationSeconds = updatedRound;
                System.out.println("[TimeConfigManager] Updated — "
                        + "join_wait=" + joinWaitSeconds + "s, "
                        + "round_duration=" + roundDurationSeconds + "s");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[TimeConfigManager] Update failed: " + e.getMessage());
        }

        return false;
    }

    // ── reload() — force reload from DB ───────────────────────────
    /**
     * Re-reads from the database.
     * Useful if the DB was updated externally (e.g. during testing).
     */
    public void reload() {
        loadFromDatabase();
    }

    // ── Summary — for logging ──────────────────────────────────────
    @Override
    public String toString() {
        return "TimeConfigManager{joinWait=" + joinWaitSeconds
                + "s, roundDuration=" + roundDurationSeconds + "s}";
    }
}