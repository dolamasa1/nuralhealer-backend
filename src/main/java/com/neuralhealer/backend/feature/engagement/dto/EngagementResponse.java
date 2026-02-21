package com.neuralhealer.backend.feature.engagement.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EngagementResponse(
                UUID id,
                String engagementId,
                String status,
                String initiatedBy,
                UserSummary doctor,
                UserSummary patient,
                String accessRule,
                LocalDateTime startAt,
                LocalDateTime endAt,
                String terminationReason) {
        public record UserSummary(
                        UUID id,
                        String firstName,
                        String lastName,
                        String email) {
        }
}
