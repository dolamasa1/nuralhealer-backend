ackage com.neuralhealer.backend.feature.notification.entity.NotificationSource;

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
