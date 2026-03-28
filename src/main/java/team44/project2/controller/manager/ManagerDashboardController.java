package team44.project2.controller.manager;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Controller for the main manager dashboard screen, providing navigation buttons to
 * employee management, menu/pricing, reports, and inventory management sub-screens.
 */
@Dependent
public class ManagerDashboardController {

    @FXML
    private Button managerManageEmployeesButton;

    @FXML
    private Button managerMenuPriceButton;

    @FXML
    private Button managerReportsButton;

    @FXML
    private Button managerInventoryButton;

    @FXML
    private Button managerDashboardLogoutButton;

    @Inject
    Instance<Object> cdiInstance;

    /**
     * Wires each navigation button to its corresponding FXML screen path.
     */
    @FXML
    private void initialize() {
        managerManageEmployeesButton.setOnAction(e -> loadScreen("/fxml/manager/employees/EmployeeList.fxml"));
        managerMenuPriceButton.setOnAction(e -> loadScreen("/fxml/manager/menu/MenuView.fxml"));
        managerReportsButton.setOnAction(e -> loadScreen("/fxml/manager/reports/OrderingTrends.fxml"));
        managerInventoryButton.setOnAction(e -> loadScreen("/fxml/manager/inventory/TakeInventory.fxml"));
    }

    /**
     * Handles the Log Out button action. Prompts the manager for confirmation before
     * navigating back to the login screen.
     */
    @FXML
    private void handleLogOut() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Log Out");
        alert.setHeaderText("Confirm Log Out");
        alert.setContentText("Are you sure you want to log out?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        loadScreen("/fxml/auth/Login.fxml");
    }

    /**
     * Loads a new screen by replacing the current stage's scene with the FXML at the
     * given path. Resets the stage title to "Login" when navigating to the login screen.
     *
     * @param fxmlPath The classpath-relative FXML resource path to load.
     */
    private void loadScreen(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());

            Stage stage = (Stage) managerManageEmployeesButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            if (fxmlPath.contains("Login")) {
                stage.setTitle("Login");
            }

        } catch (Exception e) {
            io.quarkus.logging.Log.error("Failed to load screen: " + fxmlPath, e);
        }
    }
}