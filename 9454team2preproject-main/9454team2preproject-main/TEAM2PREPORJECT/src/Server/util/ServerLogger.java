package Server.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void logServerStart(int port) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println(String.format("[%s] SERVER STARTED on port %d", timestamp, port));
    }

    public static void logServerShutdown() {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println(String.format("[%s] SERVER SHUTDOWN", timestamp));
    }

    public static void logError(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.err.println(String.format("[%s] ERROR: %s", timestamp, message));
    }
}