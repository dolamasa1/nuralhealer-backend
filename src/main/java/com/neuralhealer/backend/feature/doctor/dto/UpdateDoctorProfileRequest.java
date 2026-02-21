ackage com.neuralhealer.backend.feature.doctor.dto.UpdateDoctorProfileRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDoctorProfileRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    private String bio;

    @NotNull(message = "Specialization is required")
    @Pattern(regexp = "Psychiatrist|Therapist", message = "Specialization must be either Psychiatrist or Therapist")
    private String specialization;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 70, message = "Years of experience must be realistic")
    private Integer yearsOfExperience;

    private List<Map<String, Object>> certificates;

    @Valid
    private SocialMediaDTO socialMedia;

    @Valid
    private LocationDTO location;

    @DecimalMin(value = "0.0", message = "Consultation fee must be positive")
    private Double consultationFee;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDTO {
        @NotBlank(message = "City is required")
        private String city;

        @NotBlank(message = "Country is required")
        private String country;

        private Double latitude;
        private Double longitude;
    }
}
