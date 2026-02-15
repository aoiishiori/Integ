package Client.ADMIN.MODEL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AdminModel {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 1234;
    // Fetch all users from server
    public List<String[]> fetchAllUsers() {
        List<String[]> users = new ArrayList<>();
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.write(("ADMIN_FETCH_USERS").getBytes());
            out.flush();
            String line;
            while (!(line = in.readLine()).equals("END")) {
                String[] userData = line.split("\\|");
                users.add(userData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
    // Update user status (APPROVED/PENDING)
    public boolean updateUserStatus(String userId, String status) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.write(("ADMIN_UPDATE_USER|" + userId + "|" + status).getBytes());
            out.flush();
            return in.readLine().equals("SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    // Delete user account
    public boolean deleteUserAccount(String userId) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.write(("ADMIN_DELETE_USER|" + userId).getBytes());
            out.flush();
            return in.readLine().equals("SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    // Fetch XML transaction logs
    public String fetchTransactionLogs() {
        StringBuilder logs = new StringBuilder();
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.write(("ADMIN_FETCH_LOGS").getBytes());
            out.flush();
            String line;
            while (!(line = in.readLine()).equals("END")) {
                logs.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logs.append("Error fetching logs: ").append(e.getMessage());
        }
        return logs.toString();
    }

}
