package Server;

import Server.util.ServerLogger;
import java.io.*;
import java.net.Socket;

/**
 * ClientHandler — runs on its own thread for each connected client.
 * Reads the full XML request, passes it to RequestHandler, and
 * writes the XML response back.
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final RequestHandler requestHandler;

    public ClientHandler(Socket socket) {
        this.clientSocket   = socket;
        this.requestHandler = new RequestHandler();
    }

    @Override
    public void run() {
        String clientIP = clientSocket.getInetAddress().getHostAddress();
        ServerLogger.logClientConnect(clientIP);

        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream()), true)
        ) {
            // Read the full XML request (ends at </request>)
            StringBuilder requestBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                requestBuilder.append(line).append("\n");
                // Stop reading when we see the closing tag
                if (line.trim().equals("</request>")) break;
            }

            String xmlRequest = requestBuilder.toString().trim();

            if (xmlRequest.isEmpty()) {
                out.println(buildError("Empty request received."));
                return;
            }

            // Process and respond
            String response = requestHandler.processRequest(xmlRequest);
            out.println(response);

        } catch (IOException e) {
            ServerLogger.logError("ClientHandler error [" + clientIP + "]: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                ServerLogger.logClientDisconnect(clientIP);
            } catch (IOException e) {
                ServerLogger.logError("Error closing socket: " + e.getMessage());
            }
        }
    }

    private String buildError(String msg) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<response><status>ERROR</status><message>"
                + msg + "</message></response>";
    }
}