package com.neuralhealer.backend.mapper;

import com.neuralhealer.backend.model.dto.DoctorLobbyCardDTO;
import com.neuralhealer.backend.model.dto.DoctorProfileFullDTO;
import com.neuralhealer.backend.model.entity.DoctorProfile;
import com.neuralhealer.backend.model.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DoctorMapper {

    public DoctorProfileFullDTO toFullDTO(DoctorProfile profile, User user, String profilePictureUrl,
            String thumbnailUrl) {
        if (profile == null || user == null) {
            return null;
        }

        return DoctorProfileFullDTO.builder()
                .id(profile.getId().toString())
                .userId(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .title(profile.getTitle())
                .bio(profile.getBio())
                .specialization(profile.getSpecialization())
                .yearsOfExperience(profile.getYearsOfExperience())
                .certificates(profile.getCertificates())
                .location(DoctorProfileFullDTO.LocationDTO.builder()
                        .city(profile.getLocationCity())
                        .country(profile.getLocationCountry())
                        .latitude(profile.getLatitude())
                        .longitude(profile.getLongitude())
                        .build())
                .profilePictureUrl(profilePictureUrl)
                .profilePictureThumbnailUrl(thumbnailUrl)
                .verificationStatus(profile.getVerificationStatus())
                .availabilityStatus(profile.getAvailabilityStatus())
                .rating(profile.getRating())
                .totalReviews(profile.getTotalReviews())
                .profileCompletion(profile.getProfileCompletionPercentage())
                .socialMedia(profile.getSocialMedia())
                .consultationFee(profile.getConsultationFee())
                .build();
    }

    public DoctorLobbyCardDTO toLobbyCardDTO(DoctorProfile profile, User user, String thumbnailUrl) {
        if (profile == null || user == null) {
            return null;
        }

        String fullName = user.getFirstName() + " " + user.getLastName();
        String location = buildLocationString(profile.getLocationCity(), profile.getLocationCountry());

        return DoctorLobbyCardDTO.builder()
                .id(profile.getId().toString())
                .fullName(fullName)
                .title(profile.getTitle())
                .specialization(profile.getSpecialization())
                .yearsOfExperience(profile.getYearsOfExperience())
                .rating(profile.getRating())
                .totalReviews(profile.getTotalReviews())
                .profilePictureThumbnailUrl(thumbnailUrl)
                .location(location)
                .availabilityStatus(profile.getAvailabilityStatus())
                .verificationStatus(profile.getVerificationStatus())
                .isVerified("verified".equalsIgnoreCase(profile.getVerificationStatus()))
                .consultationFee(profile.getConsultationFee())
                .build();
    }

    public List<DoctorLobbyCardDTO> toLobbyCardDTOs(List<DoctorProfile> profiles, List<String> thumbnailUrls) {
        if (profiles == null || profiles.isEmpty()) {
            return List.of();
        }

        return profiles.stream()
                .map(profile -> {
                    int index = profiles.indexOf(profile);
                    String thumbnailUrl = thumbnailUrls != null && index < thumbnailUrls.size()
                            ? thumbnailUrls.get(index)
                            : null;
                    return toLobbyCardDTO(profile, profile.getUser(), thumbnailUrl);
                })
                .collect(Collectors.toList());
    }

    private String buildLocationString(String city, String country) {
        if (city != null && country != null) {
            return city + ", " + country;
        } else if (city != null) {
            return city;
        } else if (country != null) {
            return country;
        }
        return null;
    }
}
