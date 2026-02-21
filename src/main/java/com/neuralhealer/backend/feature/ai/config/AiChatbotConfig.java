ackage com.neuralhealer.backend.feature.ai.config.AiChatbotConfig;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Configuration for AI Chatbot integration.
 * Configures RestTemplate with proper timeouts and UTF-8 encoding for Arabic
 * text.
 */
@Configuration
public class AiChatbotConfig {

    /**
     * RestTemplate bean for AI API communication.
     * - Connection timeout: 5 seconds
     * - Read timeout: Configurable (default 90s)
     * - UTF-8 encoding for Arabic text support
     */
    @Bean
    public RestTemplate aiRestTemplate(
            RestTemplateBuilder builder,
            @org.springframework.beans.factory.annotation.Value("${ai.chatbot.timeout-seconds:300}") int timeoutSeconds) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .additionalMessageConverters(
                        new StringHttpMessageConverter(
                                java.util.Objects.requireNonNull(StandardCharsets.UTF_8)))
                .build();
    }
}
