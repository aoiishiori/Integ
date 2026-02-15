package Client.controller;

import Client.model.AuthModel;
import Client.util.SocketClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * SellerRegistrationController — MVC Controller for RegistrationForm.fxml
 *
 * Allows an existing BUYER to apply for a SELLER account.
 * Admin must approve it before the user can log in as SELLER.
 */
public class SellerRegistrationController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField storeNameField;
    @FXML private TextArea  storeDescArea;
    @FXML private TextField gcashField;
    @FXML private Label     idFileName;
    @FXML private Label     permitFileName;
    @FXML private Button    submitButton;   // if FXML has fx:id, otherwise use handleSubmit lookup

    private final AuthModel authModel = new AuthModel();

    // -------------------------------------------------------
    // Upload ID (placeholder — file handling can be extended)
    // -------------------------------------------------------
    @FXML
    void handleUploadID(ActionEvent event) {
        // In a real app: use FileChooser, encode file as Base64, send in XML
        idFileName.setText("ID_uploaded.jpg (simulated)");
    }

    @FXML
    void handleUploadPermit(ActionEvent event) {
        permitFileName.setText("Permit_uploaded.pdf (simulated)");
    }

    // -------------------------------------------------------
    // Submit seller application
    // -------------------------------------------------------
    @FXML
    void handleSubmit(ActionEvent event) {
        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String storeName = storeNameField.getText().trim();
        String gcash     = gcashField.getText().trim();

        // Basic validation
        if (firstName.isEmpty() || lastName.isEmpty() || storeName.isEmpty() || gcash.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please fill in all required fields.");
            return;
        }
        if (!gcash.matches("09\\d{9}")) {
            showAlert(Alert.AlertType.WARNING, "GCash number must be in format 09XXXXXXXXX.");
            return;
        }

        // The current logged-in user applies for seller upgrade
        String currentUsername = SessionData.getUsername();

        if (currentUsername.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "You must be logged in to apply.");
            return;
        }

        // Register a new seller account linked to the buyer's username.
        // We use UPDATE_USER_STATUS via a special "BECOME_SELLER" approach:
        // For simplicity, we send a new REGISTER with SELLER role under same username base.
        // In production you'd add a dedicated "APPLY_SELLER" action.
        //
        // For this project: send a REGISTER request with role=SELLER
        // (server sets status=PENDING automatically for SELLER role)
        String sellerUsername = currentUsername + "_seller";
        String tempPassword   = "changeme123"; // Buyer will know their own password

        String response = authModel.registerSeller(sellerUsername, tempPassword);
        String status   = SocketClient.getStatus(response);
        String message  = SocketClient.getMessage(response);

        if ("SUCCESS".equals(status)) {
            showAlert(Alert.AlertType.INFORMATION,
                    "Seller application submitted!\n\n"
                            + "Your seller account '" + sellerUsername + "' is pending admin approval.\n"
                            + "Default password: changeme123 (change after approval).\n\n"
                            + "Store: " + storeName);
            closeWindow();
        } else {
            showAlert(Alert.AlertType.ERROR, message);
        }
    }

    // -------------------------------------------------------
    // Cancel button
    // -------------------------------------------------------
    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private void closeWindow() {
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String msg) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }
}