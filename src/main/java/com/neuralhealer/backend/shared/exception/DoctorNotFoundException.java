package com.neuralhealer.backend.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DoctorNotFoundException extends ResourceNotFoundException {
    public DoctorNotFoundException(UUID doctorId) {
        super("Doctor", "id", doctorId);
    }

    public DoctorNotFoundException(String fieldName, String value) {
        super("Doctor", fieldName, value);
    }
}
