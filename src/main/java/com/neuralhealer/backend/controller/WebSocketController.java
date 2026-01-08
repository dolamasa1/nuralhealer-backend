package com.neuralhealer.backend.controller;

import com.neuralhealer.backend.model.dto.*;
import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.model.enums.WebSocketMessageType;

import com.neuralhealer.backend.service.EngagementMessageService;
import com.neuralhealer.backend.service.EngagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final EngagementMessageService messageService;
    private final EngagementService engagementService;

    /**
     * Handle incoming chat messages
     * Client sends to: /app/engagement/{engagementId}/message
     * Broadcast to: /topic/engagement/{engagementId}
     */
    @MessageMapping("/engagement/{engagementId}/message")
    public void handleMessage(
            @DestinationVariable UUID engagementId,
            @Payload SendMessageRequest request,
            @AuthenticationPrincipal User user) {

        try {
            if (user == null) {
                log.warn("Unauthenticated WebSocket message attempt");
                return;
            }

            // Verify user has access to this engagement
            if (!engagementService.canAccessEngagement(user, engagementId)) {
                log.warn("Unauthorized WebSocket message attempt by user {} for engagement {}",
                        user.getId(), engagementId);
                return;
            }

            // Save message to database
            MessageResponse savedMessage = messageService.sendMessage(user, engagementId, request);

            // Build WebSocket message
            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .type(WebSocketMessageType.CHAT_MESSAGE)
                    .engagementId(engagementId)
                    .senderId(user.getId())
                    .senderName(user.getFirstName() + " " + user.getLastName())
                    .content(savedMessage.content())
                    .timestamp(savedMessage.sentAt())
                    .metadata(savedMessage)
                    .build();

            // Broadcast to all users subscribed to this engagement
            messagingTemplate.convertAndSend(
                    "/topic/engagement/" + engagementId,
                    wsMessage);

            log.info("Message sent via WebSocket: engagement={}, sender={}", engagementId, user.getEmail());

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }

    /**
     * Handle typing indicators
     * Client sends to: /app/engagement/{engagementId}/typing
     * Broadcast to: /topic/engagement/{engagementId} (Unified topic)
     */
    @MessageMapping("/engagement/{engagementId}/typing")
    public void handleTyping(
            @DestinationVariable UUID engagementId,
            @Payload TypingIndicator indicator,
            @AuthenticationPrincipal User user) {

        try {
            if (user == null)
                return;

            if (!engagementService.canAccessEngagement(user, engagementId)) {
                return;
            }

            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .type(WebSocketMessageType.TYPING_INDICATOR)
                    .engagementId(engagementId)
                    .senderId(user.getId())
                    .senderName(user.getFirstName() + " " + user.getLastName())
                    .content(indicator.getIsTyping() ? "typing..." : "stopped")
                    .timestamp(LocalDateTime.now())
                    .metadata(indicator.getIsTyping()) // boolean state in metadata
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/engagement/" + engagementId,
                    wsMessage);

        } catch (Exception e) {
            log.error("Error handling typing indicator", e);
        }
    }

}
