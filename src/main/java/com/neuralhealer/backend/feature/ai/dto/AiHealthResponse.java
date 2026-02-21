package com.neuralhealer.backend.feature.ai.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for AI health check.
 */
public record AiHealthResponse(
        boolean connected,
        String message,
        LocalDateTime lastChecked) {
}
