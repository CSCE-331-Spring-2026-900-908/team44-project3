package team44.project2.model.menu;
import java.math.BigDecimal;
/**
 * Immutable record representing the link between a menu item and an inventory ingredient,
 * specifying how much of that ingredient is consumed each time the menu item is ordered.
 *
 * @param menuItemId  The ID of the {@link team44.project2.model.menu.MenuItem} that consumes
 *                    the ingredient.
 * @param inventoryId The ID of the {@link team44.project2.model.Inventory} ingredient.
 * @param quantity    The amount of the ingredient consumed per order of the menu item.
 */
public record MenuItemContent(
        int menuItemId,
        int inventoryId,
        BigDecimal quantity
) {
}