package api.rest.SeasFit.component;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

import java.util.Map;

@Component
public class OllamaClient {

    private final WebClient client = WebClient.builder()
            .baseUrl("http://localhost:11434")
            .build();

    public String callOllama(String prompt) {
        Map<String, Object> request = Map.of(
                "model", "gemma",
                "prompt", prompt,
                "stream", false
        );

        try {
            Map<String, Object> response = client.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return response.get("response").toString();
        } catch (Exception e) {
            return "{\"intent\": \"unknown\"}";
        }
    }
}
