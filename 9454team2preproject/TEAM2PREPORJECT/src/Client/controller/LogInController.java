package Client.controller;

import Client.controller.SessionData;
import Client.model.AuthModel;
import Client.util.SocketClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * LogInController — MVC Controller for LoginForm.fxml
 *
 * On success, stores the logged-in username and role in SessionData,
 * then opens the correct dashboard (Buyer, Seller, or Admin).
 */
public class LogInController {

    @FXML private TextField     UserNameInput;
    @FXML private PasswordField PasswordInput;
    @FXML private Button        LogInButton;
    @FXML private Hyperlink     CreateAccount;

    private final AuthModel authModel = new AuthModel();

    // -------------------------------------------------------
    // Log In button
    // -------------------------------------------------------
    @FXML
    void handleLogIn(ActionEvent event) {
        String username = UserNameInput.getText().trim();
        String password = PasswordInput.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please enter both username and password.");
            return;
        }

        String response = authModel.login(username, password);
        String status   = SocketClient.getStatus(response);
        String message  = SocketClient.getMessage(response);

        switch (status) {
            case "SUCCESS":
                // Extract role from <data> block
                String dataBlock = SocketClient.getDataBlock(response);
                String role = extractTag(dataBlock, "role");

                // Save session
                SessionData.setUsername(username);
                SessionData.setRole(role);

                openDashboard(role);
                break;

            case "PENDING":
                showAlert(Alert.AlertType.INFORMATION, message);
                break;

            case "DENIED":
                showAlert(Alert.AlertType.ERROR, message);
                break;

            default: // FAILED or ERROR
                showAlert(Alert.AlertType.ERROR,
                        message.isEmpty() ? "Login failed. Please try again." : message);
        }
    }

    // -------------------------------------------------------
    // "Create an Account" hyperlink
    // -------------------------------------------------------
    @FXML
    void handleSignUp(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Client/VIEW/Create_Account.fxml"));
            Stage stage = (Stage) CreateAccount.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Create Account");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Cannot open registration: " + e.getMessage());
        }
    }

    // -------------------------------------------------------
    // Open the correct dashboard based on role
    // -------------------------------------------------------
    private void openDashboard(String role) {
        try {
            String fxml;
            String title;

            switch (role) {
                case "ADMIN":
                    fxml  = "/Client/VIEW/Admin_Dashboard.fxml";
                    title = "Admin Panel";
                    break;
                case "SELLER":
                    fxml  = "/Client/VIEW/Seller_Dashboard.fxml";
                    title = "Seller Dashboard";
                    break;
                default: // BUYER
                    fxml  = "/Client/VIEW/Buyer_Dashboard.fxml";
                    title = "Marketplace";
                    break;
            }

            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage  stage = (Stage) LogInButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Cannot open dashboard: " + e.getMessage());
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private String extractTag(String xml, String tagName) {
        String open  = "<"  + tagName + ">";
        String close = "</" + tagName + ">";
        int start = xml.indexOf(open);
        int end   = xml.indexOf(close);
        if (start == -1 || end == -1) return "";
        return xml.substring(start + open.length(), end).trim();
    }
}