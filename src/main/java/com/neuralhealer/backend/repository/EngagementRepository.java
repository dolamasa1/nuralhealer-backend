package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.Engagement;
import com.neuralhealer.backend.model.enums.EngagementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EngagementRepository extends JpaRepository<Engagement, UUID> {

    // Find active engagement for a doctor-patient pair to enforce "One Active" rule
    @Query("SELECT e FROM Engagement e WHERE e.doctor.id = :doctorId AND e.patient.id = :patientId AND e.status IN ('PENDING', 'ACTIVE')")
    Optional<Engagement> findActiveEngagement(@Param("doctorId") UUID doctorId, @Param("patientId") UUID patientId);

    List<Engagement> findByDoctorId(UUID doctorId);

    List<Engagement> findByPatientId(UUID patientId);

    List<Engagement> findByDoctorIdAndStatus(UUID doctorId, EngagementStatus status);
}
