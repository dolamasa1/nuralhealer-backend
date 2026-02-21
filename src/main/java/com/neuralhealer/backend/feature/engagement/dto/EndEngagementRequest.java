ackage com.neuralhealer.backend.feature.engagement.dto.EndEngagementRequest;

import jakarta.validation.constraints.NotBlank;

public record EndEngagementRequest(
        @NotBlank(message = "Reason is required") String reason) {
}
