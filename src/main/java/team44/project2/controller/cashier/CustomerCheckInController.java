package team44.project2.controller.cashier;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import team44.project2.model.Customer;
import team44.project2.service.CustomerService;

import static team44.project2.util.StringUtils.toTitleCase;

/**
 * Controller for the customer check-in dialog where cashiers can look up customers
 * by phone number in order to associate them with the current order and display
 * their accumulated reward points.
 */
@Dependent
public class CustomerCheckInController {

    @FXML
    private TextField phoneField;

    @FXML
    private Label statusLabel;

    @FXML
    private Button confirmButton;

    private Customer foundCustomer;
    private CustomerSelectedCallback callback;

    @Inject
    CustomerService customerService;

    /**
     * Registers the callback that will be invoked when a customer is successfully
     * confirmed by the cashier.
     *
     * @param callback The handler to call with the selected {@code Customer}.
     */
    public void setCallback(CustomerSelectedCallback callback) {
        this.callback = callback;
    }

    /**
     * Handles the lookup button action. Queries the database for a customer matching the
     * entered phone number and displays the customer's name and reward points if found,
     * or an error message otherwise.
     */
    @FXML
    private void handleLookup() {
        String phone = phoneField.getText().trim();

        if (phone.isEmpty()) {
            statusLabel.setText("Please enter a phone number");
            return;
        }

        foundCustomer = customerService.findByPhone(phone);

        if (foundCustomer == null) {
            statusLabel.setText("Customer not found");
            confirmButton.setVisible(false);
        } else {
            String name = toTitleCase(foundCustomer.firstName()) + " " + toTitleCase(foundCustomer.lastName());
            statusLabel.setText(name + " — " + foundCustomer.rewardPoints() + " points");
            confirmButton.setVisible(true);
        }
    }

    /**
     * Handles the confirm button action. Invokes the registered callback with the found
     * customer and then closes the dialog.
     */
    @FXML
    private void handleConfirm() {
        if (foundCustomer != null && callback != null) {
            callback.onCustomerSelected(foundCustomer);
        }
        close();
    }

    /**
     * Handles the cancel button action, dismissing the dialog without selecting a
     * customer.
     */
    @FXML
    private void handleCancel() {
        close();
    }

    /**
     * Closes the customer check-in dialog window.
     */
    private void close() {
        Stage stage = (Stage) phoneField.getScene().getWindow();
        stage.close();
    }

    /**
     * Callback interface invoked when a cashier confirms a customer selection.
     */
    @FunctionalInterface
    public interface CustomerSelectedCallback {

        /**
         * Called with the customer that the cashier confirmed.
         *
         * @param customer The confirmed {@code Customer}.
         */
        void onCustomerSelected(Customer customer);
    }
}