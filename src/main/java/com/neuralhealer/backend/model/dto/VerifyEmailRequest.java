package com.neuralhealer.backend.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for email verification with OTP.
 */
public record VerifyEmailRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

        @NotBlank(message = "OTP code is required") @Pattern(regexp = "^[0-9]{6}$", message = "OTP code must be exactly 6 digits") String otpCode) {
}
