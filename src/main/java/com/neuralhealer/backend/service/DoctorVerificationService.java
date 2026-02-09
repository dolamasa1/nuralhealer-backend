package com.neuralhealer.backend.service;

import com.neuralhealer.backend.model.entity.DoctorVerificationQuestion;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DoctorVerificationService {
    List<Map<String, Object>> getVerificationQuestions();

    void submitVerificationAnswers(UUID userId, Map<String, String> answers);

    List<DoctorVerificationQuestion> getMyVerificationAnswers(UUID userId);
}
