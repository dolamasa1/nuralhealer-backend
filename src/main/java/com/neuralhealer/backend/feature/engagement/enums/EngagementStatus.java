package com.neuralhealer.backend.feature.engagement.enums;

/**
 * Status of a doctor-patient engagement.
 * Maps to PostgreSQL enum: engagement_status
 * Values: pending, active, ended, archived, cancelled
 */
public enum EngagementStatus {
    pending,
    active,
    ended,
    archived,
    cancelled
}
