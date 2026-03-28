package team44.project2.controller.cashier.payment;

import jakarta.enterprise.context.Dependent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import team44.project2.model.order.Order;

import java.math.BigDecimal;

/**
 * Controller for the transaction-complete screen shown after a successful order
 * submission, allowing cashiers to print or email a receipt or immediately start a
 * new sale.
 */
@Dependent
public class TransactionCompleteController {

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label tipLabel;

    @FXML
    private Label grandTotalLabel;

    private Order order;
    private Runnable onNewSale;

    /**
     * Sets the order data required to display the subtotal, tip, and grand total on the
     * transaction-complete screen.
     *
     * @param order    The persisted order returned by the service (retained for future use).
     * @param subtotal The subtotal before tip.
     * @param tip      The tip amount selected by the cashier.
     */
    public void setOrderData(Order order, BigDecimal subtotal, BigDecimal tip) {
        this.order = order;
        subtotalLabel.setText("Subtotal: $" + subtotal);
        tipLabel.setText("Tip: $" + tip);
        grandTotalLabel.setText("Total: $" + subtotal.add(tip));
    }

    /**
     * Registers a callback that is invoked when the cashier starts a new sale from this
     * screen.
     *
     * @param onNewSale The action to execute when transitioning to a new order.
     */
    public void setOnNewSale(Runnable onNewSale) {
        this.onNewSale = onNewSale;
    }

    /**
     * Handles the Print Receipt button action by showing an informational alert.
     */
    @FXML
    private void handlePrintReceipt() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Receipt");
        alert.setHeaderText(null);
        alert.setContentText("Receipt sent to printer.");
        alert.showAndWait();
    }

    /**
     * Handles the Email Receipt button action by showing an informational alert.
     */
    @FXML
    private void handleEmailReceipt() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Receipt");
        alert.setHeaderText(null);
        alert.setContentText("Receipt sent to customer's email.");
        alert.showAndWait();
    }

    /**
     * Handles the No Receipt button action by immediately starting a new sale.
     */
    @FXML
    private void handleNoReceipt() {
        startNewSale();
    }

    /**
     * Handles the New Sale button action by starting a new sale.
     */
    @FXML
    private void handleNewSale() {
        startNewSale();
    }

    /**
     * Invokes the new-sale callback if registered and then closes this screen.
     */
    private void startNewSale() {
        if (onNewSale != null) {
            onNewSale.run();
        }
        Stage stage = (Stage) grandTotalLabel.getScene().getWindow();
        stage.close();
    }
}