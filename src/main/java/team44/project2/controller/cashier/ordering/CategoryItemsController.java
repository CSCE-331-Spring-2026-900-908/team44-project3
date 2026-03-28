package team44.project2.controller.cashier.ordering;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import team44.project2.model.menu.MenuItem;
import team44.project2.service.MenuService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for displaying menu items in a selected category and allowing the cashier
 * to select and customize them before adding to the current order.
 */
@Dependent
public class CategoryItemsController implements Initializable {

    @FXML
    private Label categoryLabel;

    @FXML
    private ListView<MenuItem> itemsListView;

    @Inject
    MenuService menuService;

    @Inject
    Instance<Object> cdiInstance;

    private String category;
    private OrderingScreenController orderingScreenController;

    /**
     * Sets the menu category whose items this controller will display.
     *
     * @param category The category name (e.g., "Milk Tea").
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Provides a reference to the parent {@link OrderingScreenController} so that
     * confirmed cart items can be passed back.
     *
     * @param controller The active ordering-screen controller.
     */
    public void setOrderingScreenController(OrderingScreenController controller) {
        this.orderingScreenController = controller;
    }

    /**
     * Initializes the category label, populates the list view with available menu items
     * for the selected category, and registers a double-click handler to open the
     * item-customization screen.
     *
     * @param location  The location used to resolve relative paths (may be {@code null}).
     * @param resources The resource bundle for localisation (may be {@code null}).
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        categoryLabel.setText(category);

        itemsListView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name() + " - $" + item.basePrice());
            }
        });

        List<MenuItem> items = menuService.getItemsByCategory(category);
        itemsListView.getItems().addAll(items);

        itemsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                MenuItem selected = itemsListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openItemCustomization(selected);
                }
            }
        });
    }

    /**
     * Opens the item-customization screen for the given menu item in the same stage.
     *
     * @param item The {@link team44.project2.model.menu.MenuItem} the cashier selected.
     */
    private void openItemCustomization(MenuItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cashier/ordering/ItemCustomization.fxml"));
            loader.setControllerFactory(clazz -> {
                if (clazz == ItemCustomizationController.class) {
                    ItemCustomizationController controller = cdiInstance.select(ItemCustomizationController.class).get();
                    controller.setMenuItem(item);
                    controller.setOrderingScreenController(orderingScreenController);
                    return controller;
                }
                return cdiInstance.get();
            });

            Stage stage = (Stage) itemsListView.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            io.quarkus.logging.Log.error("Failed to open item customization", e);
        }
    }

    /**
     * Handles the Back button action by closing this category-items window and returning
     * to the calling screen.
     */
    @FXML
    private void handleBack() {
        Stage stage = (Stage) categoryLabel.getScene().getWindow();
        stage.close();
    }
}