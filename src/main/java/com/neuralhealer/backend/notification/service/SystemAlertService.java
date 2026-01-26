package com.neuralhealer.backend.notification.service;

import com.neuralhealer.backend.notification.entity.NotificationPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemAlertService {

    private final NotificationCreatorService notificationCreatorService;

    /**
     * Trigger a critical system alert (e.g., service downtime).
     */
    public void triggerCriticalAlert(UUID userId, String title, String message) {
        log.error("Critical Alert for User {}: {} - {}", userId, title, message);
        notificationCreatorService.createSystemNotification(userId, title, message, NotificationPriority.critical);
    }

    /**
     * Trigger a high-priority security alert.
     */
    public void triggerSecurityAlert(UUID userId, String title, String message) {
        log.warn("Security Alert for User {}: {} - {}", userId, title, message);
        notificationCreatorService.createSecurityNotification(userId, title, message);
    }

    /**
     * Trigger a general system announcement/broadcast for a specific user.
     */
    public void triggerAnnouncement(UUID userId, String title, String message) {
        notificationCreatorService.createSystemNotification(userId, title, message, NotificationPriority.normal);
    }
}
