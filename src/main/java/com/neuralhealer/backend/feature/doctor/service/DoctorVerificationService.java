ackage com.neuralhealer.backend.feature.doctor.service.DoctorVerificationService;

import com.neuralhealer.backend.feature.doctor.entity.DoctorVerificationQuestion;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DoctorVerificationService {
    List<Map<String, Object>> getVerificationQuestions();

    void submitVerificationAnswers(UUID userId, Map<String, String> answers);

    List<DoctorVerificationQuestion> getMyVerificationAnswers(UUID userId);
}
