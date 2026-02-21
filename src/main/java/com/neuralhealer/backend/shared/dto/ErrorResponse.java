package com.neuralhealer.backend.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response format for API errors.
 * Matches frontend expected format:
 * { status: 400, message: "Error message", timestamp: "...", path: "/api/..." }
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String message,
        LocalDateTime timestamp,
        String path,
        Map<String, String> errors) {
    /**
     * Create error response without field errors.
     */
    public static ErrorResponse of(int status, String message, String path) {
        return ErrorResponse.builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    /**
     * Create error response with field validation errors.
     */
    public static ErrorResponse of(int status, String message, String path, Map<String, String> errors) {
        return ErrorResponse.builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .errors(errors)
                .build();
    }
}
