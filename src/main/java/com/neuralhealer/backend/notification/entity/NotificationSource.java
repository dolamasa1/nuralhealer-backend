package com.neuralhealer.backend.notification.entity;

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
