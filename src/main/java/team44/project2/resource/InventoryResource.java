package team44.project2.resource;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import team44.project2.model.Inventory;
import team44.project2.model.RestockOrder;
import team44.project2.service.MenuService;
import team44.project2.service.inventory.InventoryService;
import team44.project2.service.inventory.RestockOrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Blocking
@Path("/api/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @Inject
    InventoryService inventoryService;

    @Inject
    RestockOrderService restockOrderService;

    @Inject
    MenuService menuService;

    @GET
    public List<Inventory> getAll() {
        return inventoryService.getAllInventory();
    }

    @POST
    public Response add(Inventory item) {
        inventoryService.addInventoryItem(item);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}/stock")
    public Response updateStock(@PathParam("id") int id, StockUpdate update) {
        inventoryService.updateStock(id, update.quantity());
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        inventoryService.deleteInventoryItem(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/names")
    public List<String> getNames() {
        return menuService.getAllInventoryNames();
    }

    @POST
    @Path("/get-or-add")
    public Response getOrAdd(GetOrAddRequest req) {
        int id = menuService.getOrAddInventoryItem(
                req.name(), req.category(), req.currentQuantity(),
                req.unit(), req.reorderThreshold(), req.supplierCost()
        );
        return Response.ok(id).build();
    }

    @POST
    @Path("/restock")
    public Response createRestock(RestockRequest req) {
        RestockOrder order = restockOrderService.createRestockOrder(
                req.employeeId(), req.inventoryId(),
                BigDecimal.valueOf(req.quantity()),
                LocalDateTime.now(), "PENDING"
        );
        if (order == null) {
            return Response.serverError().build();
        }
        return Response.status(Response.Status.CREATED).entity(order).build();
    }

    public record StockUpdate(BigDecimal quantity) {}
    public record RestockRequest(int employeeId, int inventoryId, int quantity) {}
    public record GetOrAddRequest(String name, String category, BigDecimal currentQuantity,
                                  String unit, BigDecimal reorderThreshold, BigDecimal supplierCost) {}
}
