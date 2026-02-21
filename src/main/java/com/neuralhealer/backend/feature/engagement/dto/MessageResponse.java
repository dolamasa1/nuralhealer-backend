ackage com.neuralhealer.backend.feature.engagement.dto.MessageResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        String content,
        UUID senderId,
        String senderName,
        boolean isSystemMessage,
        LocalDateTime sentAt) {
}
