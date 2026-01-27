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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

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
    public SseEmitter streamNotifications(
            @AuthenticationPrincipal User user,
            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {
        log.info("SSE connection request from user: {}, Last-Event-ID: {}", user.getEmail(), lastEventId);

        SseEmitter emitter = sseEmitterRegistry.createEmitter(user.getId());

        // Replay missed notifications if Last-Event-ID is provided
        try {
            if (lastEventId != null && !lastEventId.trim().isEmpty()) {
                LocalDateTime lastSeen = parseTimestampFromLastEventId(lastEventId);
                notificationService.pushMissedNotifications(user.getId(), lastSeen);
            } else {
                // Otherwise just push standard undelivered ones
                notificationService.pushUnreadNotifications(user.getId());
            }
        } catch (Exception e) {
            log.warn("Could not replay missed notifications for user {}: {}", user.getId(), e.getMessage());
            // Fallback: push undelivered
            notificationService.pushUnreadNotifications(user.getId());
        }

        // precise initial connection confirmation
        try {
            Map<String, Object> connectedData = Map.of(
                    "status", "connected",
                    "message", "SSE Stream Connected",
                    "timestamp", java.time.LocalDateTime.now().toString());

            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data((Object) connectedData)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send initial SSE message: {}", e.getMessage());
        }

        return emitter;
    }

    /**
     * Parses the epoch timestamp from the Last-Event-ID header.
     * Format: uuid_epoch (e.g., 550e8400-e29b-41d4-a716-446655440000_1706173200)
     */
    private LocalDateTime parseTimestampFromLastEventId(String lastEventId) {
        try {
            String[] parts = lastEventId.split("_");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid Last-Event-ID format");
            }
            long epoch = Long.parseLong(parts[1]);
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Invalid Last-Event-ID format: {}", lastEventId);
            throw new IllegalArgumentException("Invalid Last-Event-ID format", e);
        }
    }
}
