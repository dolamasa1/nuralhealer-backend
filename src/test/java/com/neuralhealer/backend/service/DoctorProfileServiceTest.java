package com.neuralhealer.backend.service;

import com.neuralhealer.backend.shared.exception.DoctorNotFoundException;
import com.neuralhealer.backend.shared.mapper.DoctorMapper;
import com.neuralhealer.backend.feature.doctor.dto.DoctorProfileFullDTO;
import com.neuralhealer.backend.feature.doctor.entity.DoctorProfile;
import com.neuralhealer.backend.shared.entity.User;
import com.neuralhealer.backend.feature.doctor.repository.DoctorProfileRepository;
import com.neuralhealer.backend.feature.doctor.service.DoctorProfileServiceImpl;
import com.neuralhealer.backend.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorProfileServiceTest {

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DoctorMapper doctorMapper;

    @InjectMocks
    private DoctorProfileServiceImpl doctorProfileService;

    private UUID doctorId;
    private UUID userId;
    private DoctorProfile doctorProfile;
    private User user;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("test@example.com").build();
        doctorProfile = DoctorProfile.builder()
                .id(doctorId)
                .user(user)
                .title("Dr.")
                .specialization("Psychiatrist")
                .build();
    }

    @Test
    void getDoctorProfile_Success() {
        when(doctorProfileRepository.findById(doctorId)).thenReturn(Optional.of(doctorProfile));
        when(fileStorageService.getPublicUrl(any(), eq(false))).thenReturn("http://full.url");
        when(fileStorageService.getPublicUrl(any(), eq(true))).thenReturn("http://thumb.url");

        DoctorProfileFullDTO expectedDTO = DoctorProfileFullDTO.builder().id(doctorId.toString()).build();
        when(doctorMapper.toFullDTO(any(), any(), anyString(), anyString())).thenReturn(expectedDTO);

        DoctorProfileFullDTO result = doctorProfileService.getDoctorProfile(doctorId);

        assertNotNull(result);
        assertEquals(doctorId.toString(), result.getId());
        verify(doctorProfileRepository).findById(doctorId);
    }

    @Test
    void getDoctorProfile_NotFound() {
        when(doctorProfileRepository.findById(doctorId)).thenReturn(Optional.empty());

        assertThrows(DoctorNotFoundException.class, () -> doctorProfileService.getDoctorProfile(doctorId));
    }

    @Test
    void calculateProfileCompletion_Works() {
        // Basic Info (5*6=30) + Pic(20) + Certs(10) + Social(10) + Verification(30) =
        // 100
        doctorProfile.setBio("Some bio"); // 5
        doctorProfile.setTitle("Dr."); // 5
        doctorProfile.setSpecialization("Psychiatrist"); // 5
        doctorProfile.setYearsOfExperience(5); // 5
        doctorProfile.setLocationCity("Cairo");
        doctorProfile.setLocationCountry("Egypt"); // 5
        doctorProfile.setConsultationFee(100.0); // 5
        doctorProfile.setProfilePicturePath("path/to/pic.jpg"); // 20
        doctorProfile.setCertificates(java.util.List.of(java.util.Map.of("name", "cert"))); // 10
        doctorProfile.setSocialMedia(java.util.Map.of("linkedin", "link")); // 10
        doctorProfile.setVerificationStatus("verified"); // 30

        when(doctorProfileRepository.findById(doctorId)).thenReturn(Optional.of(doctorProfile));

        int completion = doctorProfileService.calculateProfileCompletion(doctorId);

        assertEquals(100, completion);
    }

    @Test
    void uploadProfilePicture_EnforcesRateLimit() {
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(doctorProfile));

        // First upload succeeds (conceptually, we don't mock the internal timestamp map
        // easily but we can test the behavior)
        // We'll just call it and then call it again.

        // Since we can't easily reset/inspect the private lastUploadTimes map without
        // reflection,
        // we'll just test that it works on first call and we'll trust the logic for the
        // second.

        // Mocking MultipartFile
        org.springframework.web.multipart.MultipartFile file = mock(
                org.springframework.web.multipart.MultipartFile.class);
        when(fileStorageService.saveProfilePicture(any(), any())).thenReturn("new/path.jpg");
        when(fileStorageService.getPublicUrl(any(), eq(false))).thenReturn("http://new.url");

        String url = doctorProfileService.uploadProfilePicture(userId, file);

        assertNotNull(url);
        assertEquals("http://new.url", url);
        verify(doctorProfileRepository).save(any());

        // Second upload within 1 minute should fail
        assertThrows(IllegalStateException.class, () -> doctorProfileService.uploadProfilePicture(userId, file));
    }
}
