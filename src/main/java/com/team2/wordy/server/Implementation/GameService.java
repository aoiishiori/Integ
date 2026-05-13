package com.team2.wordy.server.Implementation;

import com.team2.wordy.proto.*;
import com.team2.wordy.proto.GameServiceGrpc;
import com.team2.wordy.server.DAO.GameDao;
import com.team2.wordy.server.DAO.StatsDao;
import com.team2.wordy.server.TimeConfigManager;
import com.team2.wordy.server.TokenManager;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameService extends GameServiceGrpc.GameServiceImplBase {

    private final GameDao gameDao;
    private final StatsDao statsDao;
    private final TimeConfigManager timeConfig;
    private final TokenManager tokenManager;
    private final Set<String> validWords; // Loaded from words.txt by WordyServer

    // sessionId → GameSession (all active games)
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();

    // token → sessionId (quick lookup to prevent double-joining)
    private final Map<String, String> tokenToSessionId = new ConcurrentHashMap<>();

    public GameService(Set<String> validWords) {
        this.gameDao      = new GameDao();
        this.statsDao     = new StatsDao();
        this.timeConfig   = TimeConfigManager.getInstance();
        this.tokenManager = TokenManager.getInstance();
        this.validWords   = validWords;
    }

    // ─────────────────────────────────────────────────────────────
    //  GameSession — in-memory state of one active game
    // ─────────────────────────────────────────────────────────────
    private static class GameSession {
        int    gameId;
        String sessionId;
        int    currentRound  = 0;
        int    currentRoundId = -1;
        long   roundStartTime = 0;
        long   lobbyStartTime = 0;

        List<String>             letters          = new ArrayList<>();
        Map<Integer, Integer>    playerRoundWins  = new ConcurrentHashMap<>(); // playerId → round wins
        Map<Integer, String>     roundSubmissions = new ConcurrentHashMap<>(); // playerId → best word this round

        // ── Last round result (read by getRoundResult) ─────────────
        String  lastRoundWinner      = "";    // username; empty = no winner (tie)
        String  lastRoundWinningWord = "";    // the winning word
        boolean lastGameOver         = false; // true = overall winner decided
        String  lastGameWinner       = "";    // username of overall winner

        boolean isLobbyReady() { return playerRoundWins.size() >= 2; }
    }

    // ─────────────────────────────────────────────────────────────
    //  startGame — Step 1
    //  Player clicks "Start Wordy". Joins a waiting game or creates one.
    //  When the second player joins, round 1 starts immediately.
    // ─────────────────────────────────────────────────────────────
    @Override
    public void startGame(StartGameRequest request,
                          StreamObserver<StartGameResponse> responseObserver) {

        TokenManager.SessionInfo session = tokenManager.validate(request.getToken());
        if (session == null || !session.isPlayer()) {
            respondError(responseObserver, StartGameResponse.newBuilder(),
                    "Unauthorized or not a player.");
            return;
        }

        int playerId = session.userId;

        // Prevent double-joining
        if (tokenToSessionId.containsKey(request.getToken())) {
            respondError(responseObserver, StartGameResponse.newBuilder(),
                    "Already in a game.");
            return;
        }

        // Find a waiting game or create a new one
        Integer waitingGameId = gameDao.getWaitingGameId();
        GameSession gs;

        if (waitingGameId != null) {
            // ── Join existing waiting game ──────────────────────────
            gs = activeSessions.values().stream()
                    .filter(s -> s.gameId == waitingGameId)
                    .findFirst()
                    .orElse(null);

            if (gs == null) {
                // Session map out of sync — rebuild
                gs            = new GameSession();
                gs.gameId     = waitingGameId;
                gs.sessionId  = UUID.randomUUID().toString();
                gs.lobbyStartTime = System.currentTimeMillis();
                activeSessions.put(gs.sessionId, gs);
            }

            gameDao.addParticipant(gs.gameId, playerId);
            gs.playerRoundWins.put(playerId, 0);
            tokenToSessionId.put(request.getToken(), gs.sessionId);

            // ── Second player joined → start round 1 immediately ───
            if (gs.isLobbyReady()) {
                gameDao.updateGameStatus(gs.gameId, "IN_PROGRESS", null);
                gs.currentRound   = 1;
                gs.letters        = generateLetters();
                gs.roundStartTime = System.currentTimeMillis();
                gs.roundSubmissions = new ConcurrentHashMap<>();
                gs.currentRoundId = gameDao.createRound(
                        gs.gameId, 1, String.join("", gs.letters));

                System.out.println("[GameService] Game " + gs.gameId
                        + " started — Round 1 letters: " + gs.letters);
            }

        } else {
            // ── Create new game, wait for second player ─────────────
            String newSessionId = UUID.randomUUID().toString();
            int newGameId = gameDao.createGame(newSessionId);
            if (newGameId == -1) {
                respondError(responseObserver, StartGameResponse.newBuilder(),
                        "Failed to create game. Check DB.");
                return;
            }

            gs               = new GameSession();
            gs.gameId        = newGameId;
            gs.sessionId     = newSessionId;
            gs.lobbyStartTime = System.currentTimeMillis();

            gameDao.addParticipant(newGameId, playerId);
            gs.playerRoundWins.put(playerId, 0);

            activeSessions.put(newSessionId, gs);
            tokenToSessionId.put(request.getToken(), newSessionId);

            System.out.println("[GameService] New game created — id=" + newGameId
                    + " session=" + newSessionId);
        }

        responseObserver.onNext(StartGameResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Joined game successfully.")
                .setSessionId(gs.sessionId)
                .build());
        responseObserver.onCompleted();
    }

    // ─────────────────────────────────────────────────────────────
    //  waitingTimer — Step 2
    //  Client polls this to check lobby status.
    //  Returns game_ready=true when round 1 has started.
    // ─────────────────────────────────────────────────────────────
    @Override
    public void waitingTimer(WaitingTimerRequest request,
                             StreamObserver<WaitingTimerResponse> responseObserver) {

        GameSession gs = activeSessions.get(request.getSessionId());
        if (gs == null) {
            respondError(responseObserver, WaitingTimerResponse.newBuilder(),
                    "Game session not found.");
            return;
        }

        long elapsed   = (System.currentTimeMillis() - gs.lobbyStartTime) / 1000;
        int  remaining = Math.max(0, timeConfig.getJoinWaitSeconds() - (int) elapsed);

        if (gs.isLobbyReady()) {
            // Second player joined — game is ready
            responseObserver.onNext(WaitingTimerResponse.newBuilder()
                    .setSecondsRemaining(0)
                    .setGameReady(true)
                    .setCancelled(false)
                    .build());

        } else if (remaining <= 0) {
            // Timer expired with no second player — cancel game
            activeSessions.remove(request.getSessionId());
            tokenToSessionId.remove(request.getToken());
            gameDao.updateGameStatus(gs.gameId, "COMPLETED", null);

            responseObserver.onNext(WaitingTimerResponse.newBuilder()
                    .setSecondsRemaining(0)
                    .setGameReady(false)
                    .setCancelled(true)
                    .build());

        } else {
            // Still waiting
            responseObserver.onNext(WaitingTimerResponse.newBuilder()
                    .setSecondsRemaining(remaining)
                    .setGameReady(false)
                    .setCancelled(false)
                    .build());
        }

        responseObserver.onCompleted();
    }

    // ─────────────────────────────────────────────────────────────
    //  playGame — Step 3
    //  Unary — client calls once per round to get the 20 letters.
    // ─────────────────────────────────────────────────────────────
    @Override
    public void playGame(PlayGameRequest request,
                         StreamObserver<PlayGameResponse> responseObserver) {

        TokenManager.SessionInfo session = tokenManager.validate(request.getToken());
        if (session == null) {
            respondError(responseObserver, PlayGameResponse.newBuilder(), "Unauthorized.");
            return;
        }

        GameSession gs = activeSessions.get(request.getSessionId());
        if (gs == null) {
            respondError(responseObserver, PlayGameResponse.newBuilder(), "Game not found.");
            return;
        }

        if (request.getRoundNumber() != gs.currentRound) {
            respondError(responseObserver, PlayGameResponse.newBuilder(),
                    "Round mismatch. Expected round " + gs.currentRound + ".");
            return;
        }

        // Build current scores
        List<PlayerScore> scores = buildScoresList(gs);

        responseObserver.onNext(PlayGameResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Round " + gs.currentRound + " — submit your words!")
                .setRoundNumber(gs.currentRound)
                .addAllLetters(gs.letters)
                .setDurationSeconds(timeConfig.getRoundDurationSeconds())
                .addAllScores(scores)
                .build());
        responseObserver.onCompleted();
    }

    // ─────────────────────────────────────────────────────────────
    //  submitWord — Step 4
    //  Player submits a word. Server validates immediately.
    //  Only the longest valid word per player is kept (best submission).
    // ─────────────────────────────────────────────────────────────
    @Override
    public void submitWord(SubmitWordRequest request,
                           StreamObserver<SubmitWordResponse> responseObserver) {

        TokenManager.SessionInfo session = tokenManager.validate(request.getToken());
        if (session == null) {
            respondError(responseObserver, SubmitWordResponse.newBuilder(), "Unauthorized.");
            return;
        }

        GameSession gs = activeSessions.get(request.getSessionId());
        if (gs == null || request.getRoundNumber() != gs.currentRound) {
            respondError(responseObserver, SubmitWordResponse.newBuilder(),
                    "Invalid game or round.");
            return;
        }

        String word     = request.getWord().trim().toUpperCase();
        int    playerId = session.userId;

        // ── Validation ─────────────────────────────────────────────

        if (word.length() < 5) {
            gameDao.addWordSubmission(gs.currentRoundId, playerId, word, false);
            responseObserver.onNext(SubmitWordResponse.newBuilder()
                    .setValid(false)
                    .setMessage("Invalid — word must be at least 5 letters.")
                    .setWord(word)
                    .build());
            responseObserver.onCompleted();
            return;
        }

        if (!canFormWord(word, gs.letters)) {
            gameDao.addWordSubmission(gs.currentRoundId, playerId, word, false);
            responseObserver.onNext(SubmitWordResponse.newBuilder()
                    .setValid(false)
                    .setMessage("Invalid — letters not available in this round.")
                    .setWord(word)
                    .build());
            responseObserver.onCompleted();
            return;
        }

        if (!validWords.contains(word.toLowerCase())) {
            gameDao.addWordSubmission(gs.currentRoundId, playerId, word, false);
            responseObserver.onNext(SubmitWordResponse.newBuilder()
                    .setValid(false)
                    .setMessage("Invalid word — not in dictionary.")
                    .setWord(word)
                    .build());
            responseObserver.onCompleted();
            return;
        }

        // ── Valid word — keep only the longest per player ───────────
        String currentBest = gs.roundSubmissions.get(playerId);
        if (currentBest == null || word.length() > currentBest.length()) {
            gs.roundSubmissions.put(playerId, word);
        }

        // Save to DB
        gameDao.addWordSubmission(gs.currentRoundId, playerId, word, true);

        responseObserver.onNext(SubmitWordResponse.newBuilder()
                .setValid(true)
                .setMessage("Valid word!")
                .setWord(word)
                .build());
        responseObserver.onCompleted();
    }

    // ─────────────────────────────────────────────────────────────
    //  roundTimer — Step 5
    //  Client polls this to get seconds remaining in the round.
    //  When round_active = false, client should call getRoundResult.
    // ─────────────────────────────────────────────────────────────
    @Override
    public void roundTimer(RoundTimerRequest request,
                           StreamObserver<RoundTimerResponse> responseObserver) {

        GameSession gs = activeSessions.get(request.getSessionId());
        if (gs == null) {
            respondError(responseObserver, RoundTimerResponse.newBuilder(),
                    "Game not found.");
            return;
        }

        long elapsed   = (System.currentTimeMillis() - gs.roundStartTime) / 1000;
        int  remaining = Math.max(0, timeConfig.getRoundDurationSeconds() - (int) elapsed);
        boolean active = remaining > 0;

        // When timer hits 0 and submissions haven't been processed yet, process now
        if (!active && gs.roundSubmissions != null) {
            processRoundEnd(gs);
        }

        responseObserver.onNext(RoundTimerResponse.newBuilder()
                .setSecondsRemaining(remaining)
                .setRoundActive(active)
                .build());
        responseObserver.onCompleted();
    }

    // ─────────────────────────────────────────────────────────────
    //  getRoundResult — Step 6
    //  Client calls this after roundTimer returns round_active = false.
    //  Returns who won the round, the winning word, updated scores,
    //  and whether the overall game is over.
    // ─────────────────────────────────────────────────────────────
    @Override
    public void getRoundResult(GetRoundResultRequest request,
                               StreamObserver<GetRoundResultResponse> responseObserver) {

        TokenManager.SessionInfo session = tokenManager.validate(request.getToken());
        if (session == null) {
            respondError(responseObserver, GetRoundResultResponse.newBuilder(),
                    "Unauthorized.");
            return;
        }

        GameSession gs = activeSessions.get(request.getSessionId());
        if (gs == null) {
            // Game may have been removed after game over — still a valid state
            respondError(responseObserver, GetRoundResultResponse.newBuilder(),
                    "Game session not found. The game may have ended.");
            return;
        }

        // Build current scores from in-memory playerRoundWins
        List<PlayerScore> scores = buildScoresList(gs);

        responseObserver.onNext(GetRoundResultResponse.newBuilder()
                .setSuccess(true)
                .setRoundNumber(request.getRoundNumber())
                .setWinner(gs.lastRoundWinner)           // empty string = no winner (tie)
                .setWinningWord(gs.lastRoundWinningWord)
                .addAllScores(scores)
                .setGameOver(gs.lastGameOver)
                .setGameWinner(gs.lastGameWinner)        // empty string if game not over
                .build());
        responseObserver.onCompleted();
    }

    // ─────────────────────────────────────────────────────────────
    //  getTopPlayers — Leaderboard
    //  Any authenticated user can call this from the home screen.
    // ─────────────────────────────────────────────────────────────
    @Override
    public void getTopPlayers(GetTopPlayersRequest request,
                              StreamObserver<GetTopPlayersResponse> responseObserver) {

        TokenManager.SessionInfo session = tokenManager.validate(request.getToken());
        if (session == null) {
            respondError(responseObserver, GetTopPlayersResponse.newBuilder(),
                    "Unauthorized.");
            return;
        }

        responseObserver.onNext(GetTopPlayersResponse.newBuilder()
                .addAllTopWinners(statsDao.getTopWinners())
                .addAllTopWords(statsDao.getTopWords())
                .build());
        responseObserver.onCompleted();
    }

    // ─────────────────────────────────────────────────────────────
    //  processRoundEnd — called internally when round timer hits 0
    //
    //  Determines the round winner (longest valid word, no tie),
    //  updates DB and in-memory state, stores result fields for
    //  getRoundResult(), and either starts the next round or
    //  ends the game if a player reached 3 wins.
    // ─────────────────────────────────────────────────────────────
    private void processRoundEnd(GameSession gs) {
        // Guard against double-processing
        if (gs.roundSubmissions == null) return;

        Map<Integer, String> submissions = gs.roundSubmissions;
        gs.roundSubmissions = null; // Mark as processed immediately

        // ── Find longest valid word ─────────────────────────────────
        Map.Entry<Integer, String> winnerEntry = null;
        for (Map.Entry<Integer, String> entry : submissions.entrySet()) {
            if (winnerEntry == null
                    || entry.getValue().length() > winnerEntry.getValue().length()) {
                winnerEntry = entry;
            }
        }

        // ── Check for ties ─────────────────────────────────────────
        boolean isTie = false;
        if (winnerEntry != null) {
            int maxLen = winnerEntry.getValue().length();
            for (Map.Entry<Integer, String> entry : submissions.entrySet()) {
                if (!entry.getKey().equals(winnerEntry.getKey())
                        && entry.getValue().length() == maxLen) {
                    isTie = true;
                    break;
                }
            }
        }

        Integer winnerId    = (!isTie && winnerEntry != null) ? winnerEntry.getKey() : null;
        String winningWord  = (winnerEntry != null) ? winnerEntry.getValue() : null;

        // ── Persist round result ────────────────────────────────────
        gameDao.updateRoundWinner(gs.currentRoundId, winnerId, winningWord);

        if (winnerId != null) {
            gameDao.incrementParticipantWins(gs.gameId, winnerId);
            gs.playerRoundWins.put(winnerId,
                    gs.playerRoundWins.getOrDefault(winnerId, 0) + 1);
            statsDao.incrementPlayerWins(winnerId);
            if (winningWord != null) {
                statsDao.updateLongestWord(winnerId, winningWord);
                statsDao.addLongestWordRecord(winningWord, winnerId);
            }
        }

        // ── Resolve winner username (no DB query needed) ────────────
        String winnerUsername = (winnerId != null)
                ? tokenManager.getUsernameByUserId(winnerId) : "";

        // ── Check game over (first to 3 round wins) ─────────────────
        boolean gameOver = gs.playerRoundWins.values().stream()
                .anyMatch(w -> w >= 3);

        String overallWinnerUsername = (gameOver && winnerId != null)
                ? winnerUsername : "";

        // ── Store result for getRoundResult() ───────────────────────
        gs.lastRoundWinner      = winnerUsername;
        gs.lastRoundWinningWord = (winningWord != null) ? winningWord : "";
        gs.lastGameOver         = gameOver;
        gs.lastGameWinner       = overallWinnerUsername;

        System.out.println("[GameService] Round " + gs.currentRound
                + " ended — winner: " + (winnerUsername.isEmpty() ? "none (tie)" : winnerUsername)
                + (gameOver ? " | GAME OVER — " + overallWinnerUsername + " wins!" : ""));

        // ── End game or start next round ────────────────────────────
        if (gameOver) {
            Integer finalWinnerId = winnerId;
            gameDao.updateGameStatus(gs.gameId, "COMPLETED", finalWinnerId);
            activeSessions.remove(gs.sessionId);
            // Note: tokenToSessionId entries will be cleaned up on next client poll
        } else {
            // Start next round
            gs.currentRound++;
            gs.letters          = generateLetters();
            gs.roundStartTime   = System.currentTimeMillis();
            gs.roundSubmissions = new ConcurrentHashMap<>();
            gs.currentRoundId   = gameDao.createRound(
                    gs.gameId, gs.currentRound, String.join("", gs.letters));

            System.out.println("[GameService] Round " + gs.currentRound
                    + " started — letters: " + gs.letters);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  generateLetters — 20 random letters with 5–7 vowels
    // ─────────────────────────────────────────────────────────────
    private List<String> generateLetters() {
        String vowels     = "AEIOU";
        String consonants = "BCDFGHJKLMNPQRSTVWXYZ";
        Random rand       = new Random();
        List<String> letters = new ArrayList<>();

        int vowelCount = 5 + rand.nextInt(3); // 5, 6, or 7 vowels
        for (int i = 0; i < vowelCount; i++) {
            letters.add(String.valueOf(vowels.charAt(rand.nextInt(vowels.length()))));
        }
        for (int i = 0; i < 20 - vowelCount; i++) {
            letters.add(String.valueOf(consonants.charAt(rand.nextInt(consonants.length()))));
        }

        Collections.shuffle(letters);
        return letters;
    }

    // ─────────────────────────────────────────────────────────────
    //  canFormWord — checks if word can be spelled from available letters
    // ─────────────────────────────────────────────────────────────
    private boolean canFormWord(String word, List<String> availableLetters) {
        List<String> copy = new ArrayList<>(availableLetters);
        for (char c : word.toCharArray()) {
            if (!copy.remove(String.valueOf(c))) return false;
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────
    //  buildScoresList — builds PlayerScore list from in-memory wins
    // ─────────────────────────────────────────────────────────────
    private List<PlayerScore> buildScoresList(GameSession gs) {
        List<PlayerScore> scores = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : gs.playerRoundWins.entrySet()) {
            String username = tokenManager.getUsernameByUserId(entry.getKey());
            scores.add(PlayerScore.newBuilder()
                    .setUsername(username)
                    .setRoundWins(entry.getValue())
                    .build());
        }
        return scores;
    }

    // ─────────────────────────────────────────────────────────────
    //  respondError — generic error helper
    //  Sets success=false and message on any response builder.
    // ─────────────────────────────────────────────────────────────
    private <T extends com.google.protobuf.GeneratedMessageV3> void respondError(
            StreamObserver<T> responseObserver,
            com.google.protobuf.GeneratedMessageV3.Builder<?> builder,
            String message) {
        try {
            builder.setField(
                    builder.getDescriptorForType().findFieldByName("success"), false);
            builder.setField(
                    builder.getDescriptorForType().findFieldByName("message"), message);
            @SuppressWarnings("unchecked")
            T response = (T) builder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}