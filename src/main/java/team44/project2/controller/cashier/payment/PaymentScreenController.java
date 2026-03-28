package team44.project2.controller.cashier.payment;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import team44.project2.model.Customer;
import team44.project2.model.order.CartItem;
import team44.project2.model.order.Order;
import team44.project2.service.OrderService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the payment screen where cashiers select a payment method, optionally
 * add a tip, confirm the order total, and then submit the order to the backend for
 * processing.
 */
@Dependent
public class PaymentScreenController implements Initializable {

    @FXML
    private Label totalLabel;

    @FXML
    private Label instructionLabel;

    @FXML
    private VBox contentPane;

    @FXML
    private Button cancelButton;

    @Inject
    OrderService orderService;

    @Inject
    Instance<Object> cdiInstance;

    private int employeeId;
    private Customer customer;
    private List<CartItem> cart;
    private BigDecimal subtotal;
    private BigDecimal tipAmount = BigDecimal.ZERO;
    private String paymentMethod;
    private Runnable onNewSale;

    /**
     * Receives all data needed to process the order.
     *
     * @param employeeId The ID of the cashier taking the order.
     * @param customer   The associated loyalty customer, or {@code null} if none.
     * @param cart       The list of customised items in the current order.
     */
    public void setOrderData(int employeeId, Customer customer, List<CartItem> cart) {
        this.employeeId = employeeId;
        this.customer = customer;
        this.cart = cart;
        this.subtotal = cart.stream()
                .map(CartItem::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Registers a callback to be invoked when the transaction is complete and the
     * cashier is ready to start a new sale.
     *
     * @param onNewSale The runnable to execute when a new sale begins.
     */
    public void setOnNewSale(Runnable onNewSale) {
        this.onNewSale = onNewSale;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    /**
     * Shows the payment-method selection panel with buttons for each available method.
     */
    public void showPaymentMethods() {
        totalLabel.setText("$" + subtotal);
        instructionLabel.setText("Select Payment Method");
        contentPane.getChildren().clear();
        cancelButton.setVisible(true);

        String[] methods = {"Credit Card", "Cash", "Gift Card"};
        for (String method : methods) {
            Button button = new Button(method);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setPrefHeight(50);
            button.setOnAction(_ -> {
                paymentMethod = method;
                showTipSelection();
            });
            contentPane.getChildren().add(button);
        }
    }

    /**
     * Displays tip-percentage buttons after a payment method has been chosen.
     */
    private void showTipSelection() {
        instructionLabel.setText("Add a Tip?");
        contentPane.getChildren().clear();

        String[] tipLabels = {"No Tip", "15%", "18%", "20%"};
        BigDecimal[] tipRates = {
                BigDecimal.ZERO,
                new BigDecimal("0.15"),
                new BigDecimal("0.18"),
                new BigDecimal("0.20")
        };

        for (int i = 0; i < tipLabels.length; i++) {
            final BigDecimal rate = tipRates[i];
            BigDecimal tip = subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);

            Button button = new Button(tipLabels[i]);
            if (rate.compareTo(BigDecimal.ZERO) > 0) {
                button.setText(tipLabels[i] + " ($" + tip + ")");
            }
            button.setMaxWidth(Double.MAX_VALUE);
            button.setPrefHeight(50);
            button.setOnAction(_ -> {
                tipAmount = tip;
                showTipConfirmation();
            });
            contentPane.getChildren().add(button);
        }

        Button backButton = new Button("Back");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setPrefHeight(50);
        backButton.setOnAction(_ -> showPaymentMethods());
        contentPane.getChildren().add(backButton);
    }

    /**
     * Shows a summary of the subtotal, tip, and grand total with Confirm / Back buttons
     * so the cashier can review before finalising the transaction.
     */
    private void showTipConfirmation() {
        BigDecimal grandTotal = subtotal.add(tipAmount);

        instructionLabel.setText("Confirm Payment");
        totalLabel.setText("$" + grandTotal);
        contentPane.getChildren().clear();

        Label subtotalLine = new Label("Subtotal: $" + subtotal);
        Label tipLine = new Label("Tip: $" + tipAmount);
        Label totalLine = new Label("Total: $" + grandTotal);

        Button confirmButton = new Button("Confirm");
        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setPrefHeight(50);
        confirmButton.setOnAction(_ -> submitOrder());

        Button changeTipButton = new Button("Change Tip");
        changeTipButton.setMaxWidth(Double.MAX_VALUE);
        changeTipButton.setPrefHeight(50);
        changeTipButton.setOnAction(_ -> showTipSelection());

        Button changePaymentButton = new Button("Change Payment Method");
        changePaymentButton.setMaxWidth(Double.MAX_VALUE);
        changePaymentButton.setPrefHeight(50);
        changePaymentButton.setOnAction(_ -> showPaymentMethods());

        contentPane.getChildren().addAll(
                subtotalLine, tipLine, totalLine,
                confirmButton, changeTipButton, changePaymentButton
        );
    }

    /**
     * Submits the order to the backend service and navigates to the transaction-complete
     * screen if successful. Displays an error message and returns to payment-method
     * selection if the submission fails.
     */
    private void submitOrder() {
        instructionLabel.setText("Processing...");
        contentPane.getChildren().clear();
        cancelButton.setVisible(false);

        Integer customerId = customer != null ? customer.customerId() : null;

        Order order = orderService.submitOrder(
                employeeId, customerId, paymentMethod, tipAmount, cart
        );

        if (order != null) {
            showTransactionComplete(order);
        } else {
            instructionLabel.setText("Transaction failed. Try again.");
            cancelButton.setVisible(true);
            showPaymentMethods();
        }
    }

    /**
     * Loads and displays the transaction-complete screen after a successful order
     * submission.
     *
     * @param order The persisted {@link team44.project2.model.order.Order} returned by
     *              the service.
     */
    private void showTransactionComplete(Order order) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/cashier/payment/TransactionComplete.fxml")
            );
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());

            Stage stage = (Stage) totalLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));

            TransactionCompleteController controller = loader.getController();
            controller.setOrderData(order, subtotal, tipAmount);
            controller.setOnNewSale(onNewSale);
        } catch (Exception e) {
            Log.error("Failed to show transaction complete", e);
        }
    }

    /**
     * Handles the Cancel button action, closing the payment dialog and returning the
     * cashier to the ordering screen.
     */
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) totalLabel.getScene().getWindow();
        stage.close();
    }
}