package team44.project2.model.order;

import team44.project2.model.menu.MenuItem;

import java.util.List;
import java.math.BigDecimal;
/**
 * Immutable record representing a single item in the cashier's in-progress shopping
 * cart, bundling the selected {@link team44.project2.model.menu.MenuItem} with any
 * customisation choices (ice level, sweetness) and chosen add-ons.
 *
 * @param item      The base menu item selected by the customer.
 * @param size      The size chosen for this cart entry (e.g. {@code "Medium"}).
 * @param sweetness The sweetness-level label chosen (e.g. {@code "50%"}).
 * @param iceLevel  The ice-level label chosen (e.g. {@code "Less Ice"}).
 * @param addOns    The list of add-on menu items selected alongside the base item.
 */
public record CartItem(
        MenuItem item,
        String size,
        String sweetness,
        String iceLevel,
        List<MenuItem> addOns
) {
    /**
     * Computes the total price of this cart item as the sum of the base menu-item price
     * and the prices of all selected add-ons.
     *
     * @return The computed grand total as a {@link java.math.BigDecimal}.
     */
    public BigDecimal totalPrice() {
        BigDecimal total = item.basePrice();
        for (MenuItem addOn : addOns) {
            total = total.add(addOn.basePrice());
        }
        return total;
    }
}