// src/main/java/.../config/OpenAIConfig.java
package api.rest.SeasFit.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {
    @Bean
    public OpenAIClient openAIClient() {
        // Lấy key từ ENV: OPENAI_API_KEY (hoặc application.yml -> System.setProperty/openai.apiKey)
        return OpenAIOkHttpClient.fromEnv();
    }
}
