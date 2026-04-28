package team44.project2.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import team44.project2.model.report.DailySalesSummary;
import team44.project2.model.report.ProductSalesData;
import team44.project2.model.report.InventoryUsageData;
import team44.project2.model.report.PaymentMethodSummary;

import team44.project2.model.report.ZReportResult;
import team44.project2.model.report.ZReportTotals;
import java.math.BigDecimal;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class responsible for generating all management reports: daily sales summaries,
 * product-level sales breakdowns, sales by category, inventory usage, X-report hourly
 * payment summaries, and the end-of-day Z-report with reset logic.
 */
@ApplicationScoped
public class ReportService {
    //sql queries for report generation
    private static final String GET_PRODUCT_SALES = """
            SELECT DATE(o.timestamp) as sale_date, 
                   mi.name as product_name,
                   SUM(oi.quantity) as total_quantity,
                   SUM(oi.quantity * oi.item_price) as total_price
            FROM orders o
            JOIN order_items oi ON o.order_id = oi.order_id
            JOIN menu_items mi ON oi.menu_item_id = mi.menu_item_id
            WHERE DATE(o.timestamp) >= ? AND DATE(o.timestamp) <= ?
              AND oi.parent_item_id IS NULL
            GROUP BY DATE(o.timestamp), mi.name
            ORDER BY sale_date, product_name
            """;

    private static final String GET_DAILY_SALES_SUMMARY = """
            SELECT DATE(o.timestamp) as sale_date,
                   SUM(o.total_price) as total_sales,
                   SUM(oi.quantity) as total_quantity
            FROM orders o
            JOIN order_items oi ON o.order_id = oi.order_id
            WHERE DATE(o.timestamp) >= ? AND DATE(o.timestamp) <= ?
              AND oi.parent_item_id IS NULL
            GROUP BY DATE(o.timestamp)
            ORDER BY sale_date
            """;

    private static final String GET_SALES_BY_CATEGORY = """
            SELECT DATE(o.timestamp) as sale_date,
                   mi.category,
                   SUM(oi.quantity) as total_quantity,
                   SUM(oi.quantity * oi.item_price) as total_price
            FROM orders o
            JOIN order_items oi ON o.order_id = oi.order_id
            JOIN menu_items mi ON oi.menu_item_id = mi.menu_item_id
            WHERE DATE(o.timestamp) >= ? AND DATE(o.timestamp) <= ?
              AND oi.parent_item_id IS NULL
            GROUP BY DATE(o.timestamp), mi.category
            ORDER BY sale_date, mi.category
            """;

    private static final String GET_INVENTORY_USAGE = """
            SELECT DATE(o.timestamp) AS usage_date,
                i.item_name,
                SUM(oi.quantity * mc.quantity) AS quantity_used
            FROM orders o
            JOIN order_items oi 
                ON o.order_id = oi.order_id
            JOIN menu_item_contents mc 
                ON mc.menu_item_id = oi.menu_item_id
            JOIN inventory i 
                ON i.inventory_id = mc.inventory_id
            WHERE DATE(o.timestamp) >= ? 
            AND DATE(o.timestamp) <= ?
            AND oi.parent_item_id IS NULL
            GROUP BY DATE(o.timestamp), i.item_name
            ORDER BY usage_date, i.item_name
            """;

    private static final String GET_HOURLY_PAYMENT_SUMMARY = 
    """
            SELECT DATE(o.timestamp) AS sale_date,
                   o.payment_method,
                   COUNT(*) AS order_count,
                   SUM(o.total_price) AS total_sales
            FROM orders o
            WHERE DATE(o.timestamp) = CURRENT_DATE
              AND EXTRACT(HOUR FROM o.timestamp) = ?
            GROUP BY DATE(o.timestamp), o.payment_method
            """;

    private static final String HAS_Z_RUN_TODAY = 
    """
    SELECT 1 FROM z_report_run WHERE business_date = CURRENT_DATE
    """;


    private static final String INSERT_Z_RUN =
    """
    INSERT INTO z_report_run (business_date, generated_by)
    VALUES (CURRENT_DATE, ?)
    """;

    private static final String INSERT_SIGNATURE = 
    """
    INSERT INTO z_report_signatures (business_date, employee_id)
    VALUES (CURRENT_DATE, ?)
    ON CONFLICT (business_date, employee_id) DO NOTHING
    """;

    private static final String GET_SIGNATURE_EMPLOYEES_TODAY = 
    """
    SELECT employee_id
    FROM z_report_signatures
    WHERE business_date = CURRENT_DATE
    ORDER BY employee_id
    """;

    private static final String GET_DAILY_PAYMENT_SUMMARY =
    """
    SELECT DATE(o.timestamp) AS sale_date,
        o.payment_method,
        COUNT(*) AS order_count,
        COALESCE(SUM(o.total_price), 0) AS total_sales
    FROM orders o
    WHERE DATE(o.timestamp) = CURRENT_DATE
    GROUP BY DATE(o.timestamp), o.payment_method
    """;

