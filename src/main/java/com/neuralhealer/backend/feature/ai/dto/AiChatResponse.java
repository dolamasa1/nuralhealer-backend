ackage com.neuralhealer.backend.feature.ai.dto.AiChatResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO from external AI chatbot API.
 */
public record AiChatResponse(
        @JsonProperty("response") String answer) {
}
