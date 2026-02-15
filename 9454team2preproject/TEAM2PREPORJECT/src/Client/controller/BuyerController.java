package Client.controller;

import Client.BUYER.MODEL.BuyerModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * BuyerController — MVC Controller for Buyer_Dashboard.fxml
 */
public class BuyerController implements Initializable {

    @FXML private FlowPane productContainer;
    @FXML private TextField searchBar;
    @FXML private VBox settingsDropdown;
    @FXML private Circle userAvatar;
    @FXML private Label welcomeLabel;

    private final BuyerModel buyerModel  = new BuyerModel();
    private final String     username    = SessionData.getUsername();
    private List<String[]>   currentList; // holds last loaded product list

    // -------------------------------------------------------
    // Initialize
    // -------------------------------------------------------
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        welcomeLabel.setText("Welcome back, " + username + "!");

        // Live search — re-filter on every keypress
        searchBar.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                renderProducts(currentList);
            } else {
                List<String[]> results = buyerModel.searchProducts(username, newVal.trim());
                renderProducts(results);
            }
        });

        loadAllProducts();
        closeDropdown();
    }

    // -------------------------------------------------------
    // Load all available products from the server
    // -------------------------------------------------------
    private void loadAllProducts() {
        currentList = buyerModel.fetchAllProducts(username);
        renderProducts(currentList);
    }

    // -------------------------------------------------------
    // Render product cards into the FlowPane
    // -------------------------------------------------------
    private void renderProducts(List<String[]> products) {
        productContainer.getChildren().clear();

        if (products.isEmpty()) {
            Label empty = new Label("No products found.");
            productContainer.getChildren().add(empty);
            return;
        }

        for (String[] p : products) {
            // p = [productId, sellerUsername, name, category,
            //      originalPrice, discountedPrice, availableQuantity, expiryDate, status]
            VBox card = buildProductCard(p);
            productContainer.getChildren().add(card);
        }
    }

    // -------------------------------------------------------
    // Build a single product card
    // -------------------------------------------------------
    private VBox buildProductCard(String[] p) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; "
                + "-fx-border-color: #dee2e6; "
                + "-fx-border-radius: 8; "
                + "-fx-background-radius: 8; "
                + "-fx-padding: 15; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        card.setPrefWidth(200);

        Label name     = new Label(p[2]);           // product name
        name.setFont(new Font("System Bold", 14));
        name.setWrapText(true);

        Label category = new Label("📦 " + p[3]);
        Label seller   = new Label("🏪 " + p[1]);
        Label price    = new Label("₱" + p[5]);     // discounted price
        price.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16;");

        Label origPrice = new Label("Original: ₱" + p[4]);
        origPrice.setStyle("-fx-text-fill: #95a5a6; -fx-strikethrough: true; -fx-font-size: 11;");

        Label qty    = new Label("Available: " + p[6]);
        Label expiry = new Label("Expires: " + p[7]);
        expiry.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");

        Button buyBtn = new Button("BUY NOW");
        buyBtn.setMaxWidth(Double.MAX_VALUE);
        buyBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 5;");
        buyBtn.setOnAction(e -> handleBuy(p[0], p[2])); // productId, name

        card.getChildren().addAll(name, category, seller, price, origPrice, qty, expiry, buyBtn);
        return card;
    }

    // -------------------------------------------------------
    // Handle Buy button on a product card
    // -------------------------------------------------------
    private void handleBuy(String productId, String productName) {
        // Ask for quantity
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Buy Product");
        dialog.setHeaderText("Buying: " + productName);
        dialog.setContentText("Enter quantity:");

        dialog.showAndWait().ifPresent(input -> {
            try {
                int qty = Integer.parseInt(input.trim());
                if (qty <= 0) throw new NumberFormatException();

                String[] result = buyerModel.buyProduct(username, productId, qty);
                Alert.AlertType type = "SUCCESS".equals(result[0])
                        ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
                new Alert(type, result[1], ButtonType.OK).showAndWait();

                if ("SUCCESS".equals(result[0])) {
                    loadAllProducts(); // Refresh
                }
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING,
                        "Please enter a valid quantity.", ButtonType.OK).showAndWait();
            }
        });
    }

    // -------------------------------------------------------
    // Settings dropdown
    // -------------------------------------------------------
    @FXML
    void toggleSettings(MouseEvent event) {
        boolean isVisible = settingsDropdown.isVisible();
        settingsDropdown.setVisible(!isVisible);
        settingsDropdown.setManaged(!isVisible);
    }

    private void closeDropdown() {
        settingsDropdown.setVisible(false);
        settingsDropdown.setManaged(false);
    }

    // -------------------------------------------------------
    // Sort buttons
    // -------------------------------------------------------
    @FXML
    void handleSortLowToHigh(ActionEvent event) {
        if (currentList == null) return;
        currentList.sort(Comparator.comparingDouble(p -> parseDouble(p[5])));
        renderProducts(currentList);
    }

    @FXML
    void handleSortHighToLow(ActionEvent event) {
        if (currentList == null) return;
        currentList.sort((a, b) -> Double.compare(parseDouble(b[5]), parseDouble(a[5])));
        renderProducts(currentList);
    }

    // -------------------------------------------------------
    // Menu actions
    // -------------------------------------------------------
    @FXML
    void handleUpdateProfile(ActionEvent event) {
        new Alert(Alert.AlertType.INFORMATION,
                "Profile picture update coming soon.", ButtonType.OK).showAndWait();
        closeDropdown();
    }

    @FXML
    void handleShowNotifications(ActionEvent event) {
        List<String[]> purchases = buyerModel.fetchMyPurchases(username);
        StringBuilder sb = new StringBuilder("Your Recent Purchases:\n\n");
        if (purchases.isEmpty()) {
            sb.append("No purchases yet.");
        } else {
            for (String[] t : purchases) {
                // [transactionId, productId, sellerUsername, quantity, timestamp]
                sb.append("📦 ").append(t[0])
                        .append("\n   Product: ").append(t[1])
                        .append("\n   Seller: ").append(t[2])
                        .append("\n   Qty: ").append(t[3])
                        .append("\n   Date: ").append(t[4])
                        .append("\n\n");
            }
        }
        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setPrefSize(400, 300);
        Stage stage = new Stage();
        stage.setTitle("My Purchases");
        stage.setScene(new Scene(area));
        stage.show();
        closeDropdown();
    }

    @FXML
    void handleChangePassword(ActionEvent event) {
        closeDropdown();
        // Password change dialog
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        PasswordField oldPw  = new PasswordField();
        PasswordField newPw  = new PasswordField();
        PasswordField conf   = new PasswordField();
        grid.add(new Label("Old Password:"), 0, 0); grid.add(oldPw, 1, 0);
        grid.add(new Label("New Password:"), 0, 1); grid.add(newPw, 1, 1);
        grid.add(new Label("Confirm:"),      0, 2); grid.add(conf,  1, 2);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK)
                return new String[]{oldPw.getText(), newPw.getText(), conf.getText()};
            return null;
        });
        dialog.showAndWait().ifPresent(fields -> {
            if (!fields[1].equals(fields[2])) {
                new Alert(Alert.AlertType.WARNING, "Passwords don't match.", ButtonType.OK).showAndWait();
                return;
            }
            Client.model.AuthModel auth = new Client.model.AuthModel();
            String resp   = auth.changePassword(username, fields[0], fields[1]);
            String status = Client.util.SocketClient.getStatus(resp);
            String msg    = Client.util.SocketClient.getMessage(resp);
            new Alert("SUCCESS".equals(status) ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                    msg, ButtonType.OK).showAndWait();
        });
    }

    @FXML
    void handleBecomeSeller(ActionEvent event) {
        closeDropdown();
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Client/VIEW/RegistrationForm.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Seller Registration");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Cannot open form: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------
    private double parseDouble(String val) {
        try { return Double.parseDouble(val); }
        catch (NumberFormatException e) { return 0.0; }
    }
}