ackage com.neuralhealer.backend.feature.notification.model.LocalizedMessage;

/**
 * Projection for the result of get_notification_message SQL function.
 */
public interface LocalizedMessage {
    String getTitle();

    String getMessage();

    String getPriority();
}
