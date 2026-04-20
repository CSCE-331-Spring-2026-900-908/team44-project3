package team44.project2.service;

import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ChatbotService {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash-lite";
    private static final String DEFAULT_SYSTEM_INSTRUCTION = "You are a friendly boba shop assistant named Boba Bob.";

    public String generateReply(String prompt) {
        String apiKey = "AIzaSyAyn7nflsvW0BhEf0DrCirqk7j_xgiXldM";
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("GENAI_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing GEMINI_API_KEY or GENAI_API_KEY environment variable");
        }

        String model = System.getenv().getOrDefault("GENAI_MODEL", DEFAULT_MODEL);
        String systemInstruction = System.getenv().getOrDefault("GENAI_SYSTEM_INSTRUCTION", DEFAULT_SYSTEM_INSTRUCTION);
        String effectivePrompt = systemInstruction + "\n\nUser: " + prompt;

        Client client = Client.builder().apiKey(apiKey).build();

        List<Content> contents = List.of(
                Content.builder()
                        .role("user")
                        .parts(List.of(
                                Part.fromText(effectivePrompt)
                        ))
                        .build()
        );

        GenerateContentConfig config = GenerateContentConfig.builder()
                .thinkingConfig(
                        ThinkingConfig.builder()
                                .thinkingBudget(0)
                                .build()
                )
                .build();

        ResponseStream<GenerateContentResponse> responseStream = client.models.generateContentStream(model, contents, config);

        StringBuilder outputText = new StringBuilder();

        for (GenerateContentResponse res : responseStream) {
            if (res.candidates().isEmpty() || res.candidates().get().get(0).content().isEmpty() || res.candidates().get().get(0).content().get().parts().isEmpty()) {
                continue;
            }

            List<Part> parts = res.candidates().get().get(0).content().get().parts().get();
            for (Part part : parts) {
                outputText.append(part.text());
            }
        }

        responseStream.close();

        return outputText.toString().trim();
    }
}
