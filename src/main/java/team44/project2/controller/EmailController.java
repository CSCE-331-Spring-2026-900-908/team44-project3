package team44.project2.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import team44.project2.service.EmailService;

@Path("/email")
public class EmailController {

    @Inject
    EmailService emailService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String send() {
        emailService.sendEmail();
        return "Email sent";
    }
}