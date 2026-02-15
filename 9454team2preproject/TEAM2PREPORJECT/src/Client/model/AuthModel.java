package Client.model;

import Client.util.SocketClient;

/**
 * AuthModel — MVC Model for authentication.
 * Handles LOGIN and REGISTER requests to the server.
 */
public class AuthModel {

    /**
     * Attempts to log in with the given credentials.
     * Returns the full XML response from the server.
     *
     * Callers check: SocketClient.isSuccess(response)
     *                SocketClient.getStatus(response)  — "SUCCESS", "PENDING", "DENIED", "FAILED"
     *                SocketClient.getMessage(response)
     *                SocketClient.getDataBlock(response) — contains <role> and <accountId>
     */
    public String login(String username, String password) {
        String data = "    <password>" + escXML(password) + "</password>";
        String request = SocketClient.buildRequest("LOGIN", username, data);
        return SocketClient.sendRequest(request);
    }

    /**
     * Registers a new account.
     * role should be "BUYER" (sellers register via SellerRegistrationController).
     * Returns full XML response string.
     */
    public String register(String username, String password, String role) {
        String data = "    <password>" + escXML(password) + "</password>\n"
                + "    <role>" + escXML(role) + "</role>";
        String request = SocketClient.buildRequest("REGISTER", username, data);
        return SocketClient.sendRequest(request);
    }

    /**
     * Submits a seller registration request.
     * Role is set to SELLER — server will mark status as PENDING.
     */
    public String registerSeller(String username, String password) {
        return register(username, password, "SELLER");
    }

    /**
     * Changes the password of the currently logged-in user.
     */
    public String changePassword(String username, String oldPassword, String newPassword) {
        String data = "    <oldPassword>" + escXML(oldPassword) + "</oldPassword>\n"
                + "    <newPassword>" + escXML(newPassword) + "</newPassword>";
        String request = SocketClient.buildRequest("CHANGE_PASSWORD", username, data);
        return SocketClient.sendRequest(request);
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------
    private String escXML(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}