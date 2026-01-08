package com.neuralhealer.backend.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Standalone Composite ID class for DoctorPatient entity.
 * Required for @IdClass usage in DoctorPatient.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorPatientId implements Serializable {
    private UUID doctorId;
    private UUID patientId;
}
