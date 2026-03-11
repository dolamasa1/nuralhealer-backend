package com.neuralhealer.backend.feature.ai.controller;

import com.neuralhealer.backend.feature.ai.dto.AiChatRequest;
import com.neuralhealer.backend.feature.ai.dto.AiChatResponse;
import com.neuralhealer.backend.shared.websocket.WebSocketMessage;
import com.neuralhealer.backend.shared.websocket.WebSocketMessageType;
import com.neuralhealer.backend.feature.notification.service.NotificationCreatorService;
import com.neuralhealer.backend.feature.ai.service.AiChatbotService;
import com.neuralhealer.backend.feature.ai.service.ChatStorageService;
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
        String requestedSessionId = headerAccessor.getFirstNativeHeader("sessionId");
        String forceNewSessionHeader = headerAccessor.getFirstNativeHeader("forceNewSession");
        boolean forceNewSession = "true".equalsIgnoreCase(forceNewSessionHeader);

        // Extract userId if available from authentication
        UUID userId = null;
        if (authentication != null
                && authentication.getPrincipal() instanceof com.neuralhealer.backend.shared.entity.User) {
            userId = ((com.neuralhealer.backend.shared.entity.User) authentication.getPrincipal()).getId();
        }

        log.debug("STOMP AI request received: session={}, user={}", wsSessionId, userId);

        if (!aiChatbotService.isAiAvailable()) {
            sendAiMessage(headerAccessor, WebSocketMessageType.AI_ERROR, "الذكاء الاصطناعي غير متاح حالياً", null);
            return;
        }

        // 1. Resolve/Create Persistent Chat Session & Save User Message
        UUID chatSessionId = null;
        if (userId != null
                && authentication.getPrincipal() instanceof com.neuralhealer.backend.shared.entity.User user) {
            UUID patientId = user.getPatientProfile() != null ? user.getPatientProfile().getId() : null;

            if (patientId != null) {
                if (forceNewSession) {
                    chatSessionId = chatStorageService.createNewSession(patientId);
                } else if (requestedSessionId != null && !requestedSessionId.isBlank()) {
                    try {
                        UUID requestedSessionUuid = UUID.fromString(requestedSessionId);
                        if (chatStorageService.sessionBelongsToPatient(requestedSessionUuid, patientId)) {
                            chatSessionId = requestedSessionUuid;
                        } else {
                            log.warn("Session {} does not belong to patient {} - creating new session", requestedSessionUuid,
                                    patientId);
                            chatSessionId = chatStorageService.createNewSession(patientId);
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid sessionId '{}' from WS session {} - creating new session", requestedSessionId,
                                wsSessionId);
                        chatSessionId = chatStorageService.createNewSession(patientId);
                    }
                } else {
                    chatSessionId = chatStorageService.getOrCreateSession(patientId);
                }

                chatStorageService.saveMessage(chatSessionId, "patient", request.question());
            } else {
                log.warn("User {} has no patient profile - skipping message persistence", userId);
            }
        }

        // 2. Send TYPING_START (as step 2 in original flow, but conceptually step 1 in
        // UI feedback)
        sendAiMessage(headerAccessor, WebSocketMessageType.AI_TYPING_START, "المساعد الذكي يكتب...", chatSessionId);

        try {
            // 2. Call AI Service
            AiChatResponse response = aiChatbotService.askQuestion(request.question());

            // Clean response (Remove Arabic prefixes)
            String cleanAnswer = response.answer();
            if (cleanAnswer != null) {
                cleanAnswer = cleanAnswer.replaceAll("^[\\s\\n]*(الإجابة|الأجابة|الرد|الإجابة هي)[:\\-]?\\s*", "");
            }

            // 3. Send TYPING_STOP
            sendAiMessage(headerAccessor, WebSocketMessageType.AI_TYPING_STOP, null, chatSessionId);

            // 4. Send RESPONSE
            sendAiMessage(headerAccessor, WebSocketMessageType.AI_RESPONSE, cleanAnswer, chatSessionId);

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
            sendAiMessage(headerAccessor, WebSocketMessageType.AI_TYPING_STOP, null, chatSessionId);
            sendAiMessage(headerAccessor, WebSocketMessageType.AI_ERROR,
                    "عذراً، حدث خطأ أثناء الاتصال بالذكاء الاصطناعي.",
                    chatSessionId);
        }
    }

    private void sendAiMessage(SimpMessageHeaderAccessor headerAccessor, WebSocketMessageType type, String content,
            UUID chatSessionId) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(type)
                .sessionId(chatSessionId)
                .senderName("AI Assistant")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();

        // Use the authenticated principal name if available, otherwise fallback to
        // session ID
        String destinationUser = headerAccessor.getUser() != null ? headerAccessor.getUser().getName()
                : headerAccessor.getSessionId();

        messagingTemplate.convertAndSendToUser(
                destinationUser,
                "/queue/ai",
                wsMessage,
                Map.of(SimpMessageHeaderAccessor.SESSION_ID_HEADER, headerAccessor.getSessionId()));
    }
}
