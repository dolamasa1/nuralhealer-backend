package com.neuralhealer.backend.controller;

import com.neuralhealer.backend.model.dto.AiChatRequest;
import com.neuralhealer.backend.model.dto.AiChatResponse;
import com.neuralhealer.backend.model.dto.WebSocketMessage;

import com.neuralhealer.backend.model.enums.WebSocketMessageType;
import com.neuralhealer.backend.service.AiChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AiWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final AiChatbotService aiChatbotService;

    /**
     * Handle standalone AI chatbot questions
     * Client sends to: /app/ai/ask
     * Response sent to: /user/queue/ai (user-specific queue)
     */
    @MessageMapping("/ai/ask")
    public void handleAiQuestion(
            @Payload AiChatRequest request,
            java.security.Principal principal) {

        if (principal == null) {
            log.warn("❌ Unauthenticated AI WebSocket request received at /app/ai/ask");
            return;
        }

        String userId = principal.getName();
        log.info("✅ AI question received from user: {}", userId);
        log.info("Payload: {}", request);

        try {
            // 1. Send AI_TYPING_START to user
            sendToUser(userId, WebSocketMessageType.AI_TYPING_START, "AI Assistant", "المساعد الذكي يكتب...", null);

            try {
                // 2. Call AI API
                AiChatResponse aiResponse = aiChatbotService.askQuestion(request.question());

                // 3. Send AI_TYPING_STOP
                sendToUser(userId, WebSocketMessageType.AI_TYPING_STOP, "AI Assistant", null, null);

                // 4. Send AI_RESPONSE
                sendToUser(userId, WebSocketMessageType.AI_RESPONSE, "AI Assistant", aiResponse.answer(), aiResponse);

            } catch (Exception aiException) {
                // AI call failed - send AI_TYPING_STOP first
                sendToUser(userId, WebSocketMessageType.AI_TYPING_STOP, "AI Assistant", null, null);

                log.error("AI request failed for user {}: {}", userId, aiException.getMessage());

                String errorMessage = aiException.getMessage() != null && aiException.getMessage().contains("timeout")
                        ? "الذكاء الاصطناعي يستغرق وقتاً طويلاً. حاول مرة أخرى."
                        : "الذكاء الاصطناعي غير متاح حالياً. يرجى المحاولة لاحقاً.";

                sendToUser(userId, WebSocketMessageType.AI_ERROR, "System", errorMessage, aiException.getMessage());
            }

        } catch (Exception e) {
            log.error("Error handling AI WebSocket request for user {}", userId, e);
            sendError(userId, "حدث خطأ غير متوقع");
        }
    }

    private void sendToUser(String userId, WebSocketMessageType type, String senderName, String content,
            Object metadata) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type(type)
                .senderId(null)
                .senderName(senderName)
                .content(content)
                .timestamp(LocalDateTime.now())
                .metadata(metadata)
                .build();

        messagingTemplate.convertAndSendToUser(userId, "/queue/ai", message);
    }

    private void sendError(String userId, String content) {
        try {
            sendToUser(userId, WebSocketMessageType.AI_ERROR, "System", content, null);
        } catch (Exception e) {
            log.error("Failed to send error message to user", e);
        }
    }
}
