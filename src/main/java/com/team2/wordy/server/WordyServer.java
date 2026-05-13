package com.team2.wordy.server;

import com.team2.wordy.server.Implementation.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * WordyServer — the gRPC server entry point.
 */
public class WordyServer {

    private static final Logger logger = Logger.getLogger(WordyServer.class.getName());
    private static final int PORT = 9090;
    private Server server;

    // ─────────────────────────────────────────────────────────────
    //  start() — builds and starts the gRPC server
    // ─────────────────────────────────────────────────────────────
    public void start() throws IOException {

        Set<String> dictionary = loadWords();
        System.out.println("Loaded " + dictionary.size() + " words from words.txt");

        server = ServerBuilder
                .forPort(PORT)
                .addService(new AuthService())
                .addService(new AdminService())
                .addService(new GameService(dictionary))
                .build()
                .start();

        logger.info("");
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Wordy gRPC Server started          ║");
        System.out.println("║   Listening on port: " + PORT + "            ║");
        System.out.println("╚══════════════════════════════════════╝");

        // Register a shutdown hook so the server stops cleanly
        // when someone presses Ctrl+C or the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("JVM shutting down — stopping gRPC server...");
            try {
                WordyServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            logger.info("Server stopped.");
        }));
    }

    private Set<String> loadWords() throws IOException {
        Set<String> words = new HashSet<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("words.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    words.add(line.trim().toLowerCase());
                }
            }
        }
        return words;
    }

    // ─────────────────────────────────────────────────────────────
    //  stop() — graceful shutdown
    //  Waits up to 30 seconds for in-flight RPCs to finish
    //  before forcing shutdown.
    // ─────────────────────────────────────────────────────────────
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  blockUntilShutdown() — keeps main thread alive
    //  gRPC runs on daemon threads, so without this the
    //  main thread exits immediately and the server dies.
    // ─────────────────────────────────────────────────────────────
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  main() — entry point
    // ─────────────────────────────────────────────────────────────
    public static void main(String[] args) throws IOException, InterruptedException {
        WordyServer wordyServer = new WordyServer();
        wordyServer.start();
        wordyServer.blockUntilShutdown();
    }
}