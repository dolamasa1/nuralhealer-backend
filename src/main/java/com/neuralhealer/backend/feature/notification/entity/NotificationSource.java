package com.neuralhealer.backend.feature.notification.entity;

import lombok.Getter;

@Getter
public enum NotificationSource {
    engagement,
    message,
    system,
    ai,
    reminder,
    admin
}
