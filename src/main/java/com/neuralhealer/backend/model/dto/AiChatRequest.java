package com.neuralhealer.backend.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for AI chatbot questions.
 * Supports Arabic text via UTF-8 encoding.
 */
public record AiChatRequest(
                @NotBlank(message = "Question cannot be empty") String question,
                String country) {
}
