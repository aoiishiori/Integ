package Server.controller;

import Server.model.Account;
import Server.util.ServerLogger;
import Server.util.XMLParser;
import Server.util.XMLReader;
import Server.util.XMLWriter;
import org.w3c.dom.Element;

import java.util.List;
import java.util.UUID;

/*
LOGIN, REGISTER, Fetch All Users, Update User Status, Delete User
Change password
 */

public class AccountController {


    public String login(String username, Element dataElement) {
        String password = XMLParser.getTagValue(dataElement, "password");

        List<Account> accounts = XMLReader.readAccounts();
        for (Account a : accounts) {
            if (a.getUsername().equals(username) && a.getPassword().equals(password)) {
                if (a.getStatus().equals("PENDING")) {
                    return XMLParser.createResponse("PENDING",
                            "Your account is awaiting admin approval.");
                }
                if (a.getStatus().equals("DENIED")) {
                    return XMLParser.createResponse("DENIED",
                            "Your account has been denied.");
                }
                // Approved — return role info
                ServerLogger.logTransaction(username, "LOGIN",
                        "role=" + a.getRole());
                String data = "<role>" + a.getRole() + "</role>"
                        + "<accountId>" + a.getAccountId() + "</accountId>";
                return XMLParser.createResponseWithData("SUCCESS", "Login successful.", data);
            }
        }
        return XMLParser.createResponse("FAILED", "Invalid username or password.");
    }

    public String register(String username, Element dataElement) {
        String password = XMLParser.getTagValue(dataElement, "password");
        String role     = XMLParser.getTagValue(dataElement, "role");
        if (role == null || role.isEmpty()) role = "BUYER";

        List<Account> accounts = XMLReader.readAccounts();

        // Check duplicate username
        for (Account a : accounts) {
            if (a.getUsername().equalsIgnoreCase(username)) {
                return XMLParser.createResponse("FAILED", "Username already exists.");
            }
        }

        // Create new account — buyers are auto-approved, sellers need admin approval
        String status = role.equals("SELLER") ? "PENDING" : "APPROVED";
        Account newAccount = new Account(
                "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                username, password, role, status
        );

        accounts.add(newAccount);
        XMLWriter.writeAccounts(accounts);

        ServerLogger.logTransaction(username, "REGISTER",
                "role=" + role + ", status=" + status);

        String message = role.equals("SELLER")
                ? "Registration submitted. Awaiting admin approval."
                : "Account created successfully.";

        return XMLParser.createResponse("SUCCESS", message);
    }

    public String fetchAllUsers(String requestingUser) {
        List<Account> accounts = XMLReader.readAccounts();

        StringBuilder data = new StringBuilder("<users>");
        for (Account a : accounts) {
            data.append("<user>")
                    .append("<accountId>").append(a.getAccountId()).append("</accountId>")
                    .append("<username>").append(a.getUsername()).append("</username>")
                    .append("<role>").append(a.getRole()).append("</role>")
                    .append("<status>").append(a.getStatus()).append("</status>")
                    .append("</user>");
        }
        data.append("</users>");

        ServerLogger.logTransaction(requestingUser, "FETCH_ALL_USERS",
                "count=" + accounts.size());

        return XMLParser.createResponseWithData("SUCCESS", "Users fetched.", data.toString());
    }

    public String updateUserStatus(String adminUsername, Element dataElement) {
        String targetUsername = XMLParser.getTagValue(dataElement, "targetUsername");
        String newStatus      = XMLParser.getTagValue(dataElement, "newStatus");

        List<Account> accounts = XMLReader.readAccounts();
        for (Account a : accounts) {
            if (a.getUsername().equals(targetUsername)) {
                a.setStatus(newStatus);
                XMLWriter.writeAccounts(accounts);
                ServerLogger.logTransaction(adminUsername, "UPDATE_USER_STATUS",
                        "target=" + targetUsername + ", newStatus=" + newStatus);
                return XMLParser.createResponse("SUCCESS",
                        "User " + targetUsername + " status updated to " + newStatus);
            }
        }
        return XMLParser.createResponse("FAILED", "User not found: " + targetUsername);
    }

    public String deleteUser(String adminUsername, Element dataElement) {
        String targetUsername = XMLParser.getTagValue(dataElement, "targetUsername");

        List<Account> accounts = XMLReader.readAccounts();
        boolean removed = accounts.removeIf(a -> a.getUsername().equals(targetUsername));

        if (removed) {
            XMLWriter.writeAccounts(accounts);
            ServerLogger.logTransaction(adminUsername, "DELETE_USER",
                    "deleted=" + targetUsername);
            return XMLParser.createResponse("SUCCESS",
                    "User " + targetUsername + " deleted.");
        }
        return XMLParser.createResponse("FAILED", "User not found: " + targetUsername);
    }

    public String changePassword(String username, Element dataElement) {
        String oldPassword = XMLParser.getTagValue(dataElement, "oldPassword");
        String newPassword = XMLParser.getTagValue(dataElement, "newPassword");

        List<Account> accounts = XMLReader.readAccounts();
        for (Account a : accounts) {
            if (a.getUsername().equals(username)) {
                if (!a.getPassword().equals(oldPassword)) {
                    return XMLParser.createResponse("FAILED", "Old password is incorrect.");
                }
                a.setPassword(newPassword);
                XMLWriter.writeAccounts(accounts);
                ServerLogger.logTransaction(username, "CHANGE_PASSWORD", "N/A");
                return XMLParser.createResponse("SUCCESS", "Password changed successfully.");
            }
        }
        return XMLParser.createResponse("FAILED", "User not found.");
    }

    public String fetchLogs(String adminUsername) {
        ServerLogger.logTransaction(adminUsername, "FETCH_LOGS", "N/A");
        String logXML = Server.util.XMLReader.readLogsAsXML();
        return XMLParser.createResponseWithData("SUCCESS", "Logs fetched.", logXML);
    }

}
