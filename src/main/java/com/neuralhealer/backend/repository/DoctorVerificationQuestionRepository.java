package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.DoctorVerificationQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorVerificationQuestionRepository extends JpaRepository<DoctorVerificationQuestion, UUID> {
    List<DoctorVerificationQuestion> findByDoctorId(UUID doctorId);

    void deleteByDoctorId(UUID doctorId);
}
