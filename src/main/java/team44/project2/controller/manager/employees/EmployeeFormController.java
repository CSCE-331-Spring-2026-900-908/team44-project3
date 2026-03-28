package team44.project2.controller.manager.employees;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import team44.project2.model.Employee;
import team44.project2.service.employee.EmployeeService;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Controller for the employee form used by managers to create new employees or edit
 * existing employee records including name, role, start date, email, password, and
 * active status.
 */
@Dependent
public class EmployeeFormController implements Initializable {

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField roleField;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox activeCheckBox;

    @Inject
    EmployeeService employeeService;

    private Employee currentEmployee;

    /**
     * JavaFX lifecycle hook called after FXML injection — currently a no-op but kept for
     * future initialisation needs.
     *
     * @param location  Unused; may be {@code null}.
     * @param resources Unused; may be {@code null}.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
    }

    /**
     * Pre-populates the form fields with the data of an existing employee for editing.
     * When {@code employee} is {@code null} the form is treated as an add-new form.
     *
     * @param employee The employee to edit, or {@code null} to create a new one.
     */
    public void setEmployee(Employee employee) {
        this.currentEmployee = employee;
        if (employee != null) {
            firstNameField.setText(employee.firstName());
            lastNameField.setText(employee.lastName());
            roleField.setText(employee.role());
            startDatePicker.setValue(employee.startDate());
            emailField.setText(employee.email());
            passwordField.setText(employee.passwordHash()); 
            activeCheckBox.setSelected(employee.isActive());
        }
    }

    /**
     * Handles the Save button action. Creates a new employee record when no current
     * employee is set, or updates the existing record otherwise. Closes the form window
     * after a successful save.
     */
    @FXML
    private void handleSave() {
        if (currentEmployee == null) {
            
            Employee employee = new Employee(
                    0, 
                    firstNameField.getText(),
                    lastNameField.getText(),
                    roleField.getText(),
                    startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now(),
                    emailField.getText(),
                    passwordField.getText(), 
                    activeCheckBox.isSelected()
            );
            employeeService.addEmployee(employee);
        } else {
            
            Employee updated = new Employee(
                    currentEmployee.employeeId(),
                    firstNameField.getText(),
                    lastNameField.getText(),
                    roleField.getText(),
                    startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now(),
                    emailField.getText(),
                    passwordField.getText(), 
                    activeCheckBox.isSelected()
            );
            employeeService.updateEmployee(updated);

        }
        
        
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
    }
}