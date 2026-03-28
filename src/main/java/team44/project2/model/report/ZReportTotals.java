package team44.project2.model.report;
import java.math.BigDecimal;

/**
 * Immutable record holding the monetary totals captured during a Z-report run. Fields
 * that are not yet tracked in the database schema (tax, discounts, voids, service
 * charges) default to zero and are reserved for future expansion.
 *
 * @param grossSales     The total revenue from all sales before tips.
 * @param tips           The total tip amount collected during the business day.
 * @param tax            Reserved for future use; currently always zero.
 * @param discounts      Reserved for future use; currently always zero.
 * @param voids          Reserved for future use; currently always zero.
 * @param serviceCharges Reserved for future use; currently always zero.
 */
public record ZReportTotals(
        BigDecimal grossSales,
        BigDecimal tips,

        //future additions to schema
        BigDecimal tax,
        BigDecimal discounts,
        BigDecimal voids,
        BigDecimal serviceCharges
) {}