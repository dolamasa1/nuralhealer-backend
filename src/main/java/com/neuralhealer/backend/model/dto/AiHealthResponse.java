package com.neuralhealer.backend.model.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for AI health check.
 */
public record AiHealthResponse(
        boolean connected,
        String message,
        LocalDateTime lastChecked) {
}
