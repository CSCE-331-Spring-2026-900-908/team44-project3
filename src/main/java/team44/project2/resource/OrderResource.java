package team44.project2.resource;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import team44.project2.model.order.CartItem;
import team44.project2.model.order.Order;
import team44.project2.service.OrderService;

import java.math.BigDecimal;
import java.util.List;

@Blocking
@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderService orderService;

    public record OrderRequest(
            Integer employeeId,
            Integer customerId,
            String paymentMethod,
            BigDecimal tipAmount,
            List<CartItem> cart,
            List<Integer> redeemedIndices
    ) {}

    @POST
    public Response submit(OrderRequest req) {
        Order order = orderService.submitOrder(
                req.employeeId(), req.customerId(),
                req.paymentMethod(), req.tipAmount(), req.cart(),
                req.redeemedIndices() != null ? req.redeemedIndices() : List.of()
        );
        if (order == null) {
            return Response.serverError().build();
        }
        return Response.status(Response.Status.CREATED).entity(order).build();
    }
}
