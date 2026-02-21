ackage com.neuralhealer.backend.feature.engagement.enums.TokenStatus;

/**
 * Status of an engagement verification token.
 * Defined in DB as enum 'token_status'.
 */
public enum TokenStatus {
    pending,
    verified,
    expired,
    cancelled
}
