package team44.project2.model.menu;
import java.math.BigDecimal;

public record MenuItemContentWithName(
        int menuItemId,
        int inventoryId,
        BigDecimal quantity,
        String itemName
) {
}
