package com.neuralhealer.backend.feature.engagement.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank(message = "Content cannot be empty") String content) {
}
