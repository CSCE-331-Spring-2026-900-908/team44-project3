package team44.project2.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import team44.project2.model.Employee;
import team44.project2.service.AuthService;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    public record LoginRequest(String email, String password) {}

    @POST
    @Path("/login")
    public Response login(LoginRequest req) {
        Employee emp = authService.authenticate(req.email(), req.password());
        if (emp == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(emp).build();
    }
}
