package com.neuralhealer.backend.service.impl;

import com.neuralhealer.backend.exception.DoctorNotFoundException;
import com.neuralhealer.backend.mapper.DoctorMapper;
import com.neuralhealer.backend.model.dto.DoctorProfileFullDTO;
import com.neuralhealer.backend.model.dto.SocialMediaDTO;
import com.neuralhealer.backend.model.dto.UpdateDoctorProfileRequest;
import com.neuralhealer.backend.model.entity.DoctorProfile;
import com.neuralhealer.backend.repository.DoctorProfileRepository;
import com.neuralhealer.backend.service.DoctorProfileService;
import com.neuralhealer.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorProfileServiceImpl implements DoctorProfileService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final FileStorageService fileStorageService;

    private final DoctorMapper doctorMapper;

    private final java.util.Map<UUID, java.time.LocalDateTime> lastUploadTimes = new java.util.concurrent.ConcurrentHashMap<>();

    @Override

    @Transactional(readOnly = true)
    public DoctorProfileFullDTO getDoctorProfile(UUID doctorId) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor id", doctorId.toString()));

        return getFullDTO(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorProfileFullDTO getMyProfile(UUID userId) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException("User id", userId.toString()));

        return getFullDTO(profile);
    }

    @Override
    @Transactional
    @CacheEvict(value = "doctorLobbyCache", allEntries = true)
    public DoctorProfileFullDTO updateProfile(UUID userId, UpdateDoctorProfileRequest request) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException("User id", userId.toString()));

        profile.setTitle(request.getTitle());
        profile.setBio(request.getBio());
        profile.setSpecialization(request.getSpecialization());
        profile.setYearsOfExperience(request.getYearsOfExperience());
        profile.setCertificates(request.getCertificates());
        profile.setConsultationFee(request.getConsultationFee());

        if (request.getLocation() != null) {
            profile.setLocationCity(request.getLocation().getCity());
            profile.setLocationCountry(request.getLocation().getCountry());
            profile.setLatitude(request.getLocation().getLatitude());
            profile.setLongitude(request.getLocation().getLongitude());
        }

        if (request.getSocialMedia() != null) {
            // Convert SocialMediaDTO to Map for JSONB storage
            profile.setSocialMedia(java.util.Map.of(
                    "linkedin",
                    StringUtils.hasText(request.getSocialMedia().getLinkedin()) ? request.getSocialMedia().getLinkedin()
                            : "",
                    "twitter",
                    StringUtils.hasText(request.getSocialMedia().getTwitter()) ? request.getSocialMedia().getTwitter()
                            : "",
                    "facebook",
                    StringUtils.hasText(request.getSocialMedia().getFacebook()) ? request.getSocialMedia().getFacebook()
                            : "",
                    "instagram",
                    StringUtils.hasText(request.getSocialMedia().getInstagram())
                            ? request.getSocialMedia().getInstagram()
                            : "",
                    "website",
                    StringUtils.hasText(request.getSocialMedia().getWebsite()) ? request.getSocialMedia().getWebsite()
                            : "",
                    "whatsapp",
                    StringUtils.hasText(request.getSocialMedia().getWhatsapp()) ? request.getSocialMedia().getWhatsapp()
                            : "",
                    "phone",
                    StringUtils.hasText(request.getSocialMedia().getPhone()) ? request.getSocialMedia().getPhone()
                            : ""));
        }

        profile.setProfileCompletionPercentage(calculateProfileCompletion(profile.getId()));

        DoctorProfile saved = doctorProfileRepository.save(profile);
        return getFullDTO(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "doctorLobbyCache", allEntries = true)
    public String uploadProfilePicture(UUID userId, MultipartFile file) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException("User id", userId.toString()));

        // Rate limit: 1 upload per minute
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime lastUpload = lastUploadTimes.get(profile.getId());
        if (lastUpload != null && lastUpload.plusMinutes(1).isAfter(now)) {
            throw new IllegalStateException("Please wait at least 1 minute between profile picture uploads.");
        }

        String relativePath = fileStorageService.saveProfilePicture(file, profile.getId());
        profile.setProfilePicturePath(relativePath);
        profile.setProfileCompletionPercentage(calculateProfileCompletion(profile.getId()));
        doctorProfileRepository.save(profile);

        lastUploadTimes.put(profile.getId(), now);

        return fileStorageService.getPublicUrl(relativePath, false);
    }

    @Override
    @Transactional
    @CacheEvict(value = "doctorLobbyCache", allEntries = true)
    public void deleteProfilePicture(UUID userId) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException("User id", userId.toString()));

        fileStorageService.deleteProfilePicture(profile.getId());
        profile.setProfilePicturePath(null);
        profile.setProfileCompletionPercentage(calculateProfileCompletion(profile.getId()));
        doctorProfileRepository.save(profile);
    }

    @Override
    @Transactional
    public void updateAvailabilityStatus(UUID userId, String status) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException("User id", userId.toString()));

        // Simple validation
        if (!java.util.List.of("online", "offline", "busy").contains(status.toLowerCase())) {
            throw new IllegalArgumentException("Invalid status. Must be online, offline, or busy.");
        }

        profile.setAvailabilityStatus(status.toLowerCase());
        doctorProfileRepository.save(profile);
    }

    @Override
    @Transactional
    @CacheEvict(value = "doctorLobbyCache", allEntries = true)
    public void updateSocialMedia(UUID userId, SocialMediaDTO socialMedia) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException("User id", userId.toString()));

        profile.setSocialMedia(java.util.Map.of(
                "linkedin", socialMedia.getLinkedin() != null ? socialMedia.getLinkedin() : "",
                "twitter", socialMedia.getTwitter() != null ? socialMedia.getTwitter() : "",
                "facebook", socialMedia.getFacebook() != null ? socialMedia.getFacebook() : "",
                "instagram", socialMedia.getInstagram() != null ? socialMedia.getInstagram() : "",
                "website", socialMedia.getWebsite() != null ? socialMedia.getWebsite() : "",
                "whatsapp", socialMedia.getWhatsapp() != null ? socialMedia.getWhatsapp() : "",
                "phone", socialMedia.getPhone() != null ? socialMedia.getPhone() : ""));

        profile.setProfileCompletionPercentage(calculateProfileCompletion(profile.getId()));
        doctorProfileRepository.save(profile);
    }

    @Override
    public int calculateProfileCompletion(UUID doctorId) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor id", doctorId.toString()));

        int completion = 0;

        // Basic Info (5 points each, Total 30)
        if (StringUtils.hasText(profile.getBio()))
            completion += 5;
        if (StringUtils.hasText(profile.getTitle()))
            completion += 5;
        if (StringUtils.hasText(profile.getSpecialization()))
            completion += 5;
        if (profile.getYearsOfExperience() != null)
            completion += 5;
        if (StringUtils.hasText(profile.getLocationCity()) && StringUtils.hasText(profile.getLocationCountry()))
            completion += 5;
        if (profile.getConsultationFee() != null)
            completion += 5;

        // Visual (20)
        if (StringUtils.hasText(profile.getProfilePicturePath()))
            completion += 20;

        // Professional (10)
        if (profile.getCertificates() != null && !profile.getCertificates().isEmpty()) {
            completion += 10;
        }

        // Contact (10)
        if (profile.getSocialMedia() != null
                && profile.getSocialMedia().values().stream().anyMatch(StringUtils::hasText)) {
            completion += 10;
        }

        // Verification (30)
        String vStatus = profile.getVerificationStatus();
        if ("verified".equalsIgnoreCase(vStatus)) {
            completion += 30;
        } else if ("pending".equalsIgnoreCase(vStatus)) {
            completion += 10;
        }

        return completion;
    }

    private DoctorProfileFullDTO getFullDTO(DoctorProfile profile) {
        String fullUrl = fileStorageService.getPublicUrl(profile.getProfilePicturePath(), false);
        String thumbUrl = fileStorageService.getPublicUrl(profile.getProfilePicturePath(), true);
        return doctorMapper.toFullDTO(profile, profile.getUser(), fullUrl, thumbUrl);
    }
}
