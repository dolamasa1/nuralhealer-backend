package com.neuralhealer.backend.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user lacks permission for an operation.
 * Returns HTTP 403 Forbidden status code.
 * 
 * Examples:
 * - Patient attempting to delete an engagement (only doctor can delete)
 * - Patient attempting to refresh token (only doctor can refresh)
 * - User attempting to access engagement they're not part of
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
