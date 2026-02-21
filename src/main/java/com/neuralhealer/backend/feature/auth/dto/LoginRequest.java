package com.neuralhealer.backend.feature.auth.dto;

import com.neuralhealer.backend.shared.util.ValidationMessages;
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
