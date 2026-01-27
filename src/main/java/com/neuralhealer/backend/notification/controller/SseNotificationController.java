package com.neuralhealer.backend.notification.controller;

import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.notification.service.NotificationService;
import com.neuralhealer.backend.notification.service.SseEmitterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Stream", description = "Real-time SSE notification stream")
public class SseNotificationController {

    private final SseEmitterRegistry sseEmitterRegistry;
    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time notifications (SSE)")
    public SseEmitter streamNotifications(@AuthenticationPrincipal User user) {
        log.info("SSE connection request from user: {}", user.getEmail());

        SseEmitter emitter = sseEmitterRegistry.createEmitter(user.getId());

        // Push any unread notifications immediately
        try {
            notificationService.pushUnreadNotifications(user.getId());
        } catch (Exception e) {
            log.warn("Failed to push initial notifications for user {}: {}", user.getId(), e.getMessage());
        }

        // precise initial connection confirmation
        try {
            Map<String, String> connectedData = Map.of(
                    "status", "connected",
                    "message", "SSE Stream Connected",
                    "timestamp", java.time.LocalDateTime.now().toString());

            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(connectedData)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send initial SSE message: {}", e.getMessage());
        }

        return emitter;
    }
}
