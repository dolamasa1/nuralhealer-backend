ackage com.neuralhealer.backend.feature.engagement.dto.SendMessageRequest;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank(message = "Content cannot be empty") String content) {
}
