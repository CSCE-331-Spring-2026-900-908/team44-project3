package team44.project2.controller.cashier.ordering;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import team44.project2.model.menu.enums.IceLevel;
import team44.project2.model.menu.MenuItem;
import team44.project2.model.menu.enums.SweetnessLevel;
import team44.project2.model.order.CartItem;
import team44.project2.service.MenuService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the item-customization screen where cashiers select size, sweetness
 * level, ice level, and add-ons for a specific menu item before adding it to the cart.
 */
@Dependent
public class ItemCustomizationController implements Initializable {

    @FXML
    private Label itemNameLabel;

    @FXML
    private FlowPane sizePane;

    @FXML
    private FlowPane sweetnessPane;

    @FXML
    private FlowPane iceLevelPane;

    @FXML
    private FlowPane addOnsPane;

    @Inject
    MenuService menuService;

    private MenuItem menuItem;
    private OrderingScreenController orderingScreenController;

    private String selectedSize;
    private String selectedSweetness;
    private String selectedIceLevel;
    private final List<MenuItem> selectedAddOns = new ArrayList<>();

    /**
     * Sets the menu item whose customization options will be displayed.
     *
     * @param item The {@link team44.project2.model.menu.MenuItem} to customise.
     */
    public void setMenuItem(MenuItem item) {
        this.menuItem = item;
    }

    /**
     * Provides a reference to the parent {@link OrderingScreenController} so that the
     * completed {@link team44.project2.model.order.CartItem} can be passed back after
     * the cashier confirms.
     *
     * @param controller The active ordering-screen controller.
     */
    public void setOrderingScreenController(OrderingScreenController controller) {
        this.orderingScreenController = controller;
    }

    /**
     * Initialises the customization screen with size, sweetness, ice-level, and
     * add-on options derived from the selected menu item.
     *
     * @param location  The location used to resolve relative paths (may be {@code null}).
     * @param resources The resource bundle for localisation (may be {@code null}).
     */
    @Override
    // Initializes the customization screen with options based on the selected menu item
    public void initialize(URL location, ResourceBundle resources) {
        itemNameLabel.setText(menuItem.name());
        loadSizes();
        loadSweetnessLevels();
        loadIceLevels();
        loadAddOns();
    }

    /**
     * Loads size options for the selected menu item and creates a toggle button for each
     * available size.
     */
    private void loadSizes() {
        List<String> sizes = menuService.getSizesForItem(menuItem.menuItemId());
        for (String size : sizes) {
            Button button = new Button(size);
            button.setPrefSize(90, 40);
            button.setOnAction(_ -> {
                selectedSize = size;
                highlightSelected(sizePane, button);
            });
            sizePane.getChildren().add(button);
        }
    }

    /**
     * Loads sweetness-level options and creates a toggle button for each
     * {@link team44.project2.model.menu.enums.SweetnessLevel} value.
     */
    private void loadSweetnessLevels() {
        for (SweetnessLevel level : SweetnessLevel.values()) {
            Button button = new Button(level.label());
            button.setPrefSize(90, 40);
            button.setOnAction(_ -> {
                selectedSweetness = level.label();
                highlightSelected(sweetnessPane, button);
            });
            sweetnessPane.getChildren().add(button);
        }
    }

    /**
     * Loads ice-level options and creates a toggle button for each
     * {@link team44.project2.model.menu.enums.IceLevel} value.
     */
    private void loadIceLevels() {
        for (IceLevel level : IceLevel.values()) {
            Button button = new Button(level.label());
            button.setPrefSize(90, 40);
            button.setOnAction(_ -> {
                selectedIceLevel = level.label();
                highlightSelected(iceLevelPane, button);
            });
            iceLevelPane.getChildren().add(button);
        }
    }

    /**
     * Loads available add-on items and creates a toggle button for each one. Clicking a
     * button adds or removes the add-on from the selected list.
     */
    private void loadAddOns() {
        List<MenuItem> addOns = menuService.getAddOns();
        for (MenuItem addOn : addOns) {
            Button button = new Button(addOn.name() + " +$" + addOn.basePrice());
            button.setPrefSize(140, 40);
            button.setOnAction(_ -> {
                if (selectedAddOns.contains(addOn)) {
                    selectedAddOns.remove(addOn);
                    button.getStyleClass().remove("selected");
                } else {
                    selectedAddOns.add(addOn);
                    button.getStyleClass().add("selected");
                }
            });
            addOnsPane.getChildren().add(button);
        }
    }

    /**
     * Highlights the selected button in a {@link javafx.scene.layout.FlowPane} by
     * applying the {@code selected} CSS class, and removes it from all other buttons.
     *
     * @param pane     The pane whose children are the option buttons.
     * @param selected The button the user just clicked.
     */
    private void highlightSelected(FlowPane pane, Button selected) {
        pane.getChildren().forEach(node -> node.getStyleClass().remove("selected"));
        selected.getStyleClass().add("selected");
    }

    /**
     * Handles the Confirm button action. Applies default values for any un-selected
     * options, constructs a {@link team44.project2.model.order.CartItem}, passes it
     * to the ordering screen, and closes this window.
     */
    @FXML
    private void handleConfirm() {
        if (selectedSize == null) selectedSize = "regular";
        if (selectedSweetness == null) selectedSweetness = "100%";
        if (selectedIceLevel == null) selectedIceLevel = "Regular Ice";

        CartItem cartItem = new CartItem(
                menuItem,
                selectedSize,
                selectedSweetness,
                selectedIceLevel,
                selectedAddOns
        );

        orderingScreenController.addToCart(cartItem);

        Stage stage = (Stage) itemNameLabel.getScene().getWindow();
        stage.close();
    }

    /**
     * Handles the Back button action, closing this screen without adding an item to the
     * cart.
     */
    @FXML
    private void handleBack() {
        Stage stage = (Stage) itemNameLabel.getScene().getWindow();
        stage.close();
    }
}