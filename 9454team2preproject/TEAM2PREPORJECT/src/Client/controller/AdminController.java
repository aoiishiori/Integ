package Client.controller;

import Client.Admin.Model.AdminModel;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * AdminController — MVC Controller for Admin_Dashboard.fxml
 */
public class AdminController implements Initializable {

    // --- FXML fields ---
    @FXML private StackPane adminContentArea;
    @FXML private TextField adminSearchField;
    @FXML private TableView<UserRow> userTable;
    @FXML private TableColumn<UserRow, String> colId;
    @FXML private TableColumn<UserRow, String> colUsername;
    @FXML private TableColumn<UserRow, String> colRole;
    @FXML private TableColumn<UserRow, String> colStatus;   // renamed from colEmail for accuracy
    @FXML private ComboBox<String> roleFilter;
    @FXML private VBox serverControlView;
    @FXML private VBox userMgmtView;
    @FXML private Label serverStatusLabel;
    @FXML private Label viewTitle;

    private final AdminModel adminModel = new AdminModel();
    private final String adminUsername  = SessionData.getUsername();
    private ObservableList<UserRow> allUsers = FXCollections.observableArrayList();

    // -------------------------------------------------------
    // Initialize — runs when FXML is loaded
    // -------------------------------------------------------
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Bind table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("accountId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Role filter options
        roleFilter.setItems(FXCollections.observableArrayList(
                "All", "ADMIN", "BUYER", "SELLER"));
        roleFilter.setValue("All");
        roleFilter.setOnAction(e -> applyFilter());

        // Search field live filter
        adminSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        // Load users on startup
        loadUsers();

        // Show user management view by default
        showUserManagement(null);
    }

    // -------------------------------------------------------
    // Load / refresh user table
    // -------------------------------------------------------
    private void loadUsers() {
        allUsers.clear();
        List<String[]> users = adminModel.fetchAllUsers(adminUsername);
        for (String[] u : users) {
            // u = [accountId, username, role, status]
            allUsers.add(new UserRow(u[0], u[1], u[2], u[3]));
        }
        userTable.setItems(allUsers);
    }

    private void applyFilter() {
        String keyword = adminSearchField.getText().toLowerCase();
        String role    = roleFilter.getValue();

        ObservableList<UserRow> filtered = FXCollections.observableArrayList();
        for (UserRow row : allUsers) {
            boolean matchesKeyword = row.getUsername().toLowerCase().contains(keyword)
                    || row.getAccountId().toLowerCase().contains(keyword);
            boolean matchesRole = "All".equals(role) || role.equals(row.getRole());
            if (matchesKeyword && matchesRole) {
                filtered.add(row);
            }
        }
        userTable.setItems(filtered);
    }

    // -------------------------------------------------------
    // Sidebar navigation
    // -------------------------------------------------------
    @FXML
    void showUserManagement(ActionEvent event) {
        viewTitle.setText("User Management");
        userMgmtView.setVisible(true);
        userMgmtView.setManaged(true);
        serverControlView.setVisible(false);
        serverControlView.setManaged(false);
        loadUsers();
    }

    @FXML
    void showSellerRequests(ActionEvent event) {
        viewTitle.setText("Seller Requests");
        roleFilter.setValue("SELLER");
        applyFilter();
        userMgmtView.setVisible(true);
        userMgmtView.setManaged(true);
        serverControlView.setVisible(false);
        serverControlView.setManaged(false);
    }

    @FXML
    void showServerControl(ActionEvent event) {
        viewTitle.setText("Server Control");
        userMgmtView.setVisible(false);
        userMgmtView.setManaged(false);
        serverControlView.setVisible(true);
        serverControlView.setManaged(true);
    }

    // -------------------------------------------------------
    // Delete selected user
    // -------------------------------------------------------
    @FXML
    void handleDeleteAccount(ActionEvent event) {
        UserRow selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a user to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete user: " + selected.getUsername() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                boolean ok = adminModel.deleteUser(adminUsername, selected.getUsername());
                showAlert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                        ok ? "User deleted." : "Failed to delete user.");
                if (ok) loadUsers();
            }
        });
    }

    // -------------------------------------------------------
    // Approve / Deny selected seller (Reset Password repurposed to Approve/Deny)
    // -------------------------------------------------------
    @FXML
    void handleResetPassword(ActionEvent event) {
        UserRow selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a user first.");
            return;
        }

        // Toggle between APPROVED and DENIED
        String newStatus = "APPROVED".equals(selected.getStatus()) ? "DENIED" : "APPROVED";
        boolean ok = adminModel.updateUserStatus(adminUsername, selected.getUsername(), newStatus);
        showAlert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                ok ? "Status updated to " + newStatus + "." : "Failed to update status.");
        if (ok) loadUsers();
    }

    // -------------------------------------------------------
    // Restart server (stop/start command — console-side)
    // -------------------------------------------------------
    @FXML
    void handleRestartServer(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION,
                "To restart: go to the server console and type 'stop', "
                        + "then relaunch Server.java.");
    }

    // -------------------------------------------------------
    // View logs in a popup
    // -------------------------------------------------------
    public void handleViewLogs() {
        String logs = adminModel.fetchLogs(adminUsername);
        TextArea logArea = new TextArea(logs);
        logArea.setEditable(false);
        logArea.setPrefSize(700, 400);

        Stage logStage = new Stage();
        logStage.setTitle("Server Activity Log");
        logStage.setScene(new Scene(logArea));
        logStage.show();
    }

    // -------------------------------------------------------
    // Logout
    // -------------------------------------------------------
    public void handleLogout() {
        try {
            SessionData.clear();
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Client/VIEW/LoginForm.fxml"));
            Stage stage = (Stage) viewTitle.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Logout error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------
    private void showAlert(Alert.AlertType type, String msg) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }

    // -------------------------------------------------------
    // Inner data class for TableView
    // -------------------------------------------------------
    public static class UserRow {
        private final String accountId;
        private final String username;
        private final String role;
        private final String status;

        public UserRow(String accountId, String username, String role, String status) {
            this.accountId = accountId;
            this.username  = username;
            this.role      = role;
            this.status    = status;
        }

        public String getAccountId() { return accountId; }
        public String getUsername()  { return username;  }
        public String getRole()      { return role;      }
        public String getStatus()    { return status;    }
    }
}