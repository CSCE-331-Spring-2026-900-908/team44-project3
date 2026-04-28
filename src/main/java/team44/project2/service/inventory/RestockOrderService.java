package team44.project2.service.inventory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;

import team44.project2.model.RestockOrder;
import io.quarkus.logging.Log;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class responsible for managing restock orders, providing methods to create
 * new orders when inventory runs low and retrieve the full order history.
 */
@ApplicationScoped
public class RestockOrderService {

    @Inject
    DataSource dataSource;
    //sql queries for restock order operations
    private static final String CREATE_ORDER = """
        INSERT INTO restock_orders (employee_id, inventory_id, quantity_ordered, order_date, status)
        VALUES (?, ?, ?, ?, ?)
        RETURNING restock_id
        """;

    private static final String APPLY_RESTOCK_TO_INVENTORY = """
        UPDATE inventory
        SET current_quantity = COALESCE(current_quantity, 0) + ?,
            last_restock_date = CURRENT_DATE
        WHERE inventory_id = ?
        """;

    private static final String GET_ALL_ORDERS = """
        SELECT restock_id, employee_id, inventory_id, quantity_ordered, order_date, status
        FROM restock_orders
        ORDER BY order_date DESC
        """;

    /**
     * Creates a new restock order and persists it to the database.
     *
     * @param employeeId  The employee placing the order.
     * @param inventoryId The inventory item to restock.
     * @param quantity    The quantity to order.
     * @param orderDate   The timestamp of the order.
     * @param status      The initial status (e.g. {@code "PENDING"}).
     * @return The newly created {@link team44.project2.model.RestockOrder}, or
     *         {@code null} if the insert failed.
     */
    public RestockOrder createRestockOrder(int employeeId, int inventoryId, BigDecimal quantity, LocalDateTime orderDate, String status) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            int restockId;
            try (PreparedStatement ps = conn.prepareStatement(CREATE_ORDER)) {
                ps.setInt(1, employeeId);
                ps.setInt(2, inventoryId);
                ps.setBigDecimal(3, quantity);
                ps.setTimestamp(4, Timestamp.valueOf(orderDate));
                ps.setString(5, status);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return null;
                    }
                    restockId = rs.getInt("restock_id");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(APPLY_RESTOCK_TO_INVENTORY)) {
                ps.setBigDecimal(1, quantity);
                ps.setInt(2, inventoryId);
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    return null;
                }
            }

            conn.commit();
            return new RestockOrder(restockId, employeeId, inventoryId, quantity, orderDate, status);

        } catch (SQLException e) {
            Log.error("Error creating restock order", e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignore) {}
            }
            return null;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignore) {}
            }
        }
    }

    /**
     * Returns all restock orders from the database, ordered by descending order date.
     *
     * @return A list of all {@link team44.project2.model.RestockOrder} records; empty if
     *         none exist or a database error occurs.
     */
    public List<RestockOrder> getAllOrders() {
        List<RestockOrder> orders = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_ORDERS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                orders.add(mapOrder(rs));
            }

        } catch (SQLException e) {
            Log.error("Error fetching restock orders", e);
        }

        return orders;
    }

    /**
     * Maps the current row of the given {@link java.sql.ResultSet} to a
     * {@link team44.project2.model.RestockOrder} instance.
     *
     * @param rs An open {@code ResultSet} positioned at the row to map.
     * @return The constructed {@code RestockOrder}.
     * @throws java.sql.SQLException If any column access fails.
     */
    private RestockOrder mapOrder(ResultSet rs) throws SQLException {
        java.sql.Timestamp sqlTimestamp = rs.getTimestamp("order_date");
        java.time.LocalDateTime orderDate = (sqlTimestamp != null) ? sqlTimestamp.toLocalDateTime() : null;

        return new RestockOrder(
                rs.getInt("restock_id"),
                rs.getInt("employee_id"),
                rs.getInt("inventory_id"),
                rs.getBigDecimal("quantity_ordered"),
                orderDate,
                rs.getString("status")
        );
    }
}
