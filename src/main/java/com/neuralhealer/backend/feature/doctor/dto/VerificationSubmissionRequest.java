ackage com.neuralhealer.backend.feature.doctor.dto.VerificationSubmissionRequest;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationSubmissionRequest {

    @NotNull(message = "Answers are required")
    private Map<String, String> answers;
}
