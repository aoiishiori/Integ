package Server;

import Server.util.ServerLogger;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        System.out.println("Client connected: " + clientAddress);

        try (
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream())
                );
                PrintWriter output = new PrintWriter(
                        clientSocket.getOutputStream(), true
                )
        ) {
            StringBuilder request = new StringBuilder();
            String line;

            while ((line = input.readLine()) != null) {
                request.append(line).append("\n");
                if (line.trim().equals("</request>")) break;
            }

            System.out.println("----- CLIENT MESSAGE -----");
            System.out.println(request.toString());
            System.out.println("--------------------------");

            output.println("ACK");

        } catch (IOException e) {
            ServerLogger.logError("Client error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client disconnected: " + clientAddress);
            } catch (IOException e) {
                ServerLogger.logError("Error closing socket: " + e.getMessage());
            }
        }
    }
}
