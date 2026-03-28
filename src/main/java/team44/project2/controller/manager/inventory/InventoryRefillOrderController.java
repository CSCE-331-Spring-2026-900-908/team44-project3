package team44.project2.controller.manager.inventory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import team44.project2.model.Inventory;
import team44.project2.model.RestockOrder;
import team44.project2.service.inventory.InventoryService;
import team44.project2.service.inventory.RestockOrderService;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * Controller for the inventory refill-order screen in the manager dashboard, allowing
 * managers to view items whose stock is below the reorder threshold and submit restock
 * orders for individual items.
 */
@Dependent
public class InventoryRefillOrderController implements Initializable {

    @FXML
    private TableView<Inventory> lowStockTable;

    @FXML
    private TableColumn<Inventory, String> itemColumn;

    @FXML
    private TableColumn<Inventory, String> categoryColumn;

    @FXML
    private TableColumn<Inventory, BigDecimal> quantityColumn;

    @FXML
    private TableColumn<Inventory, String> unitColumn;

    @FXML
    private TextField reorderQuantityField;

    @FXML
    private Button submitOrderButton;

    @Inject
    private InventoryService inventoryService;

    @Inject
    private RestockOrderService restockOrderService;

    /**
     * Configures table column value factories, loads all items with stock below the
     * reorder threshold, and sets the table selection mode to single-select.
     *
     * @param location  Unused; may be {@code null}.
     * @param resources Unused; may be {@code null}.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        
        itemColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("currentQuantity"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        
        lowStockTable.getItems().addAll(
                inventoryService.getAllInventory().stream()
                        .filter(item -> item.currentQuantity().compareTo(item.reorderThreshold()) < 0)
                        .toList()
        );

        
        lowStockTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    /**
     * Handles the Submit Order button action. Validates that an item and a numeric
     * quantity are provided, then delegates to the restock-order service to create
     * the order and displays a confirmation or error alert.
     */
    @FXML
    private void handleSubmitOrder() {

        Inventory selectedItem = lowStockTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "No item selected", "Please select an item to reorder.");
            return;
        }

        String quantityText = reorderQuantityField.getText();
        if (quantityText == null || quantityText.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Invalid quantity", "Please enter a quantity to reorder.");
            return;
        }

        try {
            BigDecimal quantity = new BigDecimal(quantityText);

            
            RestockOrder order = restockOrderService.createRestockOrder(
                    1,
                    selectedItem.inventoryId(),
                    quantity,
                    LocalDateTime.now(),
                    "PENDING"
            );

            if (order != null) {
                showAlert(Alert.AlertType.INFORMATION, "Order submitted",
                        "Restock order created for " + selectedItem.itemName() + " (" + quantity + " " + selectedItem.unit() + ")");
                reorderQuantityField.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Order failed", "Failed to create restock order. Check logs.");
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid quantity", "Please enter a valid numeric quantity.");
        }
    }

    /**
     * Displays a JavaFX alert dialog with the specified type, title, and message.
     *
     * @param type    The alert severity type.
     * @param title   The dialog window title.
     * @param message The descriptive message shown in the alert body.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
