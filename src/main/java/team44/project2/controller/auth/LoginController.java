package team44.project2.controller.auth;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import team44.project2.controller.EmployeeAware;
import team44.project2.model.Employee;
import team44.project2.service.AuthService;

import static team44.project2.util.StringUtils.toTitleCase;

/**
 * Controller for the login screen that authenticates users and navigates them to the
 * appropriate dashboard screen based on their assigned role.
 * <p>
 * Delegates credential validation to {@link team44.project2.service.AuthService} and
 * uses the returned {@link team44.project2.model.Employee} to determine which FXML
 * screen to load next.
 */
@Dependent
public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @Inject
    AuthService authService;

    @Inject
    Instance<Object> cdiInstance;

    /**
     * Handles the login button action. Validates that the email and password fields are
     * non-empty, delegates to {@link team44.project2.service.AuthService#authenticate}
     * for credential verification, and navigates to the appropriate dashboard on success.
     */
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty()) {
            showError("Please enter your email");
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter your password");
            return;
        }

        Employee employee = authService.authenticate(email, password);

        if (employee == null) {
            showError("Invalid email or password");
            return;
        }

        navigateByRole(employee);
    }

    /**
     * Navigates to the correct dashboard FXML screen based on the authenticated employee's
     * role. Managers are sent to the manager dashboard; all other roles go to the cashier
     * ordering screen. Also injects the employee ID into any controller that implements
     * {@link team44.project2.controller.EmployeeAware}.
     *
     * @param employee The authenticated {@code Employee} whose role determines navigation.
     */
    private void navigateByRole(Employee employee) {
        String name = toTitleCase(employee.firstName()) + " " + toTitleCase(employee.lastName());
        String role = toTitleCase(employee.role());
        
        // Determine FXML path based on role
        String fxml = "manager".equals(employee.role())
                ? "/fxml/manager/ManagerDashboard.fxml"
                : "/fxml/cashier/ordering/OrderingScreen.fxml";

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(name + " (" + role + ")");

            if (loader.getController() instanceof EmployeeAware ea) {
                ea.setEmployeeId(employee.employeeId());
            }
        } catch (Exception e) {
            showError("Failed to load screen");
            Log.error("Navigation failed", e);
        }
    }

    /**
     * Displays an error message in the error label on the login form.
     *
     * @param message The error text to display to the user.
     */
    private void showError(String message) {
        errorLabel.setText(message);
    }
}