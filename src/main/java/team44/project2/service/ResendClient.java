package team44.project2.service;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "resend-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ResendClient {

    @POST
    @Path("/emails")
    Uni<SendResponse> sendEmail(@HeaderParam("Authorization") String authorization, SendRequest request);

    record SendRequest(String from, List<String> to, String subject, String html) {}

    record SendResponse(String id) {}
}
