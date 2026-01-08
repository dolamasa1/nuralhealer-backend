package com.neuralhealer.backend.controller;

import com.neuralhealer.backend.model.dto.AiChatRequest;
import com.neuralhealer.backend.model.dto.AiChatResponse;
import com.neuralhealer.backend.model.dto.AiHealthResponse;
import com.neuralhealer.backend.service.AiChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;

/**
 * REST Controller for AI Chatbot.
 * Provides health check and direct question endpoints.
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatbotController {

    private final AiChatbotService aiChatbotService;

    /**
     * Check AI health status.
     * GET /api/ai/health
     */
    @GetMapping("/health")
    public ResponseEntity<AiHealthResponse> checkHealth() {
        try {
            AiHealthResponse health = aiChatbotService.checkHealth();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Health check endpoint error: {}", e.getMessage());
            return ResponseEntity.ok(new AiHealthResponse(
                    false,
                    "خطأ في فحص حالة الذكاء الاصطناعي", // "Error checking AI status" in Arabic
                    LocalDateTime.now()));
        }
    }

    /**
     * Ask AI a question (REST endpoint).
     * POST /api/ai/ask
     * Body: {"question": "السؤال هنا"}
     */
    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@Valid @RequestBody AiChatRequest request) {
        try {
            log.info("REST AI request received");
            AiChatResponse response = aiChatbotService.askQuestion(request.question());
            return ResponseEntity.ok(response);
        } catch (RestClientException e) {
            log.error("AI request failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("الذكاء الاصطناعي غير متاح حالياً")); // "AI currently unavailable" in
                                                                                  // Arabic
        } catch (Exception e) {
            log.error("Unexpected error in AI request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("حدث خطأ أثناء معالجة السؤال")); // "Error processing question" in Arabic
        }
    }

    /**
     * Simple error response record.
     */
    private record ErrorResponse(String error) {
    }
}
