package com.neuralhealer.backend.feature.livesession.dto;

import lombok.Data;

@Data
public class CreateSessionRequest {
    private String participantName;
    private String provider;
}
