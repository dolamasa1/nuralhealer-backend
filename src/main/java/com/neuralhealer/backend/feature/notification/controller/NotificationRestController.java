package com.neuralhealer.backend.feature.notification.controller;

import com.neuralhealer.backend.shared.exception.UnauthorizedException;
import com.neuralhealer.backend.feature.notification.dto.NotificationCountResponse;
import com.neuralhealer.backend.feature.notification.dto.NotificationResponse;
import com.neuralhealer.backend.shared.entity.User;
import com.neuralhealer.backend.feature.notification.entity.NotificationPriority;
import com.neuralhealer.backend.feature.notification.entity.NotificationSource;
import com.neuralhealer.backend.feature.notification.entity.NotificationType;
import com.neuralhealer.backend.feature.notification.service.NotificationService;
import com.neuralhealer.backend.feature.notification.service.SseEmitterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationRestController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @GetMapping
    @Operation(summary = "Get paginated notifications for current user with filtering")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationPriority priority,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationSource source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(notificationService.getUserNotifications(
                user.getId(), type, priority, isRead, source, pageRequest));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get current user's unread notification count")
    public ResponseEntity<NotificationCountResponse> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getUnreadCount(user));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a specific notification as read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        notificationService.markAsRead(id, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications for current user as read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get notification statistics for current user")
    public ResponseEntity<Map<String, Object>> getStats(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getNotificationStats(user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a specific notification")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        notificationService.deleteNotification(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time notifications (SSE)")
    public SseEmitter streamNotifications(
            @AuthenticationPrincipal User user,
            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {

        // Use user.getEmail() for logging, carefully checking if user is not null
        String email = user != null ? user.getEmail() : "anonymous";
        UUID userId = user != null ? user.getId() : null;

        log.info("SSE connection request from user: {}, Last-Event-ID: {}", email, lastEventId);

        if (userId == null) {
            throw new UnauthorizedException("User session required for notifications");
        }

        SseEmitter emitter = sseEmitterRegistry.createEmitter(userId);

        // Replay missed notifications if Last-Event-ID is provided
        try {
            if (lastEventId != null && !lastEventId.trim().isEmpty()) {
                LocalDateTime lastSeen = parseTimestampFromLastEventId(lastEventId);
                notificationService.pushMissedNotifications(userId, lastSeen);
            } else {
                notificationService.pushUnreadNotifications(userId);
            }
        } catch (Exception e) {
            log.warn("Could not replay missed notifications for user {}: {}", userId, e.getMessage());
            notificationService.pushUnreadNotifications(userId);
        }

        // Send initial connection confirmation
        try {
            Map<String, Object> connectedData = Map.of(
                    "status", "connected",
                    "message", "SSE Stream Connected",
                    "timestamp", LocalDateTime.now().toString());

            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(connectedData));
        } catch (Exception e) {
            log.warn("Failed to send initial SSE message: {}", e.getMessage());
        }

        return emitter;
    }

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
