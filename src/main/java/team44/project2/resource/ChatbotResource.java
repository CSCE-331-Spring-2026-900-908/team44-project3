package team44.project2.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import team44.project2.service.ChatbotService;


//handles Chatbot interactions and receives User input and chatbot response
@Path("/api/chatbot")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatbotResource {


    public static record ChatRequest(String prompt, java.util.List<HistoryEntry> history) {}

    public static record HistoryEntry(String from, String text) {}

    
    //records chatbot responses and any error messages
    public static record ChatResponse(String text, String detail) {}

    @Inject
    ChatbotService chatbotService;

    
    //handles POST requests to generate a chatbot reply
    @POST
    public Response chat(ChatRequest req) {
        if (req == null || req.prompt == null || req.prompt().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ChatResponse("Invalid input.", null))
                    .build();
        }

        try {
            String reply = chatbotService.generateReply(req.prompt(), req.history());
            return Response.ok(new ChatResponse(reply, null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ChatResponse("Boba Bob is currently offline!", e.getMessage()))
                    .build();
        }
    }
}
