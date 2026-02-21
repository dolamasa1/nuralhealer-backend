ackage com.neuralhealer.backend.feature.doctor.controller.DoctorVerificationController;

import com.neuralhealer.backend.feature.doctor.dto.VerificationSubmissionRequest;
import com.neuralhealer.backend.feature.doctor.entity.DoctorVerificationQuestion;
import com.neuralhealer.backend.shared.entity.User;
import com.neuralhealer.backend.feature.doctor.service.DoctorVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/doctors/verification")
@RequiredArgsConstructor
@Tag(name = "Doctor Verification", description = "Endpoints for doctor identity and license verification")
@SecurityRequirement(name = "bearerAuth")
public class DoctorVerificationController {

    private final DoctorVerificationService verificationService;

    @GetMapping("/questions")
    @Operation(summary = "Get verification questions", description = "Returns the list of questions a doctor must answer for verification")
    public ResponseEntity<List<Map<String, Object>>> getVerificationQuestions() {
        return ResponseEntity.ok(verificationService.getVerificationQuestions());
    }

    @PostMapping("/me/submit")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit verification answers", description = "Submits answers for verification. Sets status to pending.")
    public ResponseEntity<Map<String, String>> submitVerification(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VerificationSubmissionRequest request) {
        verificationService.submitVerificationAnswers(user.getId(), request.getAnswers());
        return ResponseEntity.ok(Map.of(
                "status", "pending",
                "message", "Verification submitted successfully and is under review"));
    }

    @GetMapping("/me/answers")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my verification answers", description = "Returns the answers currently submitted by the authenticated doctor")
    public ResponseEntity<List<DoctorVerificationQuestion>> getMyVerificationAnswers(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(verificationService.getMyVerificationAnswers(user.getId()));
    }
}
