package com.neuralhealer.backend.feature.auth.dto;

import com.neuralhealer.backend.shared.entity.UserRole;
import com.neuralhealer.backend.shared.util.ValidationMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration.
 * Used by: POST /api/auth/register
 */
public record RegisterRequest(
        @NotBlank(message = ValidationMessages.REQUIRED) @Email(message = ValidationMessages.INVALID_EMAIL) String email,

        @NotBlank(message = ValidationMessages.REQUIRED) @Size(min = 8, message = ValidationMessages.INVALID_PASSWORD) String password,

        @NotBlank(message = ValidationMessages.REQUIRED) String firstName,

        @NotBlank(message = ValidationMessages.REQUIRED) String lastName,

        @NotNull(message = ValidationMessages.REQUIRED) UserRole role,

        // Optional phone number
        String phone,

        // Doctor quick setup fields (optional)
        Boolean quickSetup,
        String title,
        String specialization) {

}
