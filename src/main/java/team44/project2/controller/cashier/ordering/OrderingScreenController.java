package team44.project2.controller.cashier.ordering;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import team44.project2.controller.EmployeeAware;
import team44.project2.controller.cashier.CustomerCheckInController;
import team44.project2.controller.cashier.payment.PaymentScreenController;
import team44.project2.model.Customer;
import team44.project2.model.menu.MenuItem;
import team44.project2.model.order.CartItem;
import team44.project2.service.MenuService;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static team44.project2.util.StringUtils.toTitleCase;

/**
 * Controller for the main ordering screen used by cashiers to create and manage customer
 * orders, including browsing menu categories, customizing items, initiating customer
 * check-in, and processing payments.
 */
@Dependent
public class OrderingScreenController implements Initializable, EmployeeAware {

    @FXML
    private FlowPane categoryPane;

    @FXML
    private Label customerLabel;

    @FXML
    private ListView<String> ticketListView;

    @FXML
    private Label totalLabel;

    @FXML
    private Button chargeButton;

    @FXML
    private TextField orderNumberField;

    @Inject
    MenuService menuService;

    @Inject
    Instance<Object> cdiInstance;

    private int employeeId;
    private BigDecimal total = BigDecimal.ZERO;
    private final List<CartItem> cart = new ArrayList<>();
    private Customer currentCustomer;

    /**
     * Stores the ID of the currently logged-in employee for use when submitting orders.
     *
     * @param employeeId The database primary key of the authenticated employee.
     */
    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    /**
     * Initialises the ordering screen by loading the available menu categories.
     *
     * @param location  Unused; may be {@code null}.
     * @param resources Unused; may be {@code null}.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCategories();
    }

    /**
     * Loads menu categories from the service and creates a button for each one so the
     * cashier can browse items by category.
     */
    private void loadCategories() {
        List<String> categories = menuService.getCategories();
        for (String category : categories) {
            Button button = new Button(category);
            button.setPrefSize(120, 80);
            button.setOnAction(_ -> openCategoryItems(category));
            categoryPane.getChildren().add(button);
        }
    }

    /**
     * Opens a modal dialog listing the items in the selected category.
     *
     * @param category The category name whose items should be displayed.
     */
    private void openCategoryItems(String category) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/cashier/ordering/CategoryItems.fxml")
            );
            loader.setControllerFactory(clazz -> {
                if (clazz == CategoryItemsController.class) {
                    CategoryItemsController controller = cdiInstance.select(CategoryItemsController.class).get();
                    controller.setCategory(category);
                    controller.setOrderingScreenController(this);
                    return controller;
                }
                return cdiInstance.select(clazz).get();
            });

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(categoryPane.getScene().getWindow());
            dialog.setTitle(category);
            dialog.setScene(new Scene(loader.load()));
            dialog.showAndWait();
        } catch (Exception e) {
            Log.error("Failed to open category items", e);
        }
    }

    /**
     * Adds a fully customised {@link team44.project2.model.order.CartItem} to the
     * current order, updates the ticket list view, and recalculates the running total.
     *
     * @param cartItem The item to add, including its size, sweetness, ice-level, and
     *                 any add-ons.
     */
    public void addToCart(CartItem cartItem) {
        cart.add(cartItem);

        String entry = cartItem.item().name() + " (" + cartItem.size() + ", " + cartItem.sweetness() + ", " + cartItem.iceLevel() + ")";
        if (!cartItem.addOns().isEmpty()) {
            List<String> addOnNames = cartItem.addOns().stream().map(MenuItem::name).toList();
            entry += " + " + String.join(", ", addOnNames);
        }
        entry += " - $" + cartItem.totalPrice();

        ticketListView.getItems().add(entry);
        total = total.add(cartItem.totalPrice());
        updateTotal();
    }

    /**
     * Refreshes the total label and charge button text to reflect the current cart total.
     */
    private void updateTotal() {
        totalLabel.setText("Total: $" + total);
        chargeButton.setText("Charge $" + total);
    }

    /**
     * Opens the customer check-in modal so the cashier can associate a loyalty customer
     * with the current order.
     */
    @FXML
    private void handleCustomerCheckIn() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/cashier/CustomerCheckIn.fxml")
            );
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(categoryPane.getScene().getWindow());
            dialog.setTitle("Customer Check In");
            dialog.setScene(new Scene(loader.load()));

            CustomerCheckInController controller = loader.getController();
            controller.setCallback(customer -> {
                currentCustomer = customer;
                String name = toTitleCase(customer.firstName()) + " " + toTitleCase(customer.lastName());
                customerLabel.setText(name + " (" + customer.rewardPoints() + " pts)");
            });

            dialog.showAndWait();
        } catch (Exception e) {
            Log.error("Failed to open customer check in", e);
        }
    }

    /**
     * Opens the payment modal to process the current order. If payment succeeds the order
     * is reset via {@link #resetOrder()}.
     */
    @FXML
    private void handleCharge() {
        if (cart.isEmpty()) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/cashier/payment/PaymentScreen.fxml")
            );
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(categoryPane.getScene().getWindow());
            dialog.setTitle("Payment");
            dialog.setScene(new Scene(loader.load()));

            PaymentScreenController controller = loader.getController();
            controller.setOrderData(employeeId, currentCustomer, cart);
            controller.setOnNewSale(this::resetOrder);
            controller.showPaymentMethods();

            dialog.showAndWait();
        } catch (Exception e) {
            Log.error("Failed to open payment screen", e);
        }
    }

    /**
     * Resets the ordering screen to a clean state, clearing the cart, ticket list,
     * running total, and associated customer.
     */
    private void resetOrder() {
        cart.clear();
        ticketListView.getItems().clear();
        total = BigDecimal.ZERO;
        currentCustomer = null;
        customerLabel.setText("No Customer");
        updateTotal();
    }

    /**
     * Handles the Log Out button action. Prompts for confirmation if there are unsaved
     * cart items, then navigates back to the login screen.
     */
    @FXML
    private void handleLogOut() {
        if (!cart.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Log Out");
            alert.setHeaderText("You have items in the cart");
            alert.setContentText("Log out and discard current order?");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/Login.fxml"));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());
            Stage stage = (Stage) categoryPane.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Login");
        } catch (Exception e) {
            Log.error("Failed to navigate to login", e);
        }
    }
}