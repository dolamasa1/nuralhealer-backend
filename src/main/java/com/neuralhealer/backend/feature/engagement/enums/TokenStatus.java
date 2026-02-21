package com.neuralhealer.backend.feature.engagement.enums;

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
