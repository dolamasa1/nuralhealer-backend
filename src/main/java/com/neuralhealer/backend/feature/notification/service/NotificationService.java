package com.neuralhealer.backend.feature.notification.service;

import com.neuralhealer.backend.shared.exception.ResourceNotFoundException;
import com.neuralhealer.backend.shared.exception.UnauthorizedException;
import com.neuralhealer.backend.feature.notification.dto.NotificationCountResponse;
import com.neuralhealer.backend.feature.notification.dto.NotificationResponse;
import com.neuralhealer.backend.feature.notification.entity.Notification;
import com.neuralhealer.backend.shared.entity.User;
import com.neuralhealer.backend.feature.notification.entity.NotificationPriority;
import com.neuralhealer.backend.feature.notification.entity.NotificationSource;
import com.neuralhealer.backend.feature.notification.entity.NotificationType;
import com.neuralhealer.backend.feature.notification.repository.NotificationRepository;
import com.neuralhealer.backend.feature.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * Get paginated notifications for a user with optional filters.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(
            UUID userId,
            NotificationType type,
            NotificationPriority priority,
            Boolean isRead,
            NotificationSource source,
            Pageable pageable) {

        Specification<Notification> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (type != null)
                predicates.add(cb.equal(root.get("type"), type));
            if (priority != null)
                predicates.add(cb.equal(root.get("priority"), priority));
            if (isRead != null)
                predicates.add(cb.equal(root.get("isRead"), isRead));
            if (source != null)
                predicates.add(cb.equal(root.get("source"), source));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return notificationRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get list of notifications for a user (legacy/simple support).
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(User user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get notification statistics for a user.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStats(UUID userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", notificationRepository.countByUserId(userId)); // Need to add this to repo
        stats.put("unreadCount", notificationRepository.countByUserIdAndIsReadFalse(userId));

        Map<String, Long> priorityStats = new HashMap<>();
        notificationRepository.countByPriorityForUser(userId)
                .forEach(row -> priorityStats.put(row[0].toString(), (Long) row[1]));
        stats.put("byPriority", priorityStats);

        Map<String, Long> typeStats = new HashMap<>();
        notificationRepository.countByTypeForUser(userId)
                .forEach(row -> typeStats.put(row[0].toString(), (Long) row[1]));
        stats.put("byType", typeStats);

        return stats;
    }

    @Transactional(readOnly = true)
    public NotificationCountResponse getUnreadCount(User user) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        return new NotificationCountResponse(count);
    }

    /**
     * Get unread count for a user (UUID version).
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("User not authorized to access this notification");
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> !n.getIsRead())
                .toList();

        unread.forEach(n -> {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
        });

        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void deleteNotification(UUID notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to delete this notification");
        }

        notificationRepository.delete(notification);
    }

    /**
     * Manually create a notification.
     * Use this for SYSTEM, MESSAGE, AI sources. Engagement changes are mostly
     * triggered by DB.
     */
    @Transactional
    public Notification createNotification(UUID userId,
            NotificationType type,
            String title,
            String message,
            NotificationPriority priority,
            NotificationSource source,
            Map<String, Object> payload) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .priority(priority)
                .source(source)
                .payload(payload != null ? payload : Collections.emptyMap())
                .deliveryStatus(Map.of("sse", false))
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        // Push directly to SSE if user is connected (no polling needed)
        if (priority != NotificationPriority.low) {
            pushToSse(userId, notification);
        }

        return notification;
    }

    @Transactional
    public void pushUnreadNotifications(UUID userId) {
        log.debug("Pushing undelivered notifications for user: {}", userId);
        List<Notification> undelivered = notificationRepository.findUndeliveredSseNotificationsForUser(userId);
        undelivered.forEach(n -> {
            if (n.getPriority() != NotificationPriority.low) {
                pushToSse(userId, n);
            }
        });
    }

    private void pushToSse(UUID userId, Notification notification) {
        if (sseEmitterRegistry.isUserConnected(userId)) {
            NotificationResponse response = mapToResponse(notification);

            // Format: uuid_epoch (e.g., 550e8400-e29b-41d4-a716-446655440000_1706173200)
            String eventId = notification.getId().toString() + "_" +
                    notification.getSentAt().toEpochSecond(ZoneOffset.UTC);

            boolean sent = sseEmitterRegistry.send(userId, eventId, response);
            if (sent) {
                // Mark as delivered via SSE
                notification.setDeliveryStatus(Map.of("sse", true));
                notificationRepository.save(notification);
                log.debug("Pushed notification {} to user {} via SSE", notification.getId(), userId);
            }
        }
    }

    // Helper to maintain backward compatibility if needed, or for simple usage
    @Transactional
    public void notifyUser(UUID userId, NotificationType type, String title, String message) {
        NotificationPriority priority = NotificationPriority.normal;

        // Phase 5 Priority Fix: started/cancelled engagement events are HIGH priority
        if (type == NotificationType.ENGAGEMENT_STARTED || type == NotificationType.ENGAGEMENT_CANCELLED) {
            priority = NotificationPriority.high;
        }

        createNotification(userId, type, title, message, priority, NotificationSource.engagement, null);
    }

    @Transactional
    public void pushMissedNotifications(UUID userId, LocalDateTime lastSeenAt) {
        log.debug("Pushing missed notifications for user: {} since {}", userId, lastSeenAt);
        List<Notification> missed = notificationRepository.findMissedNotifications(userId, lastSeenAt);
        missed.forEach(n -> {
            if (n.getPriority() != NotificationPriority.low) {
                pushToSse(userId, n);
            }
        });
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
