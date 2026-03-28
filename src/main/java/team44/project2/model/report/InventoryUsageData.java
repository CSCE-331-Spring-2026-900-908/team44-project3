package team44.project2.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * Immutable record representing the usage of a single inventory item on a specific date,
 * used for the inventory-usage report to help management track consumption rates.
 *
 * @param usageDate    The date on which the inventory consumption occurred.
 * @param itemName     The name of the inventory ingredient tracked.
 * @param quantityUsed The total quantity of the item consumed on this date.
 */
public record InventoryUsageData(
        LocalDate usageDate,
        String itemName,
        BigDecimal quantityUsed
) {}