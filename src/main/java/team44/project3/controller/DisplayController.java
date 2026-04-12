package team44.project3.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import team44.project2.service.MenuService;
import team44.project2.service.OrderService;
import team44.project2.model.menu.MenuItem;

import java.util.List;
import java.util.Map;

@Path("/api/display")
@Produces(MediaType.APPLICATION_JSON)
public class DisplayController {

    @Inject
    OrderService orderService;

    @Inject
    MenuService menuService;

    @GET
    @Path("/kitchen")
    public List<Map<String, Object>> kitchen() {
        return orderService.getKitchenOrders();
    }

    @GET
    @Path("/pickup")
    public List<Map<String, Object>> pickup() {
        return orderService.getPickupOrders();
    }

    @GET
    @Path("/menu")
    public List<MenuItem> menu() {
        return menuService.getAllMenuItems();
    }
}