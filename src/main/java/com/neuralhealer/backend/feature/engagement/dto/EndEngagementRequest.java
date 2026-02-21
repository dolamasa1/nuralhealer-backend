package com.neuralhealer.backend.feature.engagement.dto;

import jakarta.validation.constraints.NotBlank;

public record EndEngagementRequest(
        @NotBlank(message = "Reason is required") String reason) {
}
