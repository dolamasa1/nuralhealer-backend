package com.neuralhealer.backend.service;

import com.neuralhealer.backend.exception.ResourceNotFoundException;
import com.neuralhealer.backend.exception.UnauthorizedException;
import com.neuralhealer.backend.model.dto.NotificationCountResponse;
import com.neuralhealer.backend.model.dto.NotificationResponse;
import com.neuralhealer.backend.model.entity.Notification;
import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.model.enums.NotificationType;
import com.neuralhealer.backend.repository.NotificationRepository;
import com.neuralhealer.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void notifyUser(UUID userId, NotificationType type, String title, String message, UUID engagementId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // 1. Persistence Layer (Source of Truth)
            Notification notification = Notification.builder()
                    .user(user)
                    .type(type)
                    .title(title)
                    .message(message)
                    .engagementId(engagementId)
                    .createdAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            notification = notificationRepository.save(notification);

            // 2. Real-Time Layer (Best Effort)
            try {
                NotificationResponse response = mapToResponse(notification);
                // Send to /user/{userId}/queue/notifications
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/notifications",
                        response);
            } catch (Exception e) {
                log.warn("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to process notification for user {}", userId, e);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(User user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationCountResponse getUnreadCount(User user) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        return new NotificationCountResponse(count);
    }

    @Transactional
    public void markAsRead(UUID notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to modify this notification");
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(n -> !n.isRead())
                .toList();

        unreadNotifications.forEach(n -> {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
        });

        notificationRepository.saveAll(unreadNotifications);
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

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getEngagementId(),
                n.isRead(),
                n.getCreatedAt());
    }
}
