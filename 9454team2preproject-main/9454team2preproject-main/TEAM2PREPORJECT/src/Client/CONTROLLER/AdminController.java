package Client.CONTROLLER;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AdminController {

    @FXML
    private StackPane adminContentArea;

    @FXML
    private TextField adminSearchField;

    @FXML
    private TableColumn<?, ?> colEmail;

    @FXML
    private TableColumn<?, ?> colId;

    @FXML
    private TableColumn<?, ?> colRole;

    @FXML
    private TableColumn<?, ?> colUsername;

    @FXML
    private ComboBox<?> roleFilter;

    @FXML
    private VBox serverControlView;

    @FXML
    private Label serverStatusLabel;

    @FXML
    private VBox userMgmtView;

    @FXML
    private TableView<?> userTable;

    @FXML
    private Label viewTitle;

    @FXML
    void handleDeleteAccount(ActionEvent event) {

    }

    @FXML
    void handleResetPassword(ActionEvent event) {

    }

    @FXML
    void handleRestartServer(ActionEvent event) {

    }

    @FXML
    void showSellerRequests(ActionEvent event) {

    }

    @FXML
    void showServerControl(ActionEvent event) {

    }

    @FXML
    void showUserManagement(ActionEvent event) {

    }

}
