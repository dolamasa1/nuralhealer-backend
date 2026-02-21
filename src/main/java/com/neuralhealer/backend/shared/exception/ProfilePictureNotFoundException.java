package com.neuralhealer.backend.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProfilePictureNotFoundException extends RuntimeException {
    public ProfilePictureNotFoundException(String message) {
        super(message);
    }
}
