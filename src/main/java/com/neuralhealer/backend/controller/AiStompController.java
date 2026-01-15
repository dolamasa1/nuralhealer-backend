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
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * STOMP Controller for AI Chatbot.
 * Handles AI requests over the standard WebSocket broker.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AiStompController {

    private final AiChatbotService aiChatbotService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle AI questions via STOMP.
     * Destination: /app/ai/ask
     * Responses sent to: /user/queue/ai
     */
    @MessageMapping("/ai/ask")
    public void askAi(@Payload AiChatRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String destination = "/queue/ai"; // Will be prefixed by /user by SimpMessagingTemplate

        log.info("STOMP AI request received: session={}", sessionId);

        if (!aiChatbotService.isAiAvailable()) {
            sendAiMessage(headerAccessor, WebSocketMessageType.AI_ERROR, "الذكاء الاصطناعي غير متاح حالياً");
            return;
        }

        // 1. Send TYPING_START
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
