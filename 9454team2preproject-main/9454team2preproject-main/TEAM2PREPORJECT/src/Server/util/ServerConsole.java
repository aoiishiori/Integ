package Server.util;

import Server.Server;
import java.util.Scanner;

public class ServerConsole implements Runnable {

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {

                case "shutdown":
                case "stop":
                case "exit":
                    System.out.println("Shutdown command received.");
                    ServerLogger.logServerShutdown();
                    Server.requestShutdown();
                    return;

                default:
                    System.out.println("Unknown command.");
            }
        }
    }
}
