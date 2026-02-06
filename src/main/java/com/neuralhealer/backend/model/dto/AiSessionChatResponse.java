package com.neuralhealer.backend.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * Enhanced response for AI chat that includes the session ID.
 */
public record AiSessionChatResponse(
        UUID sessionId,
        String answer,
        List<String> sources) {
}
