package Client.controller;

import Client.SELLER.MODEL.SellerModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * SellerController — MVC Controller for Seller_Dashboard.fxml
 */
public class SellerController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private TableView<ProductRow> mainTable;
    @FXML private VBox settingsDropdown;
    @FXML private Circle userAvatar;
    @FXML private Label welcomeLabel;

    private final SellerModel sellerModel = new SellerModel();
    private final String      username    = SessionData.getUsername();
    private ObservableList<ProductRow> productRows = FXCollections.observableArrayList();

    // Columns (added programmatically since FXML has generic placeholders)
    private TableColumn<ProductRow, String> colProductId;
    private TableColumn<ProductRow, String> colName;
    private TableColumn<ProductRow, String> colCategory;
    private TableColumn<ProductRow, String> colPrice;
    private TableColumn<ProductRow, String> colQty;
    private TableColumn<ProductRow, String> colExpiry;
    private TableColumn<ProductRow, String> colStatus;

    // -------------------------------------------------------
    // Initialize
    // -------------------------------------------------------
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        welcomeLabel.setText("Welcome, " + username);

        // Set up table columns programmatically
        setupTableColumns();
        mainTable.setItems(productRows);

        showDashboard(null);
        closeDropdown();
    }

    @SuppressWarnings("unchecked")
    private void setupTableColumns() {
        mainTable.getColumns().clear();

        colProductId = makeColumn("Product ID", "productId", 100);
        colName      = makeColumn("Name",       "name",      150);
        colCategory  = makeColumn("Category",   "category",  100);
        colPrice     = makeColumn("Price (₱)",  "price",     90);
        colQty       = makeColumn("Qty",        "quantity",  60);
        colExpiry    = makeColumn("Expires",    "expiryDate",110);
        colStatus    = makeColumn("Status",     "status",    90);

        mainTable.getColumns().addAll(
                colProductId, colName, colCategory,
                colPrice, colQty, colExpiry, colStatus);
    }

    private TableColumn<ProductRow, String> makeColumn(String title, String property, int width) {
        TableColumn<ProductRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }

    // -------------------------------------------------------
    // Load / refresh seller's products
    // -------------------------------------------------------
    private void loadMyProducts() {
        productRows.clear();
        List<String[]> products = sellerModel.fetchMyProducts(username);
        for (String[] p : products) {
            // p = [productId, sellerUsername, name, category,
            //      originalPrice, discountedPrice, qty, expiryDate, status]
            productRows.add(new ProductRow(p[0], p[2], p[3], p[5], p[6], p[7], p[8]));
        }
    }

    // -------------------------------------------------------
    // Sidebar navigation
    // -------------------------------------------------------
    @FXML
    void showDashboard(ActionEvent event) {
        contentArea.getChildren().clear();
        Label lbl = new Label("Select 'Product Management' to manage your listings,\n"
                + "or 'Order Management' to view your sales.");
        lbl.setStyle("-fx-font-size: 16; -fx-text-fill: #7f8c8d;");
        contentArea.getChildren().add(lbl);
    }

    @FXML
    void showProducts(ActionEvent event) {
        contentArea.getChildren().clear();

        VBox view = new VBox(10);
        view.setStyle("-fx-padding: 20;");

        // Toolbar
        HBox toolbar = new HBox(10);
        Button addBtn    = new Button("➕ Add Product");
        Button editBtn   = new Button("✏️ Edit Selected");
        Button deleteBtn = new Button("🗑️ Delete Selected");

        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        editBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;");
        deleteBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");

        addBtn.setOnAction(e -> showAddProductDialog());
        editBtn.setOnAction(e -> showEditProductDialog());
        deleteBtn.setOnAction(e -> handleDeleteProduct());

        toolbar.getChildren().addAll(addBtn, editBtn, deleteBtn);
        view.getChildren().addAll(toolbar, mainTable);
        VBox.setVgrow(mainTable, Priority.ALWAYS);

        contentArea.getChildren().add(view);
        loadMyProducts();
    }

    @FXML
    void showOrders(ActionEvent event) {
        contentArea.getChildren().clear();

        List<String[]> sales = sellerModel.fetchMySales(username);

        TableView<String[]> table = new TableView<>();
        TableColumn<String[], String> cTxId     = new TableColumn<>("Transaction ID");
        TableColumn<String[], String> cProduct  = new TableColumn<>("Product ID");
        TableColumn<String[], String> cBuyer    = new TableColumn<>("Buyer");
        TableColumn<String[], String> cQty      = new TableColumn<>("Qty");
        TableColumn<String[], String> cDate     = new TableColumn<>("Date");

        cTxId   .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        cProduct.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        cBuyer  .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        cQty    .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));
        cDate   .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[4]));

        table.getColumns().addAll(cTxId, cProduct, cBuyer, cQty, cDate);
        table.setItems(FXCollections.observableArrayList(sales));

        VBox view = new VBox(10);
        view.setStyle("-fx-padding: 20;");
        view.getChildren().addAll(new Label("My Sales"), table);
        VBox.setVgrow(table, Priority.ALWAYS);

        contentArea.getChildren().add(view);
    }

    // -------------------------------------------------------
    // Add product dialog
    // -------------------------------------------------------
    private void showAddProductDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Add New Product");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField   nameField     = new TextField();
        TextField   categoryField = new TextField();
        TextField   origPrice     = new TextField();
        TextField   discPrice     = new TextField();
        TextField   qtyField      = new TextField();
        TextField   expiryField   = new TextField("YYYY-MM-DD");

        int row = 0;
        grid.add(new Label("Product Name:"),     0, row); grid.add(nameField,     1, row++);
        grid.add(new Label("Category:"),         0, row); grid.add(categoryField, 1, row++);
        grid.add(new Label("Original Price:"),   0, row); grid.add(origPrice,     1, row++);
        grid.add(new Label("Discounted Price:"), 0, row); grid.add(discPrice,     1, row++);
        grid.add(new Label("Quantity:"),         0, row); grid.add(qtyField,      1, row++);
        grid.add(new Label("Expiry Date:"),      0, row); grid.add(expiryField,   1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK);

        dialog.showAndWait().ifPresent(ok -> {
            if (!ok) return;
            try {
                String[] result = sellerModel.addProduct(
                        username,
                        nameField.getText().trim(),
                        categoryField.getText().trim(),
                        Double.parseDouble(origPrice.getText().trim()),
                        Double.parseDouble(discPrice.getText().trim()),
                        Integer.parseInt(qtyField.getText().trim()),
                        expiryField.getText().trim()
                );
                Alert.AlertType type = "SUCCESS".equals(result[0])
                        ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
                new Alert(type, result[1], ButtonType.OK).showAndWait();
                loadMyProducts();
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.WARNING,
                        "Please enter valid numbers for price and quantity.",
                        ButtonType.OK).showAndWait();
            }
        });
    }

    // -------------------------------------------------------
    // Edit product dialog (pre-fills fields with selected row)
    // -------------------------------------------------------
    private void showEditProductDialog() {
        ProductRow selected = mainTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select a product first.", ButtonType.OK).showAndWait();
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Product: " + selected.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField nameField     = new TextField(selected.getName());
        TextField categoryField = new TextField(selected.getCategory());
        TextField discPrice     = new TextField(selected.getPrice());
        TextField qtyField      = new TextField(selected.getQuantity());
        TextField expiryField   = new TextField(selected.getExpiryDate());
        ComboBox<String> statusBox = new ComboBox<>(
                FXCollections.observableArrayList("AVAILABLE", "UNAVAILABLE"));
        statusBox.setValue(selected.getStatus());

        int row = 0;
        grid.add(new Label("Name:"),             0, row); grid.add(nameField,     1, row++);
        grid.add(new Label("Category:"),         0, row); grid.add(categoryField, 1, row++);
        grid.add(new Label("Discounted Price:"), 0, row); grid.add(discPrice,     1, row++);
        grid.add(new Label("Quantity:"),         0, row); grid.add(qtyField,      1, row++);
        grid.add(new Label("Expiry Date:"),      0, row); grid.add(expiryField,   1, row++);
        grid.add(new Label("Status:"),           0, row); grid.add(statusBox,     1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK);

        dialog.showAndWait().ifPresent(ok -> {
            if (!ok) return;
            try {
                String[] result = sellerModel.updateProduct(
                        username,
                        selected.getProductId(),
                        nameField.getText().trim(),
                        categoryField.getText().trim(),
                        0,  // originalPrice unchanged (0 = skip in controller)
                        Double.parseDouble(discPrice.getText().trim()),
                        Integer.parseInt(qtyField.getText().trim()),
                        expiryField.getText().trim(),
                        statusBox.getValue()
                );
                Alert.AlertType type = "SUCCESS".equals(result[0])
                        ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
                new Alert(type, result[1], ButtonType.OK).showAndWait();
                loadMyProducts();
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.WARNING,
                        "Invalid numbers.", ButtonType.OK).showAndWait();
            }
        });
    }

    // -------------------------------------------------------
    // Delete selected product
    // -------------------------------------------------------
    private void handleDeleteProduct() {
        ProductRow selected = mainTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select a product first.", ButtonType.OK).showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete: " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                String[] result = sellerModel.deleteProduct(username, selected.getProductId());
                Alert.AlertType type = "SUCCESS".equals(result[0])
                        ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
                new Alert(type, result[1], ButtonType.OK).showAndWait();
                loadMyProducts();
            }
        });
    }

    // -------------------------------------------------------
    // Settings dropdown
    // -------------------------------------------------------
    @FXML
    void toggleSettings(MouseEvent event) {
        boolean v = settingsDropdown.isVisible();
        settingsDropdown.setVisible(!v);
        settingsDropdown.setManaged(!v);
    }

    private void closeDropdown() {
        settingsDropdown.setVisible(false);
        settingsDropdown.setManaged(false);
    }

    @FXML void handleUpdateProfile(ActionEvent event) {
        new Alert(Alert.AlertType.INFORMATION,
                "Profile update coming soon.", ButtonType.OK).showAndWait();
        closeDropdown();
    }

    @FXML void handleShowNotifications(ActionEvent event) { closeDropdown(); showOrders(null); }

    @FXML
    void handleChangePassword(ActionEvent event) {
        closeDropdown();
        // Reuse same dialog logic as BuyerController
        new Alert(Alert.AlertType.INFORMATION,
                "Use the Buyer dashboard to change your password.", ButtonType.OK).showAndWait();
    }

    @FXML
    void switchToBuyer(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Client/VIEW/Buyer_Dashboard.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Marketplace");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    // -------------------------------------------------------
    // Inner data class for product TableView
    // -------------------------------------------------------
    public static class ProductRow {
        private final String productId;
        private final String name;
        private final String category;
        private final String price;
        private final String quantity;
        private final String expiryDate;
        private final String status;

        public ProductRow(String productId, String name, String category,
                          String price, String quantity, String expiryDate, String status) {
            this.productId  = productId;
            this.name       = name;
            this.category   = category;
            this.price      = price;
            this.quantity   = quantity;
            this.expiryDate = expiryDate;
            this.status     = status;
        }

        public String getProductId()  { return productId;  }
        public String getName()       { return name;       }
        public String getCategory()   { return category;   }
        public String getPrice()      { return price;      }
        public String getQuantity()   { return quantity;   }
        public String getExpiryDate() { return expiryDate; }
        public String getStatus()     { return status;     }
    }
}