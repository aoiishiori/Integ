package com.team2.wordy.client.player.model;

import com.team2.wordy.proto.AuthServiceGrpc;
import com.team2.wordy.proto.GameServiceGrpc;
import io.grpc.ManagedChannel;

/**
 * PlayerModel — holds the gRPC stubs and session state for the player client.
 * No UI logic here. Controllers call methods on this model only.
 */
public class PlayerModel {

    // ── gRPC stubs (set once at startup by PlayerMain) ──────────────
    private AuthServiceGrpc.AuthServiceBlockingStub  authStub;
    private GameServiceGrpc.GameServiceBlockingStub  gameStub;
    private ManagedChannel channel;

    // ── Session state (set after successful login) ───────────────────
    private String token;       // session token returned by server
    private String username;    // logged-in player's username
    private String role;        // "PLAYER"

    // ── Constructor ─────────────────────────────────────────────────
    public PlayerModel(ManagedChannel channel) {
        this.channel  = channel;
        this.authStub = AuthServiceGrpc.newBlockingStub(channel);
        this.gameStub = GameServiceGrpc.newBlockingStub(channel);
    }

    // ── Stub getters (used by controllers) ──────────────────────────
    public AuthServiceGrpc.AuthServiceBlockingStub getAuthStub() { return authStub; }
    public GameServiceGrpc.GameServiceBlockingStub getGameStub() { return gameStub; }
    public ManagedChannel getChannel() { return channel; }

    // ── Session state getters / setters ─────────────────────────────
    public String getToken()    { return token; }
    public String getUsername() { return username; }
    public String getRole()     { return role; }

    public void setSession(String token, String username, String role) {
        this.token    = token;
        this.username = username;
        this.role     = role;
    }

    public void clearSession() {
        this.token    = null;
        this.username = null;
        this.role     = null;
    }

    public boolean isLoggedIn() { return token != null && !token.isBlank(); }

    // ── Shutdown ─────────────────────────────────────────────────────
    public void shutdown() throws InterruptedException {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}