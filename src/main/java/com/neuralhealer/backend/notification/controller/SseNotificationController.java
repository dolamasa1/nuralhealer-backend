package com.neuralhealer.backend.notification.controller;

import com.neuralhealer.backend.model.entity.User;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Stream", description = "Real-time SSE notification stream")
public class SseNotificationController {

    private final SseEmitterRegistry sseEmitterRegistry;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time notifications (SSE)")
    public SseEmitter streamNotifications(@AuthenticationPrincipal User user) {
        log.info("SSE connection request from user: {}", user.getEmail());

        SseEmitter emitter = sseEmitterRegistry.createEmitter(user.getId());

        // precise initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE Stream Connected")
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send initial SSE message: {}", e.getMessage());
        }

        return emitter;
    }
}
