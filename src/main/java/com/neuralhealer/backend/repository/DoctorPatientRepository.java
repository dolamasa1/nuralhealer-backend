package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.DoctorPatient;
import com.neuralhealer.backend.model.entity.DoctorPatientId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DoctorPatientRepository extends JpaRepository<DoctorPatient, DoctorPatientId> {
    boolean existsByDoctorIdAndPatientId(UUID doctorId, UUID patientId);
}
