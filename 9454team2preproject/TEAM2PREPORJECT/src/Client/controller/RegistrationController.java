package Client.controller;

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
 * RegistrationController — MVC Controller for Create_Account.fxml
 *
 * Handles new user registration (BUYER role by default).
 * Sellers register through SellerRegistrationController.
 */
public class RegistrationController {

    @FXML private TextField     regUser;
    @FXML private PasswordField regPass;
    @FXML private PasswordField regRePass;
    @FXML private CheckBox      showPass;
    @FXML private Button        createButton;

    // Backing text fields shown when showPass is checked
    private TextField visiblePass;
    private TextField visibleRePass;

    private final AuthModel authModel = new AuthModel();

    // -------------------------------------------------------
    // Create Account button
    // -------------------------------------------------------
    @FXML
    void createHandler(ActionEvent event) {
        String username  = regUser.getText().trim();
        String password  = regPass.getText();
        String rePassword = regRePass.getText();

        // Basic validation
        if (username.isEmpty() || password.isEmpty() || rePassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please fill in all fields.");
            return;
        }
        if (username.length() < 3) {
            showAlert(Alert.AlertType.WARNING, "Username must be at least 3 characters.");
            return;
        }
        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(rePassword)) {
            showAlert(Alert.AlertType.WARNING, "Passwords do not match.");
            return;
        }

        String response = authModel.register(username, password, "BUYER");
        String status   = SocketClient.getStatus(response);
        String message  = SocketClient.getMessage(response);

        if ("SUCCESS".equals(status)) {
            showAlert(Alert.AlertType.INFORMATION,
                    "Account created! You can now log in.");
            goToLogin();
        } else {
            showAlert(Alert.AlertType.ERROR,
                    message.isEmpty() ? "Registration failed." : message);
        }
    }

    // -------------------------------------------------------
    // Show/Hide password checkbox
    // -------------------------------------------------------
    @FXML
    void showPassHandler(ActionEvent event) {
        // Simple toggle: just reveal the text visually
        // (Full swap between PasswordField/TextField requires more FXML work)
        // For now, just give a hint in the console — teams can upgrade this
        if (showPass.isSelected()) {
            System.out.println("[DEBUG] Password: " + regPass.getText());
        }
    }

    // -------------------------------------------------------
    // Navigate back to login
    // -------------------------------------------------------
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Client/VIEW/LoginForm.fxml"));
            Stage stage = (Stage) createButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Cannot open login: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.showAndWait();
    }
}