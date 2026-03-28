package team44.project2.controller.manager.menu;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import team44.project2.model.menu.MenuItem;
import team44.project2.service.MenuService;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the menu-view screen in the manager dashboard, displaying all available
 * menu items in a table and allowing managers to add, edit, and delete items.
 */
@Dependent
public class MenuViewController implements Initializable {

    @FXML
    private TableView<MenuItem> menuTable;

    @FXML
    private TableColumn<MenuItem, String> nameColumn;

    @FXML
    private TableColumn<MenuItem, String> categoryColumn;

    @FXML
    private TableColumn<MenuItem, String> sizeColumn;

    @FXML
    private TableColumn<MenuItem, BigDecimal> priceColumn;

    @FXML
    private TableColumn<MenuItem, Boolean> availableColumn;

    @Inject
    private MenuService menuService;

    @Inject
    Instance<Object> cdiInstance;

    /**
     * Configures table column value factories and loads all available menu items.
     *
     * @param location  Unused; may be {@code null}.
     * @param resources Unused; may be {@code null}.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name()));
        categoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        sizeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().size()));
        priceColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().basePrice()));
        availableColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().isAvailable()));

        loadMenuItems();
    }

    /**
     * Fetches all menu items from the service and repopulates the table.
     */
    private void loadMenuItems() {
        menuTable.getItems().setAll(menuService.getAllMenuItems());
    }

    /**
     * Handles the Add Item button action by opening the menu-item form with no
     * pre-populated data.
     */
    @FXML
    private void handleAddItem() {
        openMenuItemForm(null);
    }

    /**
     * Handles the Edit Item button action by opening the menu-item form pre-populated
     * with the selected item's data, or showing an alert if nothing is selected.
     */
    @FXML
    private void handleEditItem() {
        MenuItem selected = menuTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openMenuItemForm(selected);
        } else {
            showAlert("No item selected", "Please select a menu item to edit.");
        }
    }

    /**
     * Opens the menu-item form in a modal stage, optionally pre-populated with the
     * given item. Reloads the menu list after the form is closed.
     *
     * @param item The item to edit, or {@code null} to open a blank add-new form.
     */
    private void openMenuItemForm(MenuItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/manager/menu/MenuItemForm.fxml"));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());
            Stage stage = new Stage();
            stage.setTitle(item == null ? "Add Menu Item" : "Edit Menu Item");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(loader.load()));

            MenuItemFormController controller = loader.getController();
            if (item != null) {
                controller.setEditingItem(item);
            }

            stage.showAndWait();
            
            loadMenuItems();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open the menu item form.");
        }
    }

    /**
     * Handles the Delete Item button action. Prompts for confirmation then removes the
     * selected item from both the database and the table view.
     */
    @FXML
    private void handleDeleteItem() {
        MenuItem selected = menuTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Delete");
            confirmAlert.setHeaderText("Delete Menu Item");
            confirmAlert.setContentText("Are you sure you want to delete \"" + selected.name() + "\"?");
            
            if (confirmAlert.showAndWait().map(response -> response.getButtonData().isCancelButton()).orElse(true)) {
                return;
            }
            
            menuService.deleteMenuItem(selected.menuItemId());
            menuTable.getItems().remove(selected);
        } else {
            showAlert("No item selected", "Please select a menu item to delete.");
        }
    }

    /**
     * Displays a warning alert dialog with the given title and message.
     *
     * @param title   The dialog title.
     * @param message The warning message body.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Handles the Back button action by navigating back to the manager dashboard.
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/manager/ManagerDashboard.fxml"));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());
            
            Stage stage = (Stage) menuTable.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}