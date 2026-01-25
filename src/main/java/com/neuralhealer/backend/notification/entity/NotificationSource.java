package com.neuralhealer.backend.notification.entity;

import lombok.Getter;

@Getter
public enum NotificationSource {
    ENGAGEMENT,
    MESSAGE,
    SYSTEM,
    AI,
    REMINDER,
    ADMIN;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
