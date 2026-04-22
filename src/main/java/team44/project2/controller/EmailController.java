package team44.project2.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import team44.project2.dto.ReceiptEmailRequest;
import team44.project2.service.EmailService;

@Path("/email")
public class EmailController {

    @Inject
    EmailService emailService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendEmail(ReceiptEmailRequest request) {
        try {
            boolean sent = emailService.sendEmail(request);

            if (!sent) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Receipt already sent for this order")
                        .build();
            }

            return Response.ok("Receipt sent").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Failed to send receipt").build();
        }
    }
}