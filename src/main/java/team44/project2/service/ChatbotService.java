package team44.project2.service;

import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
//import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;

@ApplicationScoped
public class ChatbotService {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash-lite";
    private static final String DEFAULT_SYSTEM_INSTRUCTION = """
            You are Boba Bob. You are the personal assistant for Boba Bob's Boba Store. You are positive and happy, and you love to help people. People will ask you questions, and you will answer them to the best of your ability based on the below menu items.

            menu_item_id,name,category,size,base_price,is_available
            1,classic pearl milk tea,milky series,regular,5.50,true
            2,honey pearl milk tea,milky series,regular,5.75,true
            3,coffee creama,milky series,regular,6.00,true
            4,coffee milk tea w/ coffee jelly,milky series,regular,6.25,true
            5,hokkaido pearl milk tea,milky series,regular,5.75,true
            6,thai pearl milk tea,milky series,regular,5.75,true
            7,taro pearl milk tea,milky series,regular,5.75,true
            8,mango green milk tea,milky series,regular,5.75,true
            9,golden retriever,milky series,regular,6.00,true
            10,coconut pearl milk tea,milky series,regular,6.00,true
            11,classic tea,fresh brew,regular,4.50,true
            12,honey tea,fresh brew,regular,4.75,true
            13,mango green tea,fruity beverage,regular,5.25,true
            14,passion chess,fruity beverage,regular,5.50,true
            15,berry lychee burst,fruity beverage,regular,5.50,true
            16,peach tea w/ honey jelly,fruity beverage,regular,5.75,true
            17,mango & passion fruit tea,fruity beverage,regular,5.50,true
            18,honey lemonade,fruity beverage,regular,5.25,true
            19,tiger boba,non-caffeinated,regular,6.00,true
            20,strawberry coconut,non-caffeinated,regular,6.00,true
            21,strawberry coconut ice blended,non-caffeinated,regular,6.50,true
            22,halo halo,non-caffeinated,regular,6.25,true
            23,halo halo ice blended,non-caffeinated,regular,6.75,true
            24,wintermelon lemonade,non-caffeinated,regular,5.50,true
            25,wintermelon lemonade ice blended,non-caffeinated,regular,6.00,true
            26,wintermelon w/ fresh milk,non-caffeinated,regular,5.75,true
            27,matcha pearl milk tea,new matcha series,regular,6.25,true
            28,matcha fresh milk,new matcha series,regular,6.00,true
            29,strawberry matcha fresh milk,new matcha series,regular,6.50,true
            30,mango matcha fresh milk,new matcha series,regular,6.50,true
            31,matcha ice blended,new matcha series,regular,6.75,true
            32,oreo w/ pearl,ice-blended,regular,6.50,true
            33,taro w/ pudding,ice-blended,regular,6.50,true
            34,thai tea w/ pearl,ice-blended,regular,6.50,true
            35,coffee w/ ice cream,ice-blended,regular,6.75,true
            36,mango w/ ice cream,ice-blended,regular,6.75,true
            37,strawberry w/ lychee jelly & ice cream,ice-blended,regular,7.00,true
            38,peach tea w/ lychee jelly,ice-blended,regular,6.50,true
            39,lava flow,ice-blended,regular,6.75,true
            40,pearls (boba),topping,,0.75,true
            41,lychee jelly,topping,,0.75,true
            42,crystal boba,topping,,0.75,true
            43,ice cream,topping,,1.25,true
            44,coffee jelly,topping,,0.75,true
            45,honey jelly,topping,,0.75,true
            46,mango popping boba,topping,,0.75,true
            47,creama,topping,,1.00,true
            48,pudding,topping,,0.75,true
            49,strawberry popping boba,topping,,0.75,true
            """;

    @Inject
    @ConfigProperty(name = "GEMINI_API_KEY", defaultValue = "")
    String geminiApiKey;

    public String generateReply(String prompt) {
        String apiKey = geminiApiKey;
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
