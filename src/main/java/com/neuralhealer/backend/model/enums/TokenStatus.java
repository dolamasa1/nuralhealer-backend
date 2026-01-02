package com.neuralhealer.backend.model.enums;

/**
 * Status of an engagement verification token.
 * Defined in DB as enum 'token_status'.
 */
public enum TokenStatus {
    PENDING,
    VERIFIED,
    EXPIRED,
    CANCELLED
}
