package team44.project2.model.order;

import java.math.BigDecimal;
/**
 * Immutable record representing a single line item within an order, linking to the
 * ordered menu item, optional parent item (for add-ons), customisation options (ice
 * and sugar level), quantity, and the price charged at the time of the order.
 *
 * @param orderItemId  The unique primary-key identifier for this line item.
 * @param orderId      The ID of the parent {@link Order} this line item belongs to.
 * @param menuItemId   The ID of the {@link team44.project2.model.menu.MenuItem} ordered.
 * @param parentItemId The {@code orderItemId} of the base item this add-on belongs to,
 *                     or {@code null} if this is itself a base item.
 * @param quantity     The number of units of this item in the order.
 * @param iceLevel     The ice-level customisation applied (e.g. {@code "Less Ice"}).
 * @param sugarLevel   The sugar-level customisation applied (e.g. {@code "50%"}).
 * @param itemPrice    The unit price charged at the time the order was placed.
 */
public record OrderItem(
        int orderItemId,
        int orderId,
        int menuItemId,
        Integer parentItemId,
        int quantity,
        String iceLevel,
        String sugarLevel,
        BigDecimal itemPrice
) {
}