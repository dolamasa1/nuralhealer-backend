package com.neuralhealer.backend.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a business rule conflict prevents an operation.
 * Returns HTTP 409 Conflict status code.
 * 
 * Examples:
 * - Attempting to cancel a non-pending engagement
 * - Trying to delete an active engagement
 * - Refreshing a token for a non-pending engagement
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