    private static final String RESET_TOTALS_TODAY = 
    """
    UPDATE report_totals
    SET gross_sales=0, tips=0,
        cash_total=0, card_total=0, gift_total=0, onfile_total=0,
        tax=0, discounts=0, voids=0, service_charges=0
    WHERE business_date = CURRENT_DATE
    """;

    private static final String ENSURE_TOTALS_ROW_TODAY =
    """
    INSERT INTO report_totals (business_date)
    VALUES(CURRENT_DATE)
    ON CONFLICT (business_date) DO NOTHING
    """;

    private static final String GET_DAILY_SALES_AND_TIPS = 
    """
    SELECT COALESCE(SUM(o.total_price), 0) AS gross_sales,
        COALESCE(SUM(o.tip_amount),0) AS tips
        FROM orders o
        WHERE DATE(o.timestamp) = CURRENT_DATE
    """;

    @Inject
    DataSource dataSource;

    /**
     * Checks whether the Z-report has already been generated for today's business date.
     *
     * @return {@code true} if a Z-report row exists for today; {@code false} otherwise
     *         or if a database error occurs.
     */
    public boolean hasZRunToday() {
        try(Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(HAS_Z_RUN_TODAY);
            ResultSet rs = stmt.executeQuery()){
                Log.info("DB URL (hasZRunToday) = " + conn.getMetaData().getURL());
                return rs.next();
         } catch(SQLException e){
            Log.error("Error checking if Z-report ran today", e);
            return false;
         }
    }


    /**
     * Records an employee's signature against today's Z-report closeout.
     * Silently ignores duplicate signatures thanks to the {@code ON CONFLICT DO NOTHING}
     * clause in the SQL.
     *
     * @param employeeId The employee signing the closeout.
     */
    public void signZReportToday(int employeeId) {
        try(Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(INSERT_SIGNATURE)){
                stmt.setInt(1, employeeId);
                stmt.executeUpdate();
        } catch(SQLException e){
            Log.error("Error inserting Z-report signature", e);
        }
    }

    /**
     * Generates the end-of-day Z-report for the current business date. This method may
     * only be called once per day; a second call will throw
     * {@link IllegalStateException}. It collects the daily payment breakdown, gross sales,
     * tips, and employee signatures, resets the {@code report_totals} table, and returns
     * a {@link team44.project2.model.report.ZReportResult}.
     *
     * @param generatedBy A string identifying which employee or process triggered the
     *                    report (stored for audit purposes).
     * @return A fully-populated {@code ZReportResult}.
     * @throws IllegalStateException If the Z-report has already been run today.
     * @throws RuntimeException      If any other database error occurs.
     */
    public ZReportResult runZReport(String generatedBy){
        try(Connection conn = dataSource.getConnection()){
            conn.setAutoCommit(false);

            //only once a day
            try(PreparedStatement stmt = conn.prepareStatement(INSERT_Z_RUN)){
                stmt.setString(1, generatedBy);
                stmt.executeUpdate();
            } catch(SQLException dup){
                conn.rollback();
                throw new IllegalStateException("Z-report already generated today.");
            }

            //payment breakdown

            List<PaymentMethodSummary> payments = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(GET_DAILY_PAYMENT_SUMMARY);
                ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("sale_date");
                    payments.add(new PaymentMethodSummary(
                            sqlDate != null ? sqlDate.toLocalDate() : null,
                            -1, // hour not applicable, this is from X-report
                            rs.getString("payment_method"),
                            rs.getInt("order_count"),
                            rs.getBigDecimal("total_sales")
                    ));
                }
            }


