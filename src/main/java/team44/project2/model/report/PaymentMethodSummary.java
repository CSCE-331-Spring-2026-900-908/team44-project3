package team44.project2.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Simple data holder for the X‑report: number of orders and total sales grouped by
 * payment method for a given hour/day.
 *
 * @param date          The calendar date this summary covers.
 * @param hour          The zero-based hour of day (0–23) this summary covers.
 * @param paymentMethod The payment method label (e.g. {@code "CASH"}, {@code "CARD"}).
 * @param orderCount    The number of orders that used this payment method in this slot.
 * @param totalSales    The total revenue from orders using this payment method.
 */
public record PaymentMethodSummary(
        LocalDate date,
        int hour,
        String paymentMethod,
        int orderCount,
        BigDecimal totalSales
) {
}
