package com.team2.wordy.client.admin.model;

import com.team2.wordy.proto.AdminServiceGrpc;
import com.team2.wordy.proto.AuthServiceGrpc;
import io.grpc.ManagedChannel;

/**
 * AdminModel — holds the gRPC stubs and session state for the admin client.
 * No UI logic here. Controllers call methods on this model only.
 */
public class AdminModel {

    // ── gRPC stubs (set once at startup by AdminMain) ────────────────
    private AuthServiceGrpc.AuthServiceBlockingStub  authStub;
    private AdminServiceGrpc.AdminServiceBlockingStub adminStub;
    private ManagedChannel channel;

    // ── Session state (set after successful login) ───────────────────
    private String token;       // session token returned by server
    private String username;    // logged-in admin's username
    private String role;        // "ADMIN"

    // ── Constructor ─────────────────────────────────────────────────
    public AdminModel(ManagedChannel channel) {
        this.channel    = channel;
        this.authStub   = AuthServiceGrpc.newBlockingStub(channel);
        this.adminStub  = AdminServiceGrpc.newBlockingStub(channel);
    }

    // ── Stub getters (used by controllers) ──────────────────────────
    public AuthServiceGrpc.AuthServiceBlockingStub  getAuthStub()  { return authStub; }
    public AdminServiceGrpc.AdminServiceBlockingStub getAdminStub() { return adminStub; }
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