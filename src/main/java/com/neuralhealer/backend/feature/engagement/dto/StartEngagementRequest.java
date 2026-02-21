package com.neuralhealer.backend.feature.engagement.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartEngagementRequest(
                UUID patientId, // Used by doctor
                UUID doctorId, // Used by patient
                String message, // Optional message from patient
                @NotNull(message = "Access Rule Name is required") String accessRuleName) {
}
