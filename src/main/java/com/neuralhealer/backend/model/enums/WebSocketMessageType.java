package com.neuralhealer.backend.model.enums;

public enum WebSocketMessageType {
    CHAT_MESSAGE, // Regular message
    TYPING_START, // User started typing
    TYPING_STOP, // User stopped typing
    USER_JOINED, // User connected to engagement
    USER_LEFT, // User disconnected
    MESSAGE_READ, // Message marked as read
    ENGAGEMENT_STATUS, // Engagement status changed
    NOTIFICATION, // System notification
    AI_TYPING_START, // AI is processing question
    AI_TYPING_STOP, // AI finished processing
    AI_RESPONSE, // AI answer ready
    AI_ERROR // AI failed to respond
}
