ackage com.neuralhealer.backend.feature.engagement.enums.CancellationRole;

/**
 * Identifies who cancelled an engagement for audit trail and notifications.
 * Not stored in database directly - computed from engagement.ended_by field.
 */
public enum CancellationRole {
    /**
     * Doctor cancelled the engagement.
     */
    DOCTOR,

    /**
     * Patient cancelled the engagement.
     */
    PATIENT,

    /**
     * System auto-cancelled (rare, e.g., compliance violation).
     */
    SYSTEM
}
