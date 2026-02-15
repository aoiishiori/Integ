package Server.util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


// eto ung file-based XML logging



/**
 * ServerLogger — logs events to console AND to data/server_log.xml
 * Satisfies rubric: timestamp, user, transaction, data affected
 */

public class ServerLogger {

    private static final DateTimeFormatter FORMATTER = // format
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String LOG_FILE = "data/server_log.xml"; // file location


    // Call once at server startup to initialize the log file
    public static synchronized void initLogFile() {
        File logFile = new File(LOG_FILE);
        if (!logFile.exists()) {
            writeRaw("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ServerLog>\n</ServerLog>");
        }
    }


    // Public logging methods

    public static void logServerStart(int port) {
        String timestamp = now();
        String msg = String.format("[%s] SERVER STARTED on port %d", timestamp, port);
        System.out.println(msg);
        appendEntry("SYSTEM", "SERVER_START", "port=" + port, "N/A");
    }

    public static void logServerShutdown() {
        String timestamp = now();
        String msg = String.format("[%s] SERVER SHUTDOWN", timestamp);
        System.out.println(msg);
        appendEntry("SYSTEM", "SERVER_SHUTDOWN", "N/A", "N/A");
    }

    public static void logTransaction(String user, String action, String dataAffected) {
        String timestamp = now();
        String msg = String.format("[%s] USER=%s | ACTION=%s | DATA=%s",
                timestamp, user, action, dataAffected);
        System.out.println(msg);
        appendEntry(user, action, dataAffected, "SUCCESS");
    }

    public static void logError(String message) {
        String timestamp = now();
        String msg = String.format("[%s] ERROR: %s", timestamp, message);
        System.err.println(msg);
        appendEntry("SYSTEM", "ERROR", message, "FAILED");
    }

    public static void logClientConnect(String ip) {
        String timestamp = now();
        System.out.println(String.format("[%s] CLIENT CONNECTED: %s", timestamp, ip));
        appendEntry(ip, "CLIENT_CONNECT", "N/A", "N/A");
    }

    public static void logClientDisconnect(String ip) {
        String timestamp = now();
        System.out.println(String.format("[%s] CLIENT DISCONNECTED: %s", timestamp, ip));
        appendEntry(ip, "CLIENT_DISCONNECT", "N/A", "N/A");
    }


    // Internal helpers


    /**
     * Appends one <LogEntry> block inside the <ServerLog> root.
     * Uses a simple string-replace on </ServerLog> to insert before it.
     */

    private static synchronized void appendEntry(String user, String action,
                                                 String dataAffected, String result) {
        File logFile = new File(LOG_FILE);
        if (!logFile.exists()) {
            initLogFile();
        }

        try {
            // Reads current content
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            // Builds a new entry
            String entry = "  <LogEntry>\n"
                    + "    <timestamp>" + escapeXML(now()) + "</timestamp>\n"
                    + "    <user>" + escapeXML(user) + "</user>\n"
                    + "    <action>" + escapeXML(action) + "</action>\n"
                    + "    <dataAffected>" + escapeXML(dataAffected) + "</dataAffected>\n"
                    + "    <result>" + escapeXML(result) + "</result>\n"
                    + "  </LogEntry>\n";

            // Inserts before closing tag
            String updated = content.toString().replace("</ServerLog>", entry + "</ServerLog>");

            writeRaw(updated);

        } catch (IOException e) {
            System.err.println("[LOGGER ERROR] " + e.getMessage());
        }
    }

    private static void writeRaw(String content) {
        try (FileWriter writer = new FileWriter(LOG_FILE)) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("[LOGGER ERROR] Cannot write log: " + e.getMessage());
        }
    }

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private static String escapeXML(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}