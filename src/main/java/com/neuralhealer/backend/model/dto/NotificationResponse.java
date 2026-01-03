package com.neuralhealer.backend.model.dto;

import com.neuralhealer.backend.model.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        UUID engagementId,
        boolean isRead,
        LocalDateTime createdAt) {
}
