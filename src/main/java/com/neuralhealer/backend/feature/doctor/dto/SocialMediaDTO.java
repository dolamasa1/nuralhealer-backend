ackage com.neuralhealer.backend.feature.doctor.dto.SocialMediaDTO;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialMediaDTO {

    @Pattern(regexp = "^https?://.*", message = "Must be a valid URL")
    private String linkedin;

    @Pattern(regexp = "^https?://.*", message = "Must be a valid URL")
    private String twitter;

    @Pattern(regexp = "^https?://.*", message = "Must be a valid URL")
    private String facebook;

    @Pattern(regexp = "^https?://.*", message = "Must be a valid URL")
    private String instagram;

    @Pattern(regexp = "^https?://.*", message = "Must be a valid URL")
    private String website;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Must be a valid E.164 phone number")
    private String whatsapp;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Must be a valid E.164 phone number")
    private String phone;
}
