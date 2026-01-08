package com.neuralhealer.backend.model.dto;

import java.util.List;

/**
 * Response DTO from external AI chatbot API.
 */
public record AiChatResponse(
        String answer,
        List<String> sources) {
}
