import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Main — JavaFX entry point for the client application.
 *
 * Launches the Login screen. After login, the LogInController
 * switches to the appropriate dashboard (Buyer / Seller / Admin).
 *
 * Run this class to start the CLIENT.
 * Run Server.Server to start the SERVER (do this first).
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(
                Objects.requireNonNull(getClass().getResource("/Client/VIEW/LoginForm.fxml")));

        primaryStage.setTitle("Food Waste Reducer — SDG 12");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}