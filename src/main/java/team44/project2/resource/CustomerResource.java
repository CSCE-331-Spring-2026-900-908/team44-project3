package team44.project2.resource;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import team44.project2.model.Customer;
import team44.project2.service.CustomerService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Blocking
@Path("/api/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000;
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<Long>> rateLimitMap =
            new ConcurrentHashMap<>();

    @Inject
    CustomerService customerService;

    @GET
    @Path("/lookup")
    public Response lookup(@QueryParam("phone") String phone) {
        Customer customer = customerService.findByPhone(phone);
        if (customer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(customer).build();
    }

    public record CheckinRequest(@Email(regexp = ".+@.+\\..+") @NotBlank String email) {}

    @POST
    @Path("/checkin")
    public Response checkin(@Valid CheckinRequest req, @Context ContainerRequestContext ctx) {
        String ip = extractIp(ctx);
        if (!allowRequest(ip)) {
            return Response.status(429).build();
        }

        Customer customer = customerService.findOrCreateByEmail(req.email().trim());
        if (customer == null) {
            return Response.serverError().build();
        }
        return Response.ok(customer).build();
    }

    private boolean allowRequest(String ip) {
        long now = System.currentTimeMillis();
        CopyOnWriteArrayList<Long> timestamps = rateLimitMap.computeIfAbsent(ip, k -> new CopyOnWriteArrayList<>());
        timestamps.removeIf(t -> now - t > WINDOW_MS);
        if (timestamps.size() >= MAX_REQUESTS) {
            return false;
        }
        timestamps.add(now);
        return true;
    }

    private String extractIp(ContainerRequestContext ctx) {
        String forwarded = ctx.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return "unknown";
    }
}
