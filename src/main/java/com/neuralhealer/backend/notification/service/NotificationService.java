package com.neuralhealer.backend.notification.service;

import com.neuralhealer.backend.exception.ResourceNotFoundException;
import com.neuralhealer.backend.exception.UnauthorizedException;
import com.neuralhealer.backend.model.dto.NotificationCountResponse;
import com.neuralhealer.backend.model.dto.NotificationResponse;
import com.neuralhealer.backend.notification.entity.Notification;
import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.notification.entity.NotificationPriority;
import com.neuralhealer.backend.notification.entity.NotificationSource;
import com.neuralhealer.backend.notification.entity.NotificationType;
import com.neuralhealer.backend.notification.repository.NotificationRepository;
import com.neuralhealer.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Get paginated notifications for a user.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable)
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
     * Get unread count for a user.
     */
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

    @Transactional
    public void markAsRead(UUID notificationId, User user) {
        markAsRead(notificationId, user.getId());
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
    public void markAllAsRead(User user) {
        markAllAsRead(user.getId());
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

        return notificationRepository.save(notification);
    }

    // Helper to maintain backward compatibility if needed, or for simple usage
    @Transactional
    public void notifyUser(UUID userId, NotificationType type, String title, String message) {
        createNotification(userId, type, title, message, NotificationPriority.NORMAL, NotificationSource.SYSTEM, null);
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
