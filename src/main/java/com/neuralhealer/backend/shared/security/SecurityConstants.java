package com.neuralhealer.backend.shared.security;

/**
 * Application-wide security constants.
 * Central place for shared string literals used across security components.
 */
public final class SecurityConstants {

    /** Name of the HTTP-only cookie that carries the JWT token. */
    public static final String AUTH_COOKIE_NAME = "neuralhealer_token";

    private SecurityConstants() {
    }
}
