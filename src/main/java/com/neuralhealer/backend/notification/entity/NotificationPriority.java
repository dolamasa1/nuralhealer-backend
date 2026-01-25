package com.neuralhealer.backend.notification.entity;

import lombok.Getter;

@Getter
public enum NotificationPriority {
    CRITICAL,
    HIGH,
    NORMAL,
    LOW;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
