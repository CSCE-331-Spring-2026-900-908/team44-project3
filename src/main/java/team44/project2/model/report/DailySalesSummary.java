package team44.project2.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * Immutable record representing a daily sales summary, capturing the calendar date,
 * total revenue, and total quantity of items sold for that day.
 *
 * @param saleDate      The calendar date this summary covers.
 * @param totalSales    The total revenue collected on this date.
 * @param totalQuantity The total number of individual items sold on this date.
 */
public record DailySalesSummary(
        LocalDate saleDate,
        BigDecimal totalSales,
        Integer totalQuantity
) {
}
