package com.neuralhealer.backend.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for resending email verification OTP.
 */
public record ResendOtpRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email) {
}
