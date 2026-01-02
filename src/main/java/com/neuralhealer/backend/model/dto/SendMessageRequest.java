package com.neuralhealer.backend.model.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank(message = "Content cannot be empty") String content) {
}
