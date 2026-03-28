package team44.project2.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import team44.project2.model.menu.MenuItem;
import team44.project2.model.order.CartItem;
import team44.project2.model.order.Order;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class responsible for order submission, coordinating the persistence of the
 * order header, its individual line items, and the corresponding inventory decrements
 * all within a single database transaction.
 */
@ApplicationScoped
public class OrderService {
    //sql queries for order operations
    private static final String INSERT_ORDER = """
            INSERT INTO orders (
                employee_id, customer_id, timestamp,
                total_price, tip_amount, payment_method
            )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING order_id
            """;

    private static final String INSERT_ORDER_ITEM = """
            INSERT INTO order_items (
                order_id, menu_item_id, parent_item_id,
                quantity, ice_level, sugar_level, item_price
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING order_item_id
            """;

    private static final String DECREMENT_INVENTORY = """
            UPDATE inventory
            SET current_quantity = inventory.current_quantity - mc.quantity
            FROM menu_item_contents mc
            WHERE inventory.inventory_id = mc.inventory_id
              AND mc.menu_item_id = ?
            """;

    @Inject
    DataSource dataSource;

    /**
     * Submits an order to the database within a single transaction. Inserts the order
     * header, each main item, and each add-on, and decrements the associated inventory
     * for every line item. Rolls back automatically on any failure.
     *
     * @param employeeId    The ID of the cashier processing the order.
     * @param customerId    The loyalty customer ID, or {@code null} for a guest order.
     * @param paymentMethod The payment method string (e.g. {@code "CASH"}, {@code "CARD"}).
     * @param tipAmount     The tip amount selected by the customer.
     * @param cart          The list of cart items to persist.
     * @return The persisted {@link team44.project2.model.order.Order}, or {@code null}
     *         if the submission failed.
     */
    public Order submitOrder(
            int employeeId,
            Integer customerId,
            String paymentMethod,
            BigDecimal tipAmount,
            List<CartItem> cart
    ) {
        BigDecimal totalPrice = cart.stream()
                .map(CartItem::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int orderId = insertOrder(
                        conn, employeeId, customerId,
                        totalPrice, tipAmount, paymentMethod
                );

                for (CartItem cartItem : cart) {
                    int parentItemId = insertMainItem(conn, orderId, cartItem);
                    decrementInventory(conn, cartItem.item().menuItemId());

                    for (MenuItem addOn : cartItem.addOns()) {
                        insertAddOn(conn, orderId, parentItemId, addOn);
                        decrementInventory(conn, addOn.menuItemId());
                    }
                }

                conn.commit();

                return new Order(
                        orderId, employeeId, customerId,
                        LocalDateTime.now(), totalPrice,
                        tipAmount, paymentMethod
                );
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            Log.error("Failed to submit order", e);
            return null;
        }
    }

    /**
     * Decrements inventory quantities for all ingredients linked to the given menu item.
     *
     * @param conn       An active database connection (within the current transaction).
     * @param menuItemId The ID of the menu item whose ingredients should be decremented.
     * @throws java.sql.SQLException If the update fails.
     */
    private void decrementInventory(Connection conn, int menuItemId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(DECREMENT_INVENTORY)) {
            stmt.setInt(1, menuItemId);
            stmt.executeUpdate();
        }
    }

    /**
     * Inserts a single order header row and returns the generated order ID.
     *
     * @param conn          Active connection.
     * @param employeeId    Cashier ID.
     * @param customerId    Loyalty customer ID, or {@code null}.
     * @param totalPrice    Pre-calculated subtotal.
     * @param tipAmount     Tip selected by the customer.
     * @param paymentMethod Payment method string.
     * @return The generated {@code order_id}.
     * @throws java.sql.SQLException On any DB error.
     */
    private int insertOrder(
            Connection conn,
            int employeeId,
            Integer customerId,
            BigDecimal totalPrice,
            BigDecimal tipAmount,
            String paymentMethod
    ) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_ORDER)) {
            stmt.setInt(1, employeeId);
            if (customerId != null) {
                stmt.setInt(2, customerId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setBigDecimal(4, totalPrice);
            stmt.setBigDecimal(5, tipAmount);
            stmt.setString(6, paymentMethod);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("order_id");
        }
    }

    /**
     * Inserts the main (parent) order-item row for a cart item and returns its generated
     * {@code order_item_id} for use as the parent reference by add-on rows.
     *
     * @param conn     Active connection.
     * @param orderId  ID of the parent order.
     * @param cartItem The cart item to persist.
     * @return The generated {@code order_item_id}.
     * @throws java.sql.SQLException On any DB error.
     */
    private int insertMainItem(
            Connection conn,
            int orderId,
            CartItem cartItem
    ) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_ORDER_ITEM)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, cartItem.item().menuItemId());
            stmt.setNull(3, Types.INTEGER);
            stmt.setInt(4, 1);
            stmt.setString(5, cartItem.iceLevel());
            stmt.setString(6, cartItem.sweetness());
            stmt.setBigDecimal(7, cartItem.item().basePrice());
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("order_item_id");
        }
    }

    /**
     * Inserts a child order-item row representing an add-on that belongs to a parent
     * menu item already inserted for this order.
     *
     * @param conn         Active connection.
     * @param orderId      ID of the parent order.
     * @param parentItemId {@code order_item_id} of the parent item row.
     * @param addOn        The add-on menu item to persist.
     * @throws java.sql.SQLException On any DB error.
     */
    private void insertAddOn(
            Connection conn,
            int orderId,
            int parentItemId,
            MenuItem addOn
    ) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_ORDER_ITEM)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, addOn.menuItemId());
            stmt.setInt(3, parentItemId);
            stmt.setInt(4, 1);
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setBigDecimal(7, addOn.basePrice());
            stmt.executeQuery();
        }
    }
}