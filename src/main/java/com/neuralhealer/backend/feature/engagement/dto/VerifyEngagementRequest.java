ackage com.neuralhealer.backend.feature.engagement.dto.VerifyEngagementRequest;

import jakarta.validation.constraints.NotBlank;

public record VerifyEngagementRequest(
        @NotBlank(message = "Token is required") String token) {
}
