package team44.project2.model.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * Immutable record representing a completed order, containing the IDs of the employee
 * who processed it and the optional loyalty customer, together with the order timestamp,
 * total price, tip, and payment method used.
 *
 * @param orderId       The unique primary-key identifier for the order.
 * @param employeeId    The ID of the cashier who processed this order.
 * @param customerId    The loyalty customer's ID, or {@code null} for a guest transaction.
 * @param timestamp     The date and time at which the order was submitted.
 * @param totalPrice    The subtotal of all items before the tip.
 * @param tipAmount     The tip amount added by the customer.
 * @param paymentMethod The payment method used (e.g. {@code "CASH"}, {@code "CARD"}).
 * @param pointsEarned  The number of reward points earned from this order.
 */
public record Order(
        int orderId,
        Integer employeeId,
        Integer customerId,
        LocalDateTime timestamp,
        BigDecimal totalPrice,
        BigDecimal tipAmount,
        String paymentMethod,
        int pointsEarned
) {
}