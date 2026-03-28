package team44.project2.controller.manager.employees;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import team44.project2.model.Employee;
import team44.project2.service.employee.EmployeeService;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the employee list screen in the manager dashboard, displaying all
 * employees in a table and allowing managers to add, edit, and delete employee records.
 */
@Dependent
public class EmployeeListController implements Initializable {

    @FXML
    private TableView<Employee> employeeTable;

    @FXML
    private TableColumn<Employee, String> firstNameColumn;

    @FXML
    private TableColumn<Employee, String> lastNameColumn;

    @FXML
    private TableColumn<Employee, String> roleColumn;

    @FXML
    private TableColumn<Employee, String> emailColumn;

    @FXML
    private TableColumn<Employee, Boolean> activeColumn;

    @Inject
    EmployeeService employeeService;

    @Inject
    Instance<Object> cdiInstance;

     private final ObservableList<Employee> employeeList =
             FXCollections.observableArrayList();

    /**
     * Configures table column value factories, loads the employee list, and registers
     * a double-click listener to open the edit form.
     *
     * @param location  Unused; may be {@code null}.
     * @param resources Unused; may be {@code null}.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        

        firstNameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().firstName()));
        lastNameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().lastName()));
        roleColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().role()));
        emailColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().email()));
        activeColumn.setCellValueFactory(cellData -> new ReadOnlyBooleanWrapper(cellData.getValue().isActive()).asObject());

        
        loadEmployees();

        
        employeeTable.setOnMouseClicked(this::handleTableClick);

        
        
    }

    /**
     * Fetches all employees from the service and refreshes the table view.
     */
    private void loadEmployees() {
         employeeList.setAll(employeeService.getAllEmployees());
         employeeTable.setItems(employeeList);
     }

    /**
     * Handles double-click events on the table to open the edit form for the selected
     * employee and refresh the list after the form closes.
     *
     * @param event The mouse event that triggered this handler.
     */
    private void handleTableClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Employee selected = employeeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                loadEmployeeForm(selected);
            }
            employeeList.setAll(employeeService.getAllEmployees());
         employeeTable.setItems(employeeList);
        }
    }

    /**
     * Opens the employee form in a new stage pre-populated with the given employee's
     * data for editing.
     *
     * @param employee The employee whose data should be loaded into the form.
     */
    private void loadEmployeeForm(Employee employee) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/manager/employees/EmployeeForm.fxml"));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());

            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Edit Employee");

            EmployeeFormController controller = loader.getController();
            controller.setEmployee(employee);

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the Add Employee button action by opening a blank employee form.
     */
    @FXML
    private void handleAddEmployee() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/manager/employees/EmployeeForm.fxml"));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());

            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Add Employee");

            stage.show();
            
            loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the Edit button action by opening the employee form for the currently
     * selected table row.
     */
    @FXML
    private void handleEditEmployee() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            loadEmployeeForm(selected);
        }
    }

    /**
     * Handles the Delete button action by removing the selected employee from the
     * database and refreshing the list.
     */
    @FXML
    private void handleDeleteEmployee() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            employeeService.deleteEmployee(selected.employeeId());
            loadEmployees();
        }
    }

    /**
     * Handles the Back button action by navigating back to the manager dashboard.
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/manager/ManagerDashboard.fxml"));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());
            
            Stage stage = (Stage) employeeTable.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}