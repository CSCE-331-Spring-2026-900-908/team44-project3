package team44.project2.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * Immutable record representing a restock order placed by an employee to replenish a
 * low-stock inventory item, tracking the quantity ordered, order timestamp, and current
 * fulfilment status.
 *
 * @param restockId       The unique primary-key identifier for this restock order.
 * @param employeeId      The ID of the employee who placed the order.
 * @param inventoryId     The ID of the inventory item being restocked.
 * @param quantityOrdered The quantity requested from the supplier.
 * @param orderDate       The timestamp at which the order was created.
 * @param status          The current fulfilment status (e.g. {@code "PENDING"}, {@code "RECEIVED"}).
 */
public record RestockOrder(
        int restockId,
        int employeeId,
        int inventoryId,
        BigDecimal quantityOrdered,
        LocalDateTime orderDate,
        String status
) {}