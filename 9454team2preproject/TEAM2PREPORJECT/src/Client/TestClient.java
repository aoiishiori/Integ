package Client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class TestClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {

        System.out.println("Connecting to server...");

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true);
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Connected to server.");
            System.out.print("Enter message to send: ");

            String userInput = scanner.nextLine();

            String request =
                    "<request>\n" +
                            "  <message>" + userInput + "</message>\n" +
                            "</request>";

            out.println(request);

            String response = in.readLine();
            if (response != null) {
                System.out.println("Server says: " + response);
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }

        System.out.println("Client terminated.");
    }
}
