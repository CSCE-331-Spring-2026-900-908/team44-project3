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
    private static final String FIND_BY_PHONE = """
            SELECT *
            FROM customers
            WHERE phone = ?
            """;

    private static final String FIND_BY_EMAIL = """
            SELECT *
            FROM customers
            WHERE LOWER(email) = LOWER(?)
            ORDER BY customer_id ASC
            LIMIT 1
            """;

    private static final String INSERT_CUSTOMER_BY_EMAIL = """
            INSERT INTO customers (first_name, last_name, phone, email, reward_points, join_date)
            VALUES ('', '', NULL, ?, 0, CURRENT_DATE)
            RETURNING *
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
    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public Customer findByPhone(String phone) {
        phone = phone == null ? "" : phone.trim();
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

    public Customer findByEmail(String email) {
        email = normalizeEmail(email);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_EMAIL)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapCustomer(rs);
            }
        } catch (Exception e) {
            Log.error("Failed to find customer by email", e);
        }

        return null;
    }

    public Customer findOrCreateByEmail(String email) {
        email = normalizeEmail(email);
        Customer existing = findByEmail(email);
        if (existing != null) return existing;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_CUSTOMER_BY_EMAIL)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapCustomer(rs);
            }
        } catch (Exception e) {
            Log.error("Failed to create customer by email", e);
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