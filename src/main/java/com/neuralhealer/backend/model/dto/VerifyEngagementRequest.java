package com.neuralhealer.backend.model.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEngagementRequest(
        @NotBlank(message = "Token is required") String token) {
}
