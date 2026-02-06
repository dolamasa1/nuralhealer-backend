package com.neuralhealer.backend.controller;

import com.neuralhealer.backend.model.dto.AiChatRequest;
import com.neuralhealer.backend.model.dto.AiChatResponse;
import com.neuralhealer.backend.model.dto.WebSocketMessage;
import com.neuralhealer.backend.model.enums.WebSocketMessageType;
import com.neuralhealer.backend.notification.service.NotificationCreatorService;
import com.neuralhealer.backend.service.AiChatbotService;
import com.neuralhealer.backend.service.ChatStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * STOMP Controller for AI Chatbot.
 * Handles AI requests over the standard WebSocket broker.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AiStompController {

    private final AiChatbotService aiChatbotService;
    private final ChatStorageService chatStorageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationCreatorService notificationCreatorService;

    /**
     * Handle AI questions via STOMP.
     * Destination: /app/ai/ask
     * Responses sent to: /user/queue/ai
     */
    @MessageMapping("/ai/ask")
    public void askAi(@Payload AiChatRequest request, SimpMessageHeaderAccessor headerAccessor,
            Authentication authentication) {
        String wsSessionId = headerAccessor.getSessionId();

        // Extract userId if available from authentication
        UUID userId = null;
        if (authentication != null
                && authentication.getPrincipal() instanceof com.neuralhealer.backend.model.entity.User) {
            userId = ((com.neuralhealer.backend.model.entity.User) authentication.getPrincipal()).getId();
        }

        log.info("STOMP AI request received: session={}, user={}", wsSessionId, userId);

        if (!aiChatbotService.isAiAvailable()) {
            sendAiMessage(headerAccessor, WebSocketMessageType.AI_ERROR, "الذكاء الاصطناعي غير متاح حالياً");
            return;
        }

        // 1. Get/Create Persistent Chat Session & Save User Message
        UUID chatSessionId = null;
        if (userId != null) {
            chatSessionId = chatStorageService.getOrCreateSession(userId);
            chatStorageService.saveMessage(chatSessionId, "patient", request.question());
        }

        // 2. Send TYPING_START (as step 2 in original flow, but conceptually step 1 in
        // UI feedback)
        sendAiMessage(headerAccessor, WebSocketMessageType.AI_TYPING_START, "المساعد الذكي يكتب...");

        try {
            // 2. Call AI Service
            AiChatResponse response = aiChatbotService.askQuestion(request.question());

            // Clean response (Remove Arabic prefixes)
            String cleanAnswer = response.answer();
            if (cleanAnswer != null) {
                cleanAnswer = cleanAnswer.replaceAll("^[\\s\\n]*(الإجابة|الأجابة|الرد|الإجابة هي)[:\\-]?\\s*", "");
            }

            // 3. Send TYPING_STOP
            sendAiMessage(headerAccessor, WebSocketMessageType.AI_TYPING_STOP, null);

            // 4. Send RESPONSE
            sendAiMessage(headerAccessor, WebSocketMessageType.AI_RESPONSE, cleanAnswer);

            // 5. Save AI Response
            if (chatSessionId != null) {
                chatStorageService.saveMessage(chatSessionId, "ai", cleanAnswer);
            }

            // 6. Trigger persistent notification if user is logged in
            if (userId != null) {
                notificationCreatorService.createAiNotification(
                        userId,
                        "AI Analysis Ready",
                        "Your smart medical assistant has provided a response.",
                        null // No persistent AI interaction ID yet, using null
                );
            }

        } catch (Exception e) {
            log.error("AI Error in STOMP: {}", e.getMessage());
            sendAiMessage(headerAccessor, WebSocketMessageType.AI_TYPING_STOP, null);
            sendAiMessage(headerAccessor, WebSocketMessageType.AI_ERROR,
                    "عذراً، حدث خطأ أثناء الاتصال بالذكاء الاصطناعي.");
        }
    }

    private void sendAiMessage(SimpMessageHeaderAccessor headerAccessor, WebSocketMessageType type, String content) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(type)
                .senderName("AI Assistant")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();

        // Use convertAndSendToUser to send directly back to the requesting session
        messagingTemplate.convertAndSendToUser(
                headerAccessor.getSessionId(),
                "/queue/ai",
                wsMessage,
                Map.of(SimpMessageHeaderAccessor.SESSION_ID_HEADER, headerAccessor.getSessionId()));
    }
}
