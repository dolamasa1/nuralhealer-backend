package com.neuralhealer.backend.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StartEngagementResponse(
                UUID engagementId,
                String status,
                String initiatedBy,
                RecipientInfo recipientInfo,
                VerificationInfo verification) {
        public record VerificationInfo(
                        String token,
                        String qrCodeData,
                        LocalDateTime expiresAt) {
        }

        public record RecipientInfo(
                        String role,
                        String email) {
        }
}