            //gross sales + tips
            BigDecimal grossSales = BigDecimal.ZERO;
            BigDecimal tips = BigDecimal.ZERO;
            try (PreparedStatement ps = conn.prepareStatement(GET_DAILY_SALES_AND_TIPS);
                ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal gs = rs.getBigDecimal("gross_sales");
                    BigDecimal tp = rs.getBigDecimal("tips");
                    grossSales = (gs != null) ? gs : BigDecimal.ZERO;
                    tips = (tp != null) ? tp : BigDecimal.ZERO;
                }
            }

            //get signatures
            List<Integer> signedEmployeeIds = new ArrayList<>();
            try(PreparedStatement ps = conn.prepareStatement(GET_SIGNATURE_EMPLOYEES_TODAY);
                ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    signedEmployeeIds.add(rs.getInt("employee_id"));
                }
            }

            //totals
            //TODO: Calculate totals
            ZReportTotals totals = new ZReportTotals(
                    grossSales,
                    tips,
                    BigDecimal.ZERO, // default for now, not in schema
                    BigDecimal.ZERO, 
                    BigDecimal.ZERO, 
                    BigDecimal.ZERO  
            );


            //reset Z-totals table - this is safe because the table does not hold any unique data, only pulls what is needed to be queried daily
            try(PreparedStatement ps = conn.prepareStatement(ENSURE_TOTALS_ROW_TODAY)){
                ps.executeUpdate();
            }
            try(PreparedStatement ps = conn.prepareStatement(RESET_TOTALS_TODAY)){
                ps.executeUpdate();
            }
            
            conn.commit();
            return new ZReportResult(LocalDate.now(), totals, payments, signedEmployeeIds);
        } catch(RuntimeException e){
            throw e;
        } catch(Exception e){
            Log.error("Error running Z-report", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns product-level sales data for each day in the given date range.
     *
     * @param startDate The inclusive start date.
     * @param endDate   The inclusive end date.
     * @return A list of {@link team44.project2.model.report.ProductSalesData} records;
     *         empty if no orders exist in the period.
     */
    public List<ProductSalesData> getProductSales(LocalDate startDate, LocalDate endDate) {
        List<ProductSalesData> salesData = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_PRODUCT_SALES)) {

            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    salesData.add(mapProductSalesData(rs));
                }
            }
        } catch (Exception e) {
            Log.error("Failed to load product sales data", e);
        }

        return salesData;
    }

    /**
     * Returns a per-day sales summary (total revenue and total quantity) for each date
     * in the given range.
     *
     * @param startDate The inclusive start date.
     * @param endDate   The inclusive end date.
     * @return A list of {@link team44.project2.model.report.DailySalesSummary} records.
     */
    public List<DailySalesSummary> getDailySalesSummary(LocalDate startDate, LocalDate endDate) {
        List<DailySalesSummary> summaries = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_DAILY_SALES_SUMMARY)) {

            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    summaries.add(mapDailySalesSummary(rs));
                }
            }
        } catch (Exception e) {
            Log.error("Failed to load daily sales summary", e);
        }

        return summaries;
    }

    /**
     * Returns product-level sales data grouped by category (instead of individual item
     * name) for each day in the given range.
     *
     * @param startDate The inclusive start date.
     * @param endDate   The inclusive end date.
     * @return A list of {@link team44.project2.model.report.ProductSalesData} records
     *         where the {@code productName} field contains the category.
     */
    public List<ProductSalesData> getSalesByCategory(LocalDate startDate, LocalDate endDate) {
        List<ProductSalesData> salesData = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_SALES_BY_CATEGORY)) {

            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    salesData.add(mapProductSalesData(rs));
                }
            }
        } catch (Exception e) {
            Log.error("Failed to load sales by category", e);
        }

        return salesData;
    }

    private ProductSalesData mapProductSalesData(ResultSet rs) throws Exception {
        java.sql.Date sqlDate = rs.getDate("sale_date");
        java.time.LocalDate saleDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;
        
        return new ProductSalesData(
                saleDate,
                rs.getString("product_name") != null 
                    ? rs.getString("product_name") 
                    : rs.getString("category"),
                rs.getInt("total_quantity"),
                rs.getBigDecimal("total_price")
        );
    }

    private DailySalesSummary mapDailySalesSummary(ResultSet rs) throws Exception {
        java.sql.Date sqlDate = rs.getDate("sale_date");
        java.time.LocalDate saleDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;
        
        return new DailySalesSummary(
                saleDate,
                rs.getBigDecimal("total_sales"),
                rs.getInt("total_quantity")
        );
    }

    /**
     * Returns inventory usage data (item name and quantity consumed) aggregated by day
     * for the given date range.
     *
     * @param startDate The inclusive start date.
     * @param endDate   The inclusive end date.
     * @return A list of {@link team44.project2.model.report.InventoryUsageData} records.
     */
    public List<InventoryUsageData> getInventoryUsage(LocalDate startDate, LocalDate endDate) {

        List<InventoryUsageData> usageList = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(GET_INVENTORY_USAGE)) {

            ps.setDate(1, java.sql.Date.valueOf(startDate));
            ps.setDate(2, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    usageList.add(new InventoryUsageData(
                            rs.getDate("usage_date").toLocalDate(),
                            rs.getString("item_name"),
                            rs.getBigDecimal("quantity_used")
                    ));
                }
            }

        } catch (SQLException e) {
            Log.error("Error fetching inventory usage report", e);
        }

        return usageList;
    }

    /**
     * Returns the payment-method breakdown for all orders placed during the given hour
     * of today's business date. If the Z-report has already been run today, returns
     * zero-value entries for each payment method.
     *
     * @param hour The zero-based hour (0–23) to query.
     * @return A list of {@link team44.project2.model.report.PaymentMethodSummary}
     *         records, one per payment method found (or the four default ones if the
     *         Z-report has run).
     */
    public List<PaymentMethodSummary> getHourlyPaymentSummary(int hour) {
        List<PaymentMethodSummary> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_HOURLY_PAYMENT_SUMMARY)) {

            ps.setInt(1, hour);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("sale_date");
                    results.add(new PaymentMethodSummary(
                            sqlDate != null ? sqlDate.toLocalDate() : null,
                            hour,
                            rs.getString("payment_method"),
                            rs.getInt("order_count"),
                            rs.getBigDecimal("total_sales")
                    ));
                }
            }
        } catch (SQLException e) {
            Log.error("Error fetching hourly payment summary", e);
        }

        return results;
    }
}

