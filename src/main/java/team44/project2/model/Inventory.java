package team44.project2.model;

import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * Immutable record representing a single inventory item in the cafe's inventory
 * management system, including current quantity, reorder threshold, unit of measurement,
 * supplier cost, and the date it was last restocked.
 *
 * @param inventoryId      The unique primary-key identifier for the inventory item.
 * @param itemName         The human-readable name of the ingredient or supply item.
 * @param category         The grouping category (e.g. {@code "Dairy"}, {@code "Syrup"}).
 * @param currentQuantity  The quantity currently in stock.
 * @param unit             The unit of measurement (e.g. {@code "kg"}, {@code "L"}).
 * @param reorderThreshold The stock level at which a restock order should be triggered.
 * @param supplierCost     The cost per unit charged by the supplier.
 * @param lastRestockDate  The date the item was most recently restocked.
 */
public record Inventory(
        int inventoryId,
        String itemName,
        String category,
        BigDecimal currentQuantity,
        String unit,
        BigDecimal reorderThreshold,
        BigDecimal supplierCost,
        LocalDate lastRestockDate
) {}