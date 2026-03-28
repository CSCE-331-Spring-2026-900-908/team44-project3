package team44.project2.controller.manager.inventory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import team44.project2.model.Inventory;
import team44.project2.service.inventory.InventoryService;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the current-stock screen in the manager dashboard, displaying the
 * real-time inventory levels of all items in a read-only table view.
 */
@Dependent
public class CurrentStockController implements Initializable {

    @FXML
    private TableView<Inventory> inventoryTable;

    @FXML
    private TableColumn<Inventory, String> nameColumn;

    @FXML
    private TableColumn<Inventory, String> categoryColumn;

    @FXML
    private TableColumn<Inventory, ?> quantityColumn;

    @FXML
    private TableColumn<Inventory, String> unitColumn;

    @Inject
    InventoryService inventoryService;

    /**
     * Configures table column value factories and populates the table with all current
     * inventory items retrieved from the service.
     *
     * @param location  Unused; may be {@code null}.
     * @param resources Unused; may be {@code null}.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("currentQuantity"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        inventoryTable.getItems().addAll(
                inventoryService.getAllInventory()
        );
    }
}