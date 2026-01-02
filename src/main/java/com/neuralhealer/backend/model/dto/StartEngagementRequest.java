package com.neuralhealer.backend.model.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartEngagementRequest(
        @NotNull(message = "Patient ID is required") UUID patientId,

        @NotNull(message = "Access Rule Name is required") String accessRuleName) {
}
