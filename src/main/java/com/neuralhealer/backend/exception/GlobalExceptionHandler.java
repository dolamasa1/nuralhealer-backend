package com.neuralhealer.backend.exception;

import com.neuralhealer.backend.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers.
 * Provides consistent error responses matching frontend expected format:
 * { status: 400, message: "Error message", timestamp: "...", path: "/api/..." }
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        /**
         * Handle validation errors from @Valid annotations.
         * Returns field-level error messages.
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationErrors(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach(error -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                log.debug("Validation failed for request {}: {}", request.getRequestURI(), errors);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ErrorResponse.of(
                                                HttpStatus.BAD_REQUEST.value(),
                                                "Validation failed",
                                                request.getRequestURI(),
                                                errors));
        }

        /**
         * Handle constraint violations from @Validated.
         */
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(
                        ConstraintViolationException ex,
                        HttpServletRequest request) {

                Map<String, String> errors = new HashMap<>();
                ex.getConstraintViolations()
                                .forEach(cv -> errors.put(cv.getPropertyPath().toString(), cv.getMessage()));

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ErrorResponse.of(
                                                HttpStatus.BAD_REQUEST.value(),
                                                "Constraint violation",
                                                request.getRequestURI(),
                                                errors));
        }

        /**
         * Handle resource not found exceptions.
         */
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {

                log.debug("Resource not found: {}", ex.getMessage());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ErrorResponse.of(
                                                HttpStatus.NOT_FOUND.value(),
                                                ex.getMessage(),
                                                request.getRequestURI()));
        }

        /**
         * Handle unauthorized exceptions.
         */
        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ErrorResponse> handleUnauthorized(
                        UnauthorizedException ex,
                        HttpServletRequest request) {

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorResponse.of(
                                                HttpStatus.UNAUTHORIZED.value(),
                                                ex.getMessage(),
                                                request.getRequestURI()));
        }

        /**
         * Handle bad credentials (wrong password).
         */
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(
                        BadCredentialsException ex,
                        HttpServletRequest request) {

                log.debug("Bad credentials for request: {}", request.getRequestURI());

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorResponse.of(
                                                HttpStatus.UNAUTHORIZED.value(),
                                                "Invalid email or password",
                                                request.getRequestURI()));
        }

        /**
         * Handle authentication exceptions.
         */
        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(
                        AuthenticationException ex,
                        HttpServletRequest request) {

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorResponse.of(
                                                HttpStatus.UNAUTHORIZED.value(),
                                                "Authentication failed",
                                                request.getRequestURI()));
        }

        /**
         * Handle illegal argument exceptions.
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex,
                        HttpServletRequest request) {

                log.warn("Illegal argument: {}", ex.getMessage());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ErrorResponse.of(
                                                HttpStatus.BAD_REQUEST.value(),
                                                ex.getMessage(),
                                                request.getRequestURI()));
        }

        /**
         * Handle all other unexpected exceptions.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {

                log.error("Unexpected error for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
                ex.printStackTrace(); // FORCE PRINT STACK TRACE FOR DEBUGGING

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.of(
                                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                "An unexpected error occurred: " + ex.getMessage(), // Include message
                                                                                                    // for debug
                                                request.getRequestURI()));
        }
}
