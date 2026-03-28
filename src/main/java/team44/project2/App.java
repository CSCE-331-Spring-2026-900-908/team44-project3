package team44.project2;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

import jakarta.inject.Inject;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.fx.FxPostStartupEvent;

/**
 * Main application class that initializes the JavaFX application and sets up the
 * primary stage with the login screen.
 * <p>
 * It listens for the {@code FxPostStartupEvent} to load the initial FXML layout for
 * the login screen and displays it to the user when the application starts. This class
 * serves as the entry point for the JavaFX application and is responsible for launching
 * the user interface.
 */
@ApplicationScoped
public class App {

    @Inject
    FXMLLoader fxmlLoader;

    /**
     * Handles the JavaFX post-startup event by loading the login screen FXML and
     * displaying it on the primary stage.
     *
     * @param event The post-startup event that carries a reference to the primary {@code Stage}.
     * @throws Exception If the FXML resource cannot be found or loaded.
     */
    void onStart(@Observes FxPostStartupEvent event) throws Exception {
        Stage stage = event.getPrimaryStage();
        Parent root = fxmlLoader.load(getClass().getResourceAsStream("/fxml/auth/Login.fxml"));
        stage.setTitle("Team 44 Project 2");
        stage.setScene(new Scene(root));
        stage.show();
    }
}