package team44.project2.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import team44.project2.model.Employee;
import team44.project2.service.SessionStore;

@Provider
public class AuthFilter implements ContainerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Inject
    SessionStore sessionStore;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();
        String method = ctx.getMethod();

        if (isPublic(path, method)) return;

        String authHeader = ctx.getHeaderString(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            ctx.abortWith(
                Response.status(Response.Status.UNAUTHORIZED).build()
            );
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        Employee employee = sessionStore.getSession(token);
        if (employee == null) {
            ctx.abortWith(
                Response.status(Response.Status.UNAUTHORIZED).build()
            );
            return;
        }

        ctx.setProperty("currentEmployee", employee);
    }

    private boolean isPublic(String path, String method) {
        if (!path.startsWith("api/")) return true;

        if (path.equals("api/auth/login")) return true;

        if (path.startsWith("api/menu/")) return true;
        if (path.equals("api/customers/lookup")) return true;
        if (path.equals("api/customers/checkin") && "POST".equals(method)) return true;
        if (path.equals("api/orders") && "POST".equals(method)) return true;

        return false;
    }
}
