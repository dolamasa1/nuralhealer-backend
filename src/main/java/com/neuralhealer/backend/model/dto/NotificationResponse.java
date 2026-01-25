package com.neuralhealer.backend.model.dto;

import com.neuralhealer.backend.notification.entity.NotificationPriority;
import com.neuralhealer.backend.notification.entity.NotificationSource;
import com.neuralhealer.backend.notification.entity.NotificationType;

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
