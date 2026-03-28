package team44.project2.model.report;
import java.time.LocalDate;
import java.util.List;

/**
 * Immutable record representing the full result of a Z-report run, including the
 * business date, aggregate totals, per-payment-method breakdown, and the list of
 * employee IDs who signed off on the closeout.
 *
 * @param businessDate      The calendar date the Z-report was run for.
 * @param totals            The aggregate monetary totals for the business day.
 * @param paymentBreakdown  The per-payment-method breakdown of orders and revenue.
 * @param signedEmployeeIds The IDs of employees who authorised the end-of-day closeout.
 */
public record ZReportResult(
    LocalDate businessDate,
    ZReportTotals totals,
    List<PaymentMethodSummary> paymentBreakdown,
    List<Integer> signedEmployeeIds
) {}

