ackage com.neuralhealer.backend.feature.auth.dto.ResendOtpRequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for resending email verification OTP.
 */
public record ResendOtpRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email) {
}
