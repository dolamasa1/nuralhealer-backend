package com.neuralhealer.backend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralhealer.backend.model.dto.AiChatResponse;
import com.neuralhealer.backend.service.AiChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Simple Raw WebSocket Handler for AI Chat.
 * No STOMP, No Subscriptions. Just send JSON -> receive JSON.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AiSimpleWebSocketHandler extends TextWebSocketHandler {

    private final AiChatbotService aiChatbotService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        if (!aiChatbotService.isAiAvailable()) {
            session.close(CloseStatus.SERVICE_OVERLOAD);
            return;
        }
        log.info("AI Chat Connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received from {}: {}", session.getId(), payload);

        try {
            // 1. Parse Input: {"question": "..."}
            @SuppressWarnings("unchecked")
            Map<String, Object> request = objectMapper.readValue(payload, Map.class);
            String question = (String) request.get("question");

            if (question == null || question.isBlank()) {
                return;
            }

            // 2. Send TYPING_START
            sendJson(session, Map.of(
                    "type", "AI_TYPING_START",
                    "senderName", "AI Assistant",
                    "content", "المساعد الذكي يكتب..."));

            try {
                // 3. Call AI Service
                AiChatResponse response = aiChatbotService.askQuestion(question);

                // Clean response (Remove "Answer:" prefix if present)
                String cleanAnswer = response.answer();
                if (cleanAnswer != null) {
                    // Optimized regex to handle various prefix formats
                    cleanAnswer = cleanAnswer.replaceAll("^[\\s\\n]*(الإجابة|الأجابة|الرد|الإجابة هي)[:\\-]?\\s*", "");
                }

                // Deduplicate sources and handle nulls
                List<String> distinctSources = List.of();
                if (response.sources() != null) {
                    distinctSources = response.sources().stream()
                            .filter(s -> s != null && !s.isBlank())
                            .distinct()
                            .toList();
                }

                // Log sources on server for reference
                if (!distinctSources.isEmpty()) {
                    log.info("AI Response sources for {}: {}", session.getId(), distinctSources);
                }

                // 4. Send TYPING_STOP
                sendJson(session, Map.of(
                        "type", "AI_TYPING_STOP",
                        "senderName", "AI Assistant"));

                // 5. Send RESPONSE (Exclude sources per user request)
                sendJson(session, Map.of(
                        "type", "AI_RESPONSE",
                        "senderName", "AI Assistant",
                        "content", cleanAnswer));

            } catch (Exception e) {
                log.error("AI Error: {}", e.getMessage());

                // Send TYPING_STOP before ERROR
                sendJson(session, Map.of(
                        "type", "AI_TYPING_STOP",
                        "senderName", "AI Assistant"));

                sendJson(session, Map.of(
                        "type", "AI_ERROR",
                        "senderName", "System",
                        "content", "عذراً، حدث خطأ أثناء الاتصال بالذكاء الاصطناعي."));
            }

        } catch (Exception e) {
            log.error("Invalid JSON from {}: {}", session.getId(), payload);
            sendJson(session, Map.of(
                    "type", "ERROR",
                    "content", "Invalid format. Expected JSON with 'question' field."));
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        log.info("AI Chat Disconnected: {}", session.getId());
    }

    private void sendJson(WebSocketSession session, Map<String, Object> data) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(data);
            if (json != null) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }
}
