package api.rest.SeasFit.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {
    @Bean
    public OpenAIClient openAIClient() {
        // Lấy OPENAI_API_KEY từ env (System.getenv)
        return OpenAIOkHttpClient.fromEnv();
        // Hoặc:
        // return OpenAIOkHttpClient.builder().apiKey(System.getenv("OPENAI_API_KEY")).build();
    }
}
