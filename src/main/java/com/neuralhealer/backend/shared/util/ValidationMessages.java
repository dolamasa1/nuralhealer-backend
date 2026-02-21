package com.neuralhealer.backend.shared.util;

/**
 * Simple validation message constants.
 * Used across DTOs for consistent error messages.
 */
public final class ValidationMessages {

    private ValidationMessages() {
        // Prevent instantiation
    }

    public static final String REQUIRED = "This field is required";
    public static final String INVALID_EMAIL = "Invalid email format";
    public static final String INVALID_PASSWORD = "Password must be at least 8 characters";
}
