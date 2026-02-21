ackage com.neuralhealer.backend.feature.ai.dto.AiSessionChatResponse;

import java.util.List;
import java.util.UUID;

/**
 * Enhanced response for AI chat that includes the session ID.
 */
public record AiSessionChatResponse(
                UUID sessionId,
                String answer) {
}
