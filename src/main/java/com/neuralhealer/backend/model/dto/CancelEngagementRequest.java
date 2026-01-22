package com.neuralhealer.backend.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for cancelling an engagement (soft cancel with audit trail).
 * 
 * @param reason        Required reason for cancellation
 * @param newAccessRule Optional. For patients cancelling active engagements,
 *                      specifies what access level the doctor should have after
 *                      cancellation.
 *                      If null, retention rules from the original access_rule
 *                      apply.
 */
public record CancelEngagementRequest(
        @NotBlank(message = "Cancellation reason is required") String reason,

        String newAccessRule) {
}
