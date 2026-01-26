package com.neuralhealer.backend.notification.service;

import com.neuralhealer.backend.notification.entity.NotificationPriority;
import com.neuralhealer.backend.notification.entity.NotificationSource;
import com.neuralhealer.backend.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationCreatorService {

    private final NotificationService notificationService;

    /**
     * Standardized method for system alerts.
     */
    public void createSystemNotification(UUID userId, String title, String message, NotificationPriority priority) {
        notificationService.createNotification(
                userId,
                NotificationType.SYSTEM_ALERT,
                title,
                message,
                priority,
                NotificationSource.system,
                null);
    }

    /**
     * Create notification for new messages.
     */
    public void createMessageNotification(UUID userId, UUID senderId, String senderName, String messagePreview) {
        Map<String, Object> payload = Map.of(
                "senderId", senderId,
                "senderName", senderName,
                "preview", messagePreview);

        notificationService.createNotification(
                userId,
                NotificationType.MESSAGE_RECEIVED,
                "New Message",
                senderName + ": " + messagePreview,
                NotificationPriority.normal,
                NotificationSource.message,
                payload);
    }

    /**
     * Create notification for AI results.
     */
    public void createAiNotification(UUID userId, String title, String summary, UUID resultId) {
        Map<String, Object> payload = Map.of("resultId", resultId != null ? resultId : UUID.randomUUID());

        notificationService.createNotification(
                userId,
                NotificationType.AI_RESPONSE_READY,
                title,
                summary,
                NotificationPriority.normal,
                NotificationSource.ai,
                payload);
    }

    /**
     * Create notification for security events (MFA, Login).
     */
    public void createSecurityNotification(UUID userId, String title, String message) {
        notificationService.createNotification(
                userId,
                NotificationType.SECURITY_ALERT,
                title,
                message,
                NotificationPriority.high,
                NotificationSource.system,
                null);
    }
}
