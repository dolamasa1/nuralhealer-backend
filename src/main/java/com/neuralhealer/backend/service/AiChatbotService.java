package com.neuralhealer.backend.service;

import com.neuralhealer.backend.model.dto.AiChatRequest;
import com.neuralhealer.backend.model.dto.AiChatResponse;
import com.neuralhealer.backend.model.dto.AiHealthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * Service for communicating with external AI Chatbot API.
 * Handles health checks, question/answer flow, and error handling.
 */
@Service
@Slf4j
public class AiChatbotService {

    private final RestTemplate aiRestTemplate;
    private final String baseUrl;
    private final String healthEndpoint;
    private final String askEndpoint;
    private final String apiKey;
    private final String ngrokSkipHeader;

    private LocalDateTime lastHealthCheck;
    private boolean lastHealthStatus = false;

    public AiChatbotService(
            RestTemplate aiRestTemplate,
            @Value("${ai.chatbot.base-url}") String baseUrl,
            @Value("${ai.chatbot.health-endpoint:/health}") String healthEndpoint,
            @Value("${ai.chatbot.ask-endpoint:/ask}") String askEndpoint,
            @Value("${ai.chatbot.api-key:}") String apiKey,
            @Value("${ai.chatbot.ngrok-skip-browser-warning:false}") String ngrokSkipHeader) {
        this.aiRestTemplate = aiRestTemplate;
        this.baseUrl = baseUrl;
        this.healthEndpoint = healthEndpoint;
        this.askEndpoint = askEndpoint;
        this.apiKey = apiKey;
        this.ngrokSkipHeader = ngrokSkipHeader;
    }

    /**
     * Check AI API health status.
     * Result is cached for 1 minute to avoid hammering the AI service.
     */
    @Cacheable(value = "aiHealthCache", unless = "#result == null")
    public AiHealthResponse checkHealth() {
        try {
            String url = baseUrl + healthEndpoint;
            log.info("Checking AI health at: {}", url);

            ResponseEntity<String> response = aiRestTemplate.getForEntity(url, String.class);

            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            lastHealthStatus = isHealthy;
            lastHealthCheck = LocalDateTime.now();

            String message = isHealthy ? "AI connected" : "AI connection issue";
            log.info("AI health check: {}", message);

            return new AiHealthResponse(isHealthy, message, lastHealthCheck);

        } catch (RestClientException e) {
            log.error("AI health check failed: {}", e.getMessage());
            lastHealthStatus = false;
            lastHealthCheck = LocalDateTime.now();
            return new AiHealthResponse(false, "AI not connected: " + e.getMessage(), lastHealthCheck);
        }
    }

    /**
     * Quick check if AI is available based on cached health status.
     */
    public boolean isAiAvailable() {
        if (lastHealthCheck == null) {
            // First time check
            checkHealth();
        }
        return lastHealthStatus;
    }

    /**
     * Send a question to AI and get response.
     * Handles Arabic text with proper UTF-8 encoding.
     *
     * @param question The question to ask (supports Arabic)
     * @return AI response with answer and sources
     * @throws RestClientException if AI is unavailable or request fails
     */
    public AiChatResponse askQuestion(String question) {
        try {
            String url = baseUrl + askEndpoint;
            log.info("Sending question to AI: {} (length: {} chars)",
                    question.substring(0, Math.min(50, question.length())), question.length());

            // Create request with proper headers for Arabic text and API Key
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept-Charset", "UTF-8");
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("x-api-key", apiKey);
            }
            if ("true".equalsIgnoreCase(ngrokSkipHeader)) {
                headers.set("ngrok-skip-browser-warning", "true");
            }

            AiChatRequest requestBody = new AiChatRequest(question, "egypt");
            HttpEntity<AiChatRequest> entity = new HttpEntity<>(requestBody, headers);

            // Send POST request
            ResponseEntity<AiChatResponse> response = aiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AiChatResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AiChatResponse aiResponse = response.getBody();
                log.info("AI response received: answer length={}",
                        aiResponse.answer() != null ? aiResponse.answer().length() : 0);
                return aiResponse;
            } else {
                throw new RestClientException("AI returned non-successful status: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            log.error("Error calling AI API: {}", e.getMessage(), e);
            throw new RestClientException("AI request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get the last health check time.
     */
    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }
}
