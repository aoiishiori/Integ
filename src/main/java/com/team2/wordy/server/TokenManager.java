package com.team2.wordy.server;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TokenManager — session token generation and validation.
 *   - Generate a unique session token when a player logs in
 *   - Store the mapping: token → username and role
 *   - Validate tokens for every incoming RPC
 *   - Invalidate tokens on logout
 *
 * Thread-safe: uses ConcurrentHashMap — safe for concurrent gRPC calls.
 */
public class TokenManager {

    // ── Singleton ──────────────────────────────────────────────────
    private static final TokenManager INSTANCE = new TokenManager();
    public static TokenManager getInstance() { return INSTANCE; }
    private TokenManager() {}

    // ── Token store ────────────────────────────────────────────────
    // token → SessionInfo (username + role)
    private final Map<String, SessionInfo> tokenStore = new ConcurrentHashMap<>();

    // ── SessionInfo — what we know about a logged-in user ──────────
    public static class SessionInfo {
        public final int    userId;
        public final String username;
        public final String role;       // "PLAYER" | "ADMIN"
        public final long   createdAt;  // System.currentTimeMillis()

        public SessionInfo(int userId, String username, String role) {
            this.userId    = userId;
            this.username  = username;
            this.role      = role;
            this.createdAt = System.currentTimeMillis();
        }

        public boolean isAdmin()  { return "ADMIN".equalsIgnoreCase(role); }
        public boolean isPlayer() { return "PLAYER".equalsIgnoreCase(role); }
    }

    // ── generateToken() ────────────────────────────────────────────
    /**
     * Creates a new cryptographically random token and stores it.
     * Called by AuthService after successful login.
     * Also enforces single-session by revoking any existing token
     * for the same username before issuing a new one.
     */
    public String generateToken(int userId, String username, String role) {
        // Single-session enforcement — kick existing session if any
        revokeByUsername(username);

        // Generate 32 random bytes → Base64 URL-safe string (~43 chars)
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokenStore.put(token, new SessionInfo(userId, username, role));
        return token;
    }

    // ── validate() ─────────────────────────────────────────────────
    /**
     * Validates a token and returns its SessionInfo.
     * Returns null if the token is invalid or not found.
     *
     * Usage in any ServiceImpl:
     *   SessionInfo session = TokenManager.getInstance().validate(token);
     *   if (session == null) { reject RPC }
     */
    public SessionInfo validate(String token) {
        if (token == null || token.isBlank()) return null;
        return tokenStore.get(token);
    }

    // ── validateAdmin() ────────────────────────────────────────────
    /**
     * Validates a token AND checks it belongs to an ADMIN.
     * Convenience method for AdminService.
     */
    public SessionInfo validateAdmin(String token) {
        SessionInfo session = validate(token);
        if (session == null || !session.isAdmin()) return null;
        return session;
    }

    // ── validatePlayer() ───────────────────────────────────────────
    /**
     * Validates a token AND checks it belongs to a PLAYER.
     * Convenience method for GameService.
     */
    public SessionInfo validatePlayer(String token) {
        SessionInfo session = validate(token);
        if (session == null || !session.isPlayer()) return null;
        return session;
    }

    // ── revoke() ───────────────────────────────────────────────────
    /**
     * Removes a token from the store (logout).
     * Called by AuthService on logout.
     */
    public void revoke(String token) {
        if (token != null) tokenStore.remove(token);
    }

    // ── revokeByUsername() ─────────────────────────────────────────
    /**
     * Removes ALL tokens belonging to a username.
     * Used for single-session enforcement — when a user logs in again,
     * their old session is kicked instantly.
     * Also used by AdminService when a player is archived.
     */
    public void revokeByUsername(String username) {
        tokenStore.entrySet()
                .removeIf(e -> e.getValue().username.equalsIgnoreCase(username));
    }

    // ── getUsernameByToken() ────────────────────────────────────────
    /**
     * Quick lookup — get just the username from a token.
     * Returns null if token is invalid.
     */
    public String getUsernameByToken(String token) {
        SessionInfo session = validate(token);
        return session != null ? session.username : null;
    }

    // ── getUsernameByUserId() ───────────────────────────────────────
    /**
     * Looks up a username by userId from the in-memory token store.
     * Used by GameService.processRoundEnd() to resolve winner usernames
     * without querying the database.
     *
     * Returns empty string if userId is not currently logged in.
     */
    public String getUsernameByUserId(int userId) {
        return tokenStore.values().stream()
                .filter(s -> s.userId == userId)
                .map(s -> s.username)
                .findFirst()
                .orElse("");
    }

    // ── isLoggedIn() ───────────────────────────────────────────────
    /**
     * Check if a specific username currently has an active session.
     * Used by AuthService to detect already-logged-in accounts.
     */
    public boolean isLoggedIn(String username) {
        return tokenStore.values().stream()
                .anyMatch(s -> s.username.equalsIgnoreCase(username));
    }

    // ── activeSessionCount() ───────────────────────────────────────
    /**
     * Returns the number of currently active sessions.
     * Useful for logging/debugging.
     */
    public int activeSessionCount() {
        return tokenStore.size();
    }
}