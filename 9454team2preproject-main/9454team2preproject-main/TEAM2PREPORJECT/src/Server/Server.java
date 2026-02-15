package Server;

import Server.util.*;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

public class Server {

    private static final int PORT = 5000;
    private static final int MAX_CLIENTS = 50;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = true;
    private static volatile boolean shutdownRequested = false;

    public Server() {
        this.threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
    }

    public void start() {
        try {

            serverSocket = new ServerSocket(PORT);
            ServerLogger.logServerStart(PORT);
            new Thread(new ServerConsole()).start();

            System.out.println("===========================================");
            System.out.println("  Food-Waste Reducer System - Server");
            System.out.println("  SDG 12: Responsible Consumption");
            System.out.println("===========================================");
            System.out.println("Server running on port " + PORT);
            System.out.println("Waiting for client connections...\n");

            while (running && !shutdownRequested) {
                try {
                    serverSocket.setSoTimeout(1000); // Timeout check for shutdown...

                    Socket clientSocket = serverSocket.accept();
                    threadPool.execute(new ClientHandler(clientSocket));

                } catch (SocketTimeoutException e) {
                    if (shutdownRequested) { // Timeout shutdown requested...
                        break;
                    }
                } catch (IOException e) {
                    if (running) {
                        ServerLogger.logError("Error accepting client connection: " + e.getMessage());
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