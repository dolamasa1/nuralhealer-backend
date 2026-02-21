ackage com.neuralhealer.backend.feature.doctor.controller.DoctorProfileController;

import com.neuralhealer.backend.feature.doctor.dto.DoctorProfileFullDTO;
import com.neuralhealer.backend.feature.doctor.dto.SocialMediaDTO;
import com.neuralhealer.backend.feature.doctor.dto.UpdateDoctorProfileRequest;
import com.neuralhealer.backend.shared.entity.User;
import com.neuralhealer.backend.feature.doctor.service.DoctorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor Profile", description = "Endpoints for managing doctor profile information")
@SecurityRequirement(name = "bearerAuth")
public class DoctorProfileController {

    private final DoctorProfileService doctorProfileService;

    @GetMapping("/{doctorId}/profile")
    @Operation(summary = "Get doctor profile", description = "Publicly accessible endpoint to view a doctor's full profile")
    public ResponseEntity<DoctorProfileFullDTO> getDoctorProfile(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(doctorProfileService.getDoctorProfile(doctorId));
    }

    @GetMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my profile", description = "Returns the profile of the currently authenticated doctor")
    public ResponseEntity<DoctorProfileFullDTO> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(doctorProfileService.getMyProfile(user.getId()));
    }

    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Update my profile", description = "Updates professional information, location, and bio")
    public ResponseEntity<DoctorProfileFullDTO> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateDoctorProfileRequest request) {
        return ResponseEntity.ok(doctorProfileService.updateProfile(user.getId(), request));
    }

    @PostMapping("/me/profile-picture")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Upload profile picture", description = "Uploads and processes a new profile picture. Replaces old one.")
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        String url = doctorProfileService.uploadProfilePicture(user.getId(), file);
        return ResponseEntity.ok(Map.of("profilePictureUrl", url));
    }

    @DeleteMapping("/me/profile-picture")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Delete profile picture", description = "Deletes current profile picture and thumbnail")
    public ResponseEntity<Void> deleteProfilePicture(@AuthenticationPrincipal User user) {
        doctorProfileService.deleteProfilePicture(user.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/availability")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Update availability status", description = "Updates status to online, offline, or busy")
    public ResponseEntity<Void> updateAvailability(
            @AuthenticationPrincipal User user,
            @RequestParam String status) {
        doctorProfileService.updateAvailabilityStatus(user.getId(), status);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/social-media")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Update social media links", description = "Updates all social media links at once")
    public ResponseEntity<Void> updateSocialMedia(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SocialMediaDTO socialMedia) {
        doctorProfileService.updateSocialMedia(user.getId(), socialMedia);
        return ResponseEntity.ok().build();
    }
}
