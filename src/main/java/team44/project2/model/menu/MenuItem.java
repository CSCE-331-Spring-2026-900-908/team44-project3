package team44.project2.model.menu;
import java.math.BigDecimal;
/**
 * Immutable record representing a single item on the cafe's menu, including its name,
 * category, available size, base price, and whether it is currently offered to customers.
 *
 * @param menuItemId  The unique primary-key identifier for the menu item.
 * @param name        The display name of the menu item (e.g. {@code "Matcha Latte"}).
 * @param category    The menu category (e.g. {@code "Milk Tea"}, {@code "Add-on"}).
 * @param size        The size variant offered (e.g. {@code "Medium"}, {@code "Large"}).
 * @param basePrice   The standard retail price before any customisation surcharges.
 * @param isAvailable {@code true} if the item is currently on sale; {@code false} if hidden.
 */
public record MenuItem(
        int menuItemId,
        String name,
        String category,
        String size,
        BigDecimal basePrice,
        boolean isAvailable
) {
}