package team44.project2.controller;

/**
 * Interface for JavaFX controllers that need to receive the currently logged-in
 * employee's ID after screen navigation.
 * <p>
 * Controllers implementing this interface are automatically detected by the navigation
 * helpers, which call {@link #setEmployeeId(int)} immediately after the FXML is loaded.
 */
public interface EmployeeAware {

    /**
     * Receives the ID of the employee who is currently authenticated.
     *
     * @param employeeId The database primary key of the authenticated employee.
     */
    void setEmployeeId(int employeeId);
}