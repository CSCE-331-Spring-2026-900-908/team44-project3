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
import team44.project2.model.Employee;
import team44.project2.service.AuthService;
import team44.project2.service.SessionStore;

@Blocking
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    SessionStore sessionStore;

    public record LoginRequest(@Email(regexp = ".+@.+\\..+") @NotBlank String email, @NotBlank String password) {}
    public record LoginResponse(String token, Employee employee) {}

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest req) {
        Employee emp = authService.authenticate(req.email(), req.password());
        if (emp == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String token = sessionStore.createSession(emp);
        return Response.ok(new LoginResponse(token, emp)).build();
    }

    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            sessionStore.removeSession(authHeader.substring(7));
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/me")
    public Response me(@Context ContainerRequestContext ctx) {
        Employee emp = (Employee) ctx.getProperty("currentEmployee");
        if (emp == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(emp).build();
    }
}
