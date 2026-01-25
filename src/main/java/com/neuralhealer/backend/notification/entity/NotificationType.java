package com.neuralhealer.backend.notification.entity;

public enum NotificationType {
    // Engagement Related
    ENGAGEMENT_STARTED,
    ENGAGEMENT_ENDED,
    ENGAGEMENT_CANCELLED,
    ACCESS_CHANGED,

    // Communication
    MESSAGE_RECEIVED,

    // AI & System
    AI_RESPONSE_READY,
    SYSTEM_ALERT,
    SYSTEM_BROADCAST,
    REMINDER_ALERT;
}
