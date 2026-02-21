ackage com.neuralhealer.backend.feature.engagement.enums.EngagementAction;

/**
 * Defines all possible actions that can be performed on engagements.
 * Used for authorization checks and permission system.
 */
public enum EngagementAction {
    /**
     * Create a new engagement (Doctor only).
     */
    CREATE_ENGAGEMENT,

    /**
     * Verify START token to activate engagement (Patient only).
     */
    VERIFY_START,

    /**
     * Cancel a pending engagement (Doctor or Patient).
     */
    CANCEL_PENDING,

    /**
     * Cancel an active engagement (Doctor or Patient).
     */
    CANCEL_ACTIVE,

    /**
     * Hard delete a pending engagement (Doctor only - creator).
     */
    DELETE_PENDING,

    /**
     * Refresh START token for pending engagement (Doctor only - creator).
     */
    REFRESH_TOKEN,

    /**
     * Get current valid token (Doctor only - creator).
     */
    GET_TOKEN,

    /**
     * Request to end an active engagement (Doctor or Patient).
     */
    REQUEST_END,

    /**
     * Verify END token to confirm termination (Counter-party).
     */
    VERIFY_END,

    /**
     * View engagement details (Doctor or Patient - participant).
     */
    VIEW_ENGAGEMENT,

    /**
     * List user's own engagements (Any authenticated user).
     */
    LIST_ENGAGEMENTS,

    /**
     * Send message in engagement (Doctor or Patient - participant, if active).
     */
    SEND_MESSAGE,

    /**
     * View messages in engagement (Doctor or Patient - per access rules).
     */
    VIEW_MESSAGES
}
