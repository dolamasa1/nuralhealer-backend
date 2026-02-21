package com.neuralhealer.backend.feature.doctor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralhealer.backend.shared.exception.DoctorNotFoundException;
import com.neuralhealer.backend.feature.doctor.entity.DoctorProfile;
import com.neuralhealer.backend.feature.doctor.entity.DoctorVerificationQuestion;
import com.neuralhealer.backend.feature.doctor.repository.DoctorProfileRepository;

import com.neuralhealer.backend.feature.doctor.repository.DoctorVerificationQuestionRepository;
import com.neuralhealer.backend.feature.email.repository.SystemSettingRepository;
import com.neuralhealer.backend.feature.doctor.service.DoctorVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorVerificationServiceImpl implements DoctorVerificationService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorVerificationQuestionRepository verificationRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final ObjectMapper objectMapper;

    private static final String QUESTIONS_SETTING_KEY = "doctor_verification_questions";

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVerificationQuestions() {
        return systemSettingRepository.findByKey(QUESTIONS_SETTING_KEY)
                .map(setting -> {
                    try {
                        return objectMapper.readValue(setting.getValue(),
                                new TypeReference<List<Map<String, Object>>>() {
                                });
                    } catch (Exception e) {
                        log.error("Failed to parse verification questions from system settings", e);
                        return new ArrayList<Map<String, Object>>();
                    }
                })
                .orElse(new ArrayList<>());
    }

    @Override
    @Transactional
    public void submitVerificationAnswers(UUID userId, Map<String, String> answers) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException("User id", userId.toString()));

        // Delete existing answers
        verificationRepository.deleteByDoctorId(profile.getId());

        // Save new answers
        List<DoctorVerificationQuestion> entities = new ArrayList<>();
        answers.forEach((key, value) -> {
            entities.add(DoctorVerificationQuestion.builder()
                    .doctor(profile)
                    .questionKey(key)
                    .answer(value)
                    .build());
        });

        verificationRepository.saveAll(entities);

        // Update profile status
        profile.setVerificationStatus("pending");
        doctorProfileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorVerificationQuestion> getMyVerificationAnswers(UUID userId) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException("User id", userId.toString()));

        return verificationRepository.findByDoctorId(profile.getId());
    }
}
