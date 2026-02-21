package com.neuralhealer.backend.feature.doctor.repository;

import com.neuralhealer.backend.feature.doctor.entity.DoctorPatient;
import com.neuralhealer.backend.feature.doctor.entity.DoctorPatientId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorPatientRepository extends JpaRepository<DoctorPatient, DoctorPatientId> {
    boolean existsByDoctorIdAndPatientId(UUID doctorId, UUID patientId);

    /**
     * Find doctor-patient relationship by doctor and patient profile IDs.
     */
    Optional<DoctorPatient> findByDoctorIdAndPatientId(UUID doctorId, UUID patientId);
}
