package Client.Admin.Model;

import Client.util.SocketClient;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminModel — MVC Model for the Admin dashboard.
 * All server communication uses proper XML protocol via SocketClient.
 */
public class AdminModel {

    // -------------------------------------------------------
    // Fetch all user accounts
    // Returns a list of String arrays: [accountId, username, role, status]
    // -------------------------------------------------------
    public List<String[]> fetchAllUsers(String adminUsername) {
        List<String[]> users = new ArrayList<>();

        String request  = SocketClient.buildRequest("FETCH_ALL_USERS", adminUsername);
        String response = SocketClient.sendRequest(request);

        if (!SocketClient.isSuccess(response)) return users;

        // Parse the <users><user>...</user></users> block
        String dataBlock = SocketClient.getDataBlock(response);
        String[] userBlocks = dataBlock.split("</user>");

        for (String block : userBlocks) {
            if (!block.contains("<user>")) continue;
            String inner = block.substring(block.indexOf("<user>") + 6);

            String accountId = extractTag(inner, "accountId");
            String username  = extractTag(inner, "username");
            String role      = extractTag(inner, "role");
            String status    = extractTag(inner, "status");

            users.add(new String[]{accountId, username, role, status});
        }

        return users;
    }

    // -------------------------------------------------------
    // Approve or Deny a user (newStatus = "APPROVED" or "DENIED")
    // -------------------------------------------------------
    public boolean updateUserStatus(String adminUsername,
                                    String targetUsername, String newStatus) {
        String data = "    <targetUsername>" + targetUsername + "</targetUsername>\n"
                + "    <newStatus>" + newStatus + "</newStatus>";
        String request  = SocketClient.buildRequest("UPDATE_USER_STATUS", adminUsername, data);
        String response = SocketClient.sendRequest(request);
        return SocketClient.isSuccess(response);
    }

    // -------------------------------------------------------
    // Delete a user account
    // -------------------------------------------------------
    public boolean deleteUser(String adminUsername, String targetUsername) {
        String data = "    <targetUsername>" + targetUsername + "</targetUsername>";
        String request  = SocketClient.buildRequest("DELETE_USER", adminUsername, data);
        String response = SocketClient.sendRequest(request);
        return SocketClient.isSuccess(response);
    }

    // -------------------------------------------------------
    // Fetch transaction logs as raw XML string
    // -------------------------------------------------------
    public String fetchLogs(String adminUsername) {
        String request  = SocketClient.buildRequest("FETCH_LOGS", adminUsername);
        String response = SocketClient.sendRequest(request);

        if (SocketClient.isSuccess(response)) {
            return SocketClient.getDataBlock(response);
        }
        return "Could not fetch logs: " + SocketClient.getMessage(response);
    }

    // -------------------------------------------------------
    // Fetch all transactions
    // -------------------------------------------------------
    public String fetchAllTransactions(String adminUsername) {
        String request  = SocketClient.buildRequest("FETCH_ALL_TRANSACTIONS", adminUsername);
        String response = SocketClient.sendRequest(request);

        if (SocketClient.isSuccess(response)) {
            return SocketClient.getDataBlock(response);
        }
        return "Could not fetch transactions: " + SocketClient.getMessage(response);
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------
    private String extractTag(String xml, String tagName) {
        String open  = "<"  + tagName + ">";
        String close = "</" + tagName + ">";
        int start = xml.indexOf(open);
        int end   = xml.indexOf(close);
        if (start == -1 || end == -1) return "";
        return xml.substring(start + open.length(), end).trim();
    }
}