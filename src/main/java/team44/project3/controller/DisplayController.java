package team44.project3.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import team44.project2.service.MenuService;
import team44.project2.service.OrderService;
import team44.project2.model.menu.MenuItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.*;

@Path("/api/display")
@Produces(MediaType.APPLICATION_JSON)
public class DisplayController {

    @Inject
    MenuService menuService;

    @Inject
    DataSource dataSource;

    /*
     * Kitchen Display
     */
    @GET
    @Path("/kitchen")
    public List<Map<String, Object>> getKitchenOrders() {

        List<Map<String, Object>> orders = new ArrayList<>();

        String sql = """
            SELECT order_id, timestamp
            FROM orders
            ORDER BY timestamp DESC
            LIMIT 20
        """;

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {

                Map<String, Object> order = new HashMap<>();

                order.put("orderId", rs.getInt("order_id"));
                order.put("timestamp", rs.getTimestamp("timestamp"));

                orders.add(order);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return orders;
    }

    /*
     * Pickup Display
     */
    @GET
    @Path("/pickup")
    public List<Integer> getReadyOrders() {

        List<Integer> ready = new ArrayList<>();

        String sql = """
            SELECT order_id
            FROM orders
            ORDER BY timestamp DESC
            LIMIT 10
        """;

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {
                ready.add(rs.getInt("order_id"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ready;
    }

    /*
     * Menu Board
     */
    @GET
    @Path("/menu")
    public Map<String, List<MenuItem>> getMenuBoard() {

        Map<String, List<MenuItem>> menu = new HashMap<>();

        List<String> categories = menuService.getCategories();

        for (String category : categories) {
            menu.put(category, menuService.getItemsByCategory(category));
        }

        return menu;
    }
}