package com.neuralhealer.backend.notification.service;

import com.neuralhealer.backend.model.entity.User;
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

    // Placeholder for manual notification creation logic
    // This will standardize creation from different sources (AI, System, etc.)

    public void createSystemNotification(UUID userId, String title, String message, NotificationPriority priority) {
        notificationService.createNotification(
                userId,
                NotificationType.SYSTEM_ALERT,
                title,
                message,
                priority,
                NotificationSource.SYSTEM,
                null);
    }
}
