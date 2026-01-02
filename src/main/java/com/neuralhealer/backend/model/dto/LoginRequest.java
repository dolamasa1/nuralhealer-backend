package com.neuralhealer.backend.model.dto;

import com.neuralhealer.backend.util.ValidationMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 * Used by: POST /api/auth/login
 */
public record LoginRequest(
                @NotBlank(message = ValidationMessages.REQUIRED) @Email(message = ValidationMessages.INVALID_EMAIL) String email,

                @NotBlank(message = ValidationMessages.REQUIRED) String password) {
}
