package com.neuralhealer.backend.notification.model;

/**
 * Projection for the result of get_notification_message SQL function.
 */
public interface LocalizedMessage {
    String getTitle();

    String getMessage();

    String getPriority();
}
