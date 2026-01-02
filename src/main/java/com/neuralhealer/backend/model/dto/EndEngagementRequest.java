package com.neuralhealer.backend.model.dto;

import jakarta.validation.constraints.NotBlank;

public record EndEngagementRequest(
        @NotBlank(message = "Reason is required") String reason) {
}
