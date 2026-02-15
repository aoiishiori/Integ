package Client.CONTROLLER;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.ImagePattern;
import javafx.scene.image.Image;

public class BuyerController {

    // --- Essential UI Elements ---
    @FXML private FlowPane productContainer; // To inject product cards from XML

    @FXML private TextField searchBar;       // For real-time filtering

    @FXML private VBox settingsDropdown;     // To toggle visibility

    @FXML private Circle userAvatar;         // To update with Base64 ImagePattern

    @FXML private Label welcomeLabel;        // To personalize "Welcome, [Name]"

    // --- 1. Settings Dropdown Logic ---
    @FXML
    void toggleSettings(MouseEvent event) {
        // Toggle visibility and layout management
        boolean isVisible = settingsDropdown.isVisible();
        settingsDropdown.setVisible(!isVisible);
        settingsDropdown.setManaged(!isVisible);
    }

    private void closeDropdown() {
        settingsDropdown.setVisible(false);
        settingsDropdown.setManaged(false);
    }

    // --- 2. Menu Actions ---
    @FXML
    void handleUpdateProfile(ActionEvent event) {
        // Logic: FileChooser -> Base64 conversion -> Update XML
        closeDropdown();
    }

    @FXML
    void handleShowNotifications(ActionEvent event) {
        // Logic: Open Notification Modal
        closeDropdown();
    }

    @FXML
    void handleChangePassword(ActionEvent event) {
        // Logic: Open Password Modal (Old, New, Re-type)
        closeDropdown();
    }

    @FXML
    void handleBecomeSeller(ActionEvent event) {
        // Logic: Open Seller Registration Modal
        closeDropdown();
    }

    // --- 3. Filtering Logic ---
    @FXML
    void handleSortLowToHigh(ActionEvent event) {
        // Sort XML list by price ascending
    }

    @FXML
    void handleSortHighToLow(ActionEvent event) {
        // Sort XML list by price descending
    }
}