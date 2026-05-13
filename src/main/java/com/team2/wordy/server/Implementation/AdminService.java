package com.team2.wordy.server.Implementation;

import com.team2.wordy.proto.*;
import com.team2.wordy.proto.AdminServiceGrpc;
import com.team2.wordy.server.DAO.AdminDao;
import com.team2.wordy.server.TimeConfigManager;
import com.team2.wordy.server.TokenManager;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class AdminService extends AdminServiceGrpc.AdminServiceImplBase {

    private final AdminDao adminDao;
    private final TimeConfigManager timeConfigManager;

    public AdminService() {
        this.adminDao = new AdminDao();
        this.timeConfigManager = TimeConfigManager.getInstance();
    }

    // Create Player
    @Override
    public void createPlayer(CreatePlayerRequest request, StreamObserver<CreatePlayerResponse> responseObserver) {
        int adminId = adminDao.validateAdminToken(request.getAdminToken());
        if (adminId == -1) {
            responseObserver.onNext(CreatePlayerResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Unauthorized. Token is invalid or not an admin.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        PlayerRecord newPlayer = adminDao.createPlayer(request.getUsername(), request.getPassword(), request.getRole());
        if (newPlayer == null) {
            responseObserver.onNext(CreatePlayerResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Username already exists or invalid data.")
                    .build());
        } else {
            responseObserver.onNext(CreatePlayerResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Player created.")
                    .setPlayer(newPlayer)
                    .build());
        }
        responseObserver.onCompleted();
    }

    // Get All Players
    @Override
    public void getAllPlayers(GetAllPlayersRequest request, StreamObserver<GetAllPlayersResponse> responseObserver) {
        int adminId = adminDao.validateAdminToken(request.getAdminToken());
        if (adminId == -1) {
            responseObserver.onNext(GetAllPlayersResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Unauthorized. Token is invalid or not an admin.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        boolean includeArchived = request.getIncludeArchived();
        List<PlayerRecord> players = adminDao.getAllPlayers(includeArchived);
        responseObserver.onNext(GetAllPlayersResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Found " + players.size() + " player(s).")
                .addAllPlayers(players)
                .build());
        responseObserver.onCompleted();
    }

    // Update Player
    @Override
    public void updatePlayer(UpdatePlayerRequest request, StreamObserver<UpdatePlayerResponse> responseObserver) {
        int adminId = adminDao.validateAdminToken(request.getAdminToken());
        if (adminId == -1) {
            responseObserver.onNext(UpdatePlayerResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Unauthorized. Token is invalid or not an admin.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        PlayerRecord updatedPlayer = adminDao.updatePlayer(
                request.getPlayerId(),
                request.getNewUsername(),
                request.getNewPassword(),
                request.getNewRole(),
                request.getActive() // Changed from getActivate() to getActive()
        );

        if (updatedPlayer == null) {
            responseObserver.onNext(UpdatePlayerResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Player not found or update failed.")
                    .build());
        } else {
            responseObserver.onNext(UpdatePlayerResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Player updated.")
                    .setPlayer(updatedPlayer)
                    .build());
        }
        responseObserver.onCompleted();
    }

    // Delete (Archive) Player
    @Override
    public void deletePlayer(DeletePlayerRequest request, StreamObserver<DeletePlayerResponse> responseObserver) {
        int adminId = adminDao.validateAdminToken(request.getAdminToken());
        if (adminId == -1) {
            responseObserver.onNext(DeletePlayerResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Unauthorized. Token is invalid or not an admin.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        // Archive player and get their username
        String archivedUsername = adminDao.archivePlayer(request.getPlayerId());

        if (archivedUsername != null) {
            // Revokes token in memory so the player is instantly kicked out
            TokenManager.getInstance().revokeByUsername(archivedUsername);

            responseObserver.onNext(DeletePlayerResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Player archived.")
                    .build());
        } else {
            responseObserver.onNext(DeletePlayerResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Player not found or already archived.")
                    .build());
        }
        responseObserver.onCompleted();
    }

    // Search Player
    @Override
    public void searchPlayer(SearchPlayerRequest request, StreamObserver<SearchPlayerResponse> responseObserver) {
        // Verify admin token
        int adminId = adminDao.validateAdminToken(request.getAdminToken());
        if (adminId == -1) {
            responseObserver.onNext(SearchPlayerResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Unauthorized. Token is invalid or not an admin.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        // Ask adminDao for matching players through query
        boolean includeArchived = request.getIncludeArchived();
        List<PlayerRecord> players = adminDao.searchPlayer(request.getQuery(), includeArchived);
        responseObserver.onNext(SearchPlayerResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Found " + players.size() + " result(s).")
                .addAllPlayers(players)
                .build());
        responseObserver.onCompleted();
    }

    // Get Time Config
    @Override
    public void getTimeConfig(GetTimeConfigRequest request, StreamObserver<GetTimeConfigResponse> responseObserver) {
        int adminId = adminDao.validateAdminToken(request.getAdminToken());
        if (adminId == -1) {
            responseObserver.onNext(GetTimeConfigResponse.newBuilder().setSuccess(false).build());
            responseObserver.onCompleted();
            return;
        }

        responseObserver.onNext(GetTimeConfigResponse.newBuilder()
                .setSuccess(true)
                .setJoinWaitSeconds(timeConfigManager.getJoinWaitSeconds())
                .setRoundDurationSeconds(timeConfigManager.getRoundDurationSeconds())
                .build());
        responseObserver.onCompleted();
    }

    // Set Time Config
    @Override
    public void setTimeConfig(SetTimeConfigRequest request, StreamObserver<SetTimeConfigResponse> responseObserver) {
        int adminId = adminDao.validateAdminToken(request.getAdminToken());
        if (adminId == -1) {
            responseObserver.onNext(SetTimeConfigResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Unauthorized Token is invalid or not an admin.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        boolean updated = timeConfigManager.update(request.getJoinWaitSeconds(), request.getRoundDurationSeconds());
        if (updated) {
            responseObserver.onNext(SetTimeConfigResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Configuration updated")
                    .setJoinWaitSeconds(timeConfigManager.getJoinWaitSeconds())
                    .setRoundDurationSeconds(timeConfigManager.getRoundDurationSeconds())
                    .build());
        } else {
            responseObserver.onNext(SetTimeConfigResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to update database configuration.")
                    .build());
        }
        responseObserver.onCompleted();
    }
}