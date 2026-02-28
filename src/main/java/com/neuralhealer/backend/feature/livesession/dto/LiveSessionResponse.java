package com.neuralhealer.backend.feature.livesession.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveSessionResponse {
    private String sessionId;
    private String roomName;
    private String jitsiDomain;
    private String status;
    private String createdBy;
    private List<String> participants;
    private Instant createdAt;
}
