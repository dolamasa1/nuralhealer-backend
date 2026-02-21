package com.neuralhealer.backend.feature.notification.dto;

import com.neuralhealer.backend.feature.notification.entity.NotificationPriority;
import com.neuralhealer.backend.feature.notification.entity.NotificationSource;
import com.neuralhealer.backend.feature.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
                UUID id,
                NotificationType type,
                String title,
                String message,
                NotificationPriority priority,
                NotificationSource source,
                Map<String, Object> payload,
                boolean isRead,
                LocalDateTime sentAt,
                LocalDateTime readAt) {
}
