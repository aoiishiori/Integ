package com.team2.wordy.server.Implementation;

import com.team2.wordy.proto.*;
import com.team2.wordy.server.DAO.AuthDao;
import com.team2.wordy.server.ServerDatabase;
import com.team2.wordy.server.TokenManager;
import io.grpc.stub.StreamObserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuthService extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthDao authDao;
    private final TokenManager tokenManager;

    public AuthService() {
        this.authDao = new AuthDao();
        this.tokenManager = TokenManager.getInstance();
    }

    // Verify credentials against DB
    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        AuthDao.UserInfo userInfo = authDao.verifyCredentials(request.getUsername(), request.getPassword());

        if (userInfo == null) {
            responseObserver.onNext(LoginResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid credentials.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        if (userInfo.isArchived) {
            responseObserver.onNext(LoginResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Account is archived. Contact admin.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        // Generate new token
        String token = tokenManager.generateToken(userInfo.userId, userInfo.username, userInfo.role);

        // Sync token to database. Also overwrites old DB token
        boolean dbUpdated = authDao.updateSessionToken(userInfo.userId, token);

        if (!dbUpdated) {
            // Rollback memory token if DB fails
            tokenManager.revoke(token);
            responseObserver.onNext(LoginResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Server error during login.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        responseObserver.onNext(LoginResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Login successful.")
                .setRole(userInfo.role)
                .setToken(token)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        String token = request.getToken();

        // Validate token in memory to find user
        TokenManager.SessionInfo session = tokenManager.validate(token);

        if (session == null) {
            responseObserver.onNext(LogoutResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid or expired token.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        // Remove token from memory
        tokenManager.revoke(token);

        // Sync to DB to clear session_token
        boolean dbCleared = clearTokenFromDb(token);

        if (!dbCleared) {
            System.err.println("Warning: Failed to clear token from DB for user: " + session.username);
        }

        responseObserver.onNext(LogoutResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Logged out successfully.")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        String username = request.getUsername().trim();
        String password = request.getPassword();

        // Basic validation
        if (username.isEmpty() || password.isEmpty()) {
            responseObserver.onNext(RegisterResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Username and password cannot be empty.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        // Delegate to AuthDao (which strictly enforces role = "PLAYER")
        AuthDao.UserInfo newUser = authDao.registerPlayer(username, password);

        if (newUser == null) {
            responseObserver.onNext(RegisterResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Username already exists or registration failed.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        // Login upon successful registration
        String token = tokenManager.generateToken(newUser.userId, newUser.username, newUser.role);
        boolean dbUpdated = authDao.updateSessionToken(newUser.userId, token);

        if (!dbUpdated) {
            tokenManager.revoke(token);
            responseObserver.onNext(RegisterResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Account created. Please log in to your newly created account.")
                    .build());
        } else {
            responseObserver.onNext(RegisterResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Account created.")
                    .setRole(newUser.role)
                    .setToken(token)
                    .build());
        }

        responseObserver.onCompleted();
    }

    // Helper to clear token from DB without needing userId
    private boolean clearTokenFromDb(String token) {
        String sql = "UPDATE users SET session_token = NULL WHERE session_token = ?";
        try (Connection conn = ServerDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Clear Token From DB error: " + e.getMessage());
            System.err.println("Check the connection of the database.");
            return false;
        }
    }
}