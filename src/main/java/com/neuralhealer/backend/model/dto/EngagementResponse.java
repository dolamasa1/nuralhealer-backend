package com.neuralhealer.backend.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EngagementResponse(
        UUID id,
        String engagementId,
        String status,
        UserSummary doctor,
        UserSummary patient,
        String accessRule,
        LocalDateTime startAt,
        LocalDateTime endAt) {
    public record UserSummary(
            UUID id,
            String firstName,
            String lastName,
            String email) {
    }
}
