ackage com.neuralhealer.backend.feature.doctor.service.DoctorProfileService;

import com.neuralhealer.backend.feature.doctor.dto.DoctorProfileFullDTO;
import com.neuralhealer.backend.feature.doctor.dto.SocialMediaDTO;
import com.neuralhealer.backend.feature.doctor.dto.UpdateDoctorProfileRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DoctorProfileService {
    DoctorProfileFullDTO getDoctorProfile(UUID doctorId);

    DoctorProfileFullDTO getMyProfile(UUID userId);

    DoctorProfileFullDTO updateProfile(UUID userId, UpdateDoctorProfileRequest request);

    String uploadProfilePicture(UUID userId, MultipartFile file);

    void deleteProfilePicture(UUID userId);

    void updateAvailabilityStatus(UUID userId, String status);

    void updateSocialMedia(UUID userId, SocialMediaDTO socialMedia);

    int calculateProfileCompletion(UUID doctorId);
}
