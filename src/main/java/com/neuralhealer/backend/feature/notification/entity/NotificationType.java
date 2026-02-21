ackage com.neuralhealer.backend.feature.notification.entity.NotificationType;

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
    SECURITY_ALERT,
    REMINDER_ALERT,

    // User Lifecycle
    USER_WELCOME,
    USER_REENGAGE_ACTIVE,
    USER_INACTIVITY_WARNING,
    EMAIL_VERIFICATION;
}
