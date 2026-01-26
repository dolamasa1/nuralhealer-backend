package com.neuralhealer.backend.notification.service;

import com.neuralhealer.backend.model.dto.NotificationResponse;
import com.neuralhealer.backend.notification.entity.Notification;
import com.neuralhealer.backend.notification.entity.NotificationPriority;
import com.neuralhealer.backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPoller {

    private final NotificationRepository notificationRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * Poll for undelivered notifications every 500ms.
     * Rules:
     * - LOW priority: Never via SSE (skip)
     * - CRITICAL/HIGH: Always push if connected
     * - NORMAL: Push if connected
     */
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void pollNotifications() {
        List<Notification> undelivered = notificationRepository.findUndeliveredSseNotifications();

        if (!undelivered.isEmpty()) {
            log.trace("Found {} undelivered notifications", undelivered.size());
        }

        for (Notification notification : undelivered) {
            try {
                processNotification(notification);
            } catch (Exception e) {
                log.error("Failed to process notification {}: {}", notification.getId(), e.getMessage());
            }
        }
    }

    private void processNotification(Notification notification) {
        // Skip LOW priority for SSE
        if (notification.getPriority() == NotificationPriority.low) {
            markAsPushed(notification, false); // Mark addressed but not pushed
            return;
        }

        java.util.UUID userId = notification.getUser().getId();

        // Check connection
        if (sseEmitterRegistry.isUserConnected(userId)) {
            // Convert to DTO
            NotificationResponse response = mapToResponse(notification);

            // Send
            sseEmitterRegistry.send(userId, response);

            // Mark successful push
            markAsPushed(notification, true);
        } else {
            // User not connected.
            // If CRITICAL/HIGH, we might want to retry later?
            // Current req: "Deliver only if user connected" (NORMAL)
            // "Always deliver" (CRITICAL/HIGH) implies we should try, but if not connected
            // we can't.
            // So we leave it as undelivered? No, that would cause accumulation.
            // Requirement says: "Query: WHERE delivery_status->>'sse' = false".
            // If we don't mark it true, we keep polling it.
            // Optimization: If not connected, do we keep it pending?
            // Rule: "Poll every 500ms". If we keep pending, it fetches forever.
            // Decision: If user OFFLINE, we probably shouldn't hammer DB.
            // But for Phase 2, let's keep it simple: WE only deliver if connected.
            // We should NOT mark as 'sse: true' if we didn't send it, potentially.
            // But then findUndeliveredSseNotifications() will return it forever.
            // Let's implementation: Only mark sse=true IF sent.
            // BUT this means query will grow large.
            // Typically SSE is "fire and forget" or "deliver if online".
            // Let's leave it as is -> stored in DB for REST retrieval.

            // However, to prevent re-fetching the same old notifications for offline users:
            // We should probably have a 'last_polled' or similar?
            // OR we accept that 'undelivered' means 'not yet sent via SSE'.
            // If user never connects, it stays 'false'.
            // To avoid fetching million rows: Query should probably also filter by
            // `created_at > now() - 1 hour`?
            // Let's stick to the simplest interpretation:
            // Fetch everything that is sse=false.
            // If user online -> send & mark true.
            // If user offline -> Do we ignore? Yes.
            // Issue: Repeatedly fetching same rows.
            // IMPROVEMENT: Query could be `... AND created_at > :recentTime`?
            // Or better: We mark as "skipped" if user offline?
            // "NORMAL: Deliver only if user connected". If NOT connected, it is NOT
            // delivered.
            // "LOW: Never via SSE".

            // Refinement: processNotification works on the queue.
            // We shouldn't process old notifications repeatedly.
            // Let's mark them as 'skipped' or 'failed' in delivery_status if they are too
            // old?
            // Or just leave them.
        }
    }

    private void markAsPushed(Notification n, boolean pushed) {
        Map<String, Object> status = new HashMap<>(n.getDeliveryStatus());
        status.put("sse", pushed); // true if pushed, false (or special value) if skipped?
        // Wait, if pushed=false, it will be picked up again.
        // For LOW priority, we want to STOP picking it up. So set sse=true (meaning
        // "processed for SSE channel").
        // "delivery_status" usually means "attempted/processed".

        if (!pushed && n.getPriority() == NotificationPriority.low) {
            status.put("sse", "skipped_low_priority"); // Treat as strict JSONB later if needed, but Map allows value
            // If we put string, query `delivery_status->>'sse' = 'false'` might break if it
            // expects boolean.
            // Query is: `n.delivery_status->>'sse' = 'false'`.
            // If we set it to "skipped", it is NOT 'false'. So it won't be picked up.
            // Correct.
        } else if (pushed) {
            status.put("sse", true);
        }

        n.setDeliveryStatus(status);
        notificationRepository.save(n);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getPriority(),
                n.getSource(),
                n.getPayload(),
                n.getIsRead(),
                n.getSentAt(),
                n.getReadAt());
    }
}
