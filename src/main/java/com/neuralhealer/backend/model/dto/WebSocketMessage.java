package com.neuralhealer.backend.model.dto;

import com.neuralhealer.backend.model.enums.WebSocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private WebSocketMessageType type;
    private UUID engagementId;
    private UUID senderId;
    private String senderName;
    private String content;
    private LocalDateTime timestamp;
    private Object metadata; // For additional data (e.g., status string)
}
