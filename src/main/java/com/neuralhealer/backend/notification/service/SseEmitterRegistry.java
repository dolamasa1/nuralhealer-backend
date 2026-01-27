package com.neuralhealer.backend.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseEmitterRegistry {

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong totalConnectionsCreated = new java.util.concurrent.atomic.AtomicLong(
            0);

    public SseEmitter createEmitter(UUID userId) {
        totalConnectionsCreated.incrementAndGet();
        // 30 minutes timeout
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.debug("Emitter completed for user: {}", userId);
        });

        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.debug("Emitter timed out for user: {}", userId);
        });

        emitter.onError((e) -> {
            emitters.remove(userId);
            log.debug("Emitter error for user: {}: {}", userId, e.getMessage());
        });

        emitters.put(userId, emitter);
        log.debug("Emitter registered for user: {}", userId);

        return emitter;
    }

    public boolean send(UUID userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .data(data)
                        .id(UUID.randomUUID().toString())
                        .name("notification")
                        .reconnectTime(5000));
                return true;
            } catch (IOException e) {
                log.warn("Failed to send SSE to user {}, removing emitter: {}", userId, e.getMessage());
                emitters.remove(userId);
            }
        }
        return false;
    }

    public boolean isUserConnected(UUID userId) {
        return emitters.containsKey(userId);
    }

    /**
     * Send heartbeat to keep connections alive and detect disconnected clients.
     * Scheduled every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000)
    public void sendHeartbeat() {
        if (emitters.isEmpty())
            return;

        log.debug("Sending heartbeat to {} connections", emitters.size());
        Map<String, Object> heartbeatData = Map.of(
                "status", "ping",
                "timestamp", java.time.LocalDateTime.now().toString());

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(heartbeatData)
                        .comment("keep-alive"));
            } catch (IOException e) {
                emitters.remove(userId);
                log.debug("Dead emitter removed during heartbeat for user: {}", userId);
            }
        });
    }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeConnections", emitters.size());
        metrics.put("totalConnectionsSinceStart", totalConnectionsCreated.get());
        return metrics;
    }
}
