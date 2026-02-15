package Server;

import Server.util.ServerConsole;
import Server.util.ServerLogger;
import Server.util.XMLWriter;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

/**
 * Server — main entry point for the server application.
 *
 * - Starts a thread pool of up to MAX_CLIENTS threads.
 * - Listens on PORT for incoming client connections.
 * - Can be stopped gracefully via the ServerConsole (type "stop").
 * - Initializes all XML data files on startup.
 *
 * SDG 12: Responsible Consumption and Production
 * (Food-Waste Reducer Marketplace)
 */
public class Server {

    private static final int PORT        = 5000;
    private static final int MAX_CLIENTS = 50;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running          = true;
    private static volatile boolean shutdownRequested = false;

    public Server() {
        this.threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
    }

    public void start() {
        // Initialize all data/ XML files before accepting clients
        XMLWriter.initializeDataFiles();

        try {
            serverSocket = new ServerSocket(PORT);
            ServerLogger.logServerStart(PORT);

            // Start console thread so admin can type "stop" at any time
            new Thread(new ServerConsole()).start();

            System.out.println("===========================================");
            System.out.println("  Food-Waste Reducer System - Server");
            System.out.println("  SDG 12: Responsible Consumption");
            System.out.println("===========================================");
            System.out.println("Server running on port " + PORT);
            System.out.println("Type  'stop'  to shut down gracefully.\n");

            while (running && !shutdownRequested) {
                try {
                    // 1-second timeout so we can check shutdownRequested flag
                    serverSocket.setSoTimeout(1000);
                    Socket clientSocket = serverSocket.accept();
                    threadPool.execute(new ClientHandler(clientSocket));

                } catch (SocketTimeoutException e) {
                    // Normal — just re-check the flag
                    if (shutdownRequested) break;

                } catch (IOException e) {
                    if (running) {
                        ServerLogger.logError("Accept error: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            ServerLogger.logError("Failed to start server: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        running = false;
        System.out.println("\nShutting down server...");

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            ServerLogger.logError("Error closing server socket: " + e.getMessage());
        }

        ServerLogger.logServerShutdown();
        System.out.println("Server stopped.");
    }

    public static void requestShutdown() {
        shutdownRequested = true;
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}