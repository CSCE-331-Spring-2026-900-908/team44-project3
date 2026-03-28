package team44.project2.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import team44.project2.model.Customer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Service class responsible for customer data operations, specifically looking up
 * customers by their phone number so cashiers can associate loyalty accounts with
 * in-progress orders.
 */
@ApplicationScoped
public class CustomerService {
    //sql query to find a customer by their phone number for customer lookup purposes during order creation and management
    private static final String FIND_BY_PHONE = """
            SELECT *
            FROM customers
            WHERE phone = ?
            """;

    @Inject
    DataSource dataSource;

    /**
     * Looks up a customer by their phone number.
     *
     * @param phone The phone number to search for (exact match).
     * @return The matching {@link team44.project2.model.Customer}, or {@code null} if
     *         no customer with that phone number exists.
     */
    public Customer findByPhone(String phone) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_PHONE)) {

            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapCustomer(rs);
            }
        } catch (Exception e) {
            Log.error("Failed to find customer by phone", e);
        }

        return null;
    }

    /**
     * Maps the current row of the given {@link java.sql.ResultSet} to a
     * {@link team44.project2.model.Customer} instance.
     *
     * @param rs An open {@code ResultSet} positioned at the row to map.
     * @return The constructed {@code Customer}.
     * @throws Exception If any SQL column access fails.
     */
    private Customer mapCustomer(ResultSet rs) throws Exception {
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getInt("reward_points"),
                rs.getDate("join_date") != null
                        ? rs.getDate("join_date").toLocalDate()
                        : null
        );
    }
}