package team44.project2.controller.manager.inventory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import team44.project2.model.Inventory;
import team44.project2.service.inventory.InventoryService;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

/**
 * Controller for the take-inventory screen in the manager dashboard, allowing managers
 * to update stock quantities for existing inventory items, add entirely new items via a
 * dialog, and delete items they no longer carry.
 */
@Dependent
public class TakeInventoryController implements Initializable {

    @FXML
    private ComboBox<Inventory> itembox;

    @FXML
    private TextField quantityField;

    @FXML
    private TableView<Inventory> inventoryTable;

    @FXML
    private TableColumn<Inventory, String> itemNameColumn;

    @FXML
    private TableColumn<Inventory, String> categoryColumn;

    @FXML
    private TableColumn<Inventory, BigDecimal> currentQuantityColumn;

    @FXML
    private TableColumn<Inventory, String> unitColumn;

    @FXML
    private TableColumn<Inventory, BigDecimal> reorderThresholdColumn;

    @Inject
    InventoryService inventoryService;

    @Inject
    Instance<Object> cdiInstance;

    /**
     * Configures table column value factories, loads all inventory items into the table
     * and the combo box, and sets a custom cell factory to display item names in the
     * combo box.
     *
     * @param location  Unused; may be {@code null}.
     * @param resources Unused; may be {@code null}.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        itemNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().itemName()));
        categoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        currentQuantityColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().currentQuantity()));
        unitColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().unit()));
        reorderThresholdColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().reorderThreshold()));

        loadInventoryTable();

        itembox.getItems().addAll(
                inventoryService.getAllInventory()
        );

        itembox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Inventory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.itemName());
            }
        });

        itembox.setButtonCell(itembox.getCellFactory().call(null));
    }

    /**
     * Fetches all inventory items and replaces the table content with the latest data.
     */
    private void loadInventoryTable() {
        inventoryTable.getItems().setAll(inventoryService.getAllInventory());
    }

    /**
     * Handles the Update Stock button action. Sets the quantity of the combo-box-selected
     * item to the value entered in the quantity field and refreshes the display.
     */
    @FXML
    private void handleAddItem() {

        Inventory selectedItem = itembox.getValue();
        String quantityText = quantityField.getText();

        if (selectedItem == null || quantityText == null || quantityText.isBlank()) {
            return;
        }

        try {
            BigDecimal newQuantity = new BigDecimal(quantityText);

            inventoryService.updateStock(
                    selectedItem.inventoryId(),
                    newQuantity
            );

            loadInventoryTable();
            itembox.setValue(null);
            quantityField.clear();

        } catch (NumberFormatException e) {
            
        }
    }

    /**
     * Handles the Add Inventory button action. Shows a dialog that collects name,
     * category, unit, quantity, reorder threshold, and supplier cost for a new item,
     * then persists it and refreshes the display.
     */
    @FXML
    private void handleAddInventory() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Inventory Item");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        TextField categoryField = new TextField();
        TextField unitField = new TextField();
        TextField qtyField = new TextField();
        TextField reorderField = new TextField();
        TextField costField = new TextField();

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryField, 1, 1);
        grid.add(new Label("Unit:"), 0, 2);
        grid.add(unitField, 1, 2);
        grid.add(new Label("Quantity:"), 0, 3);
        grid.add(qtyField, 1, 3);
        grid.add(new Label("Reorder Threshold:"), 0, 4);
        grid.add(reorderField, 1, 4);
        grid.add(new Label("Supplier Cost:"), 0, 5);
        grid.add(costField, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String name = nameField.getText();
                String category = categoryField.getText();
                String unit = unitField.getText();
                BigDecimal qty = qtyField.getText() == null || qtyField.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(qtyField.getText());
                BigDecimal reorder = reorderField.getText() == null || reorderField.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(reorderField.getText());
                BigDecimal cost = costField.getText() == null || costField.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(costField.getText());

                Inventory newItem = new Inventory(0, name, category, qty, unit, reorder, cost, null);
                inventoryService.addInventoryItem(newItem);

                loadInventoryTable();
                itembox.getItems().setAll(inventoryService.getAllInventory());
            } catch (NumberFormatException ex) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Please enter valid numbers for quantity, reorder threshold, and cost.");
                a.showAndWait();
            }
        }
    }

    /**
     * Handles the Delete Inventory button action. Prompts for confirmation before
     * permanently removing the selected item from the database and refreshing the
     * display.
     */
    @FXML
    private void handleDeleteInventory() {
        Inventory selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Please select an inventory item to delete.");
            a.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete '" + selected.itemName() + "'?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            inventoryService.deleteInventoryItem(selected.inventoryId());
            loadInventoryTable();
            itembox.getItems().setAll(inventoryService.getAllInventory());
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
            
            Stage stage = (Stage) itembox.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}